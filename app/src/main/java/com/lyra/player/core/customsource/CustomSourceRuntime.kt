package com.lyra.player.core.customsource

/*
 * LyraPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 LyraPlayer developers
 * https://github.com/cwuom/Lyra
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * File: com.lyra.player.core.customsource/CustomSourceRuntime
 * Created: 2026/9/24
 */

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.webkit.WebView
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.lyra.player.core.logging.LyraLogger
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

private const val TAG = "CustomSource"
private const val HOST_ASSET_URL = "file:///android_asset/customsource/host.html"
private const val LOAD_TIMEOUT_MS = 60_000L
private const val REQUEST_TIMEOUT_MS = 25_000L
private const val MAX_SOURCE_RESPONSE_CHARS = 2_097_152

/**
 * 脚本声明出来的音源，例如 kw / kg / wy。
 */
internal data class CustomSourceDeclaration(
    val sourceId: String,
    val qualitys: List<String>
)

internal data class CustomSourceLoadState(
    val loaded: Boolean,
    val declarations: List<CustomSourceDeclaration>,
    val error: String?
)

/**
 * 运行一个自定义源脚本的环境。
 *
 * 用系统 WebView（V8）执行 lx-music 格式的音源脚本；脚本需要的网络请求、md5 等能力
 * 通过 [Bridge] 桥接给原生实现，网络请求异步回传，不阻塞脚本执行。
 */
internal class CustomSourceRuntime(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val scriptInfoJson: String
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lifecycleLock = Any()
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val httpCalls = ConcurrentHashMap<String, Call>()
    // 从解析 payload 到主线程投递都保留身份，解析失败及已排队的结果也能被 abort 丢弃。
    private val pendingHttpRequests = ConcurrentHashMap<String, Any>()
    private val sourceHttpClient = httpClient.newBuilder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var webView: WebView? = null

    @Volatile
    private var readyDeferred: CompletableDeferred<Boolean>? = null

    @Volatile
    private var initedDeferred: CompletableDeferred<List<CustomSourceDeclaration>>? = null

    @Volatile
    private var declarations: List<CustomSourceDeclaration> = emptyList()

    val isReady: Boolean get() = declarations.isNotEmpty()

    val declaredSources: List<CustomSourceDeclaration> get() = declarations

    val sourceIds: Set<String>
        get() = declarations.mapTo(LinkedHashSet()) { it.sourceId }

    suspend fun load(scriptText: String): CustomSourceLoadState {
        val ready = CompletableDeferred<Boolean>()
        val inited = CompletableDeferred<List<CustomSourceDeclaration>>()
        synchronized(lifecycleLock) {
            readyDeferred = ready
            declarations = emptyList()
        }
        try {
            val view = withContext(Dispatchers.Main) {
                synchronized(lifecycleLock) {
                    if (readyDeferred !== ready) null else {
                        ensureWebView()
                        webView
                    }
                }
            }
            val environmentReady = view != null &&
                (withTimeoutOrNull(LOAD_TIMEOUT_MS) { ready.await() } == true)
            if (!environmentReady) {
                return CustomSourceLoadState(false, emptyList(), "自定义源运行环境加载失败")
            }

            withContext(Dispatchers.Main) {
                synchronized(lifecycleLock) {
                    if (webView === view && readyDeferred === ready) {
                        initedDeferred = inited
                        view?.evaluateJavascript(scriptText, null)
                    } else {
                        inited.complete(emptyList())
                    }
                }
            }
            val loaded = withTimeoutOrNull(LOAD_TIMEOUT_MS) { inited.await() }
            return synchronized(lifecycleLock) {
                if (loaded == null || webView !== view || readyDeferred !== ready) {
                    if (readyDeferred === ready) declarations = emptyList()
                    CustomSourceLoadState(false, emptyList(), "脚本没有返回音源信息（可能初始化失败）")
                } else {
                    declarations = loaded
                    CustomSourceLoadState(true, loaded, null)
                }
            }
        } finally {
            synchronized(lifecycleLock) {
                if (readyDeferred === ready) readyDeferred = null
                if (initedDeferred === inited) initedDeferred = null
            }
            ready.cancel()
            inited.cancel()
        }
    }

    /**
     * 向脚本请求歌曲直链，失败返回 null。
     */
    suspend fun resolveMusicUrl(
        sourceId: String,
        quality: String,
        musicInfo: JSONObject
    ): String? {
        val view = synchronized(lifecycleLock) {
            if (!isReady) return null
            webView ?: return null
        }
        val requestId = UUID.randomUUID().toString()
        val payload = JSONObject()
            .put("source", sourceId)
            .put("action", "musicUrl")
            .put(
                "info",
                JSONObject()
                    .put("type", quality)
                    .put("musicInfo", musicInfo)
            )
            .toString()
        val deferred = CompletableDeferred<String>()
        val resultJson = try {
            withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                val requestContext = currentCoroutineContext()
                synchronized(lifecycleLock) {
                    if (view !== webView) {
                        deferred.complete("")
                    } else {
                        pendingRequests[requestId] = deferred
                        val posted = mainHandler.post {
                            synchronized(lifecycleLock) {
                                if (pendingRequests[requestId] !== deferred ||
                                    !deferred.isActive || !requestContext.isActive
                                ) {
                                    return@post
                                }
                                if (view !== webView) {
                                    pendingRequests.remove(requestId, deferred)
                                    deferred.complete("")
                                } else {
                                    runCatching {
                                        view.evaluateJavascript(
                                            "__neriInvokeRequest(${JSONObject.quote(requestId)}, " +
                                                "${JSONObject.quote(payload)})",
                                            null
                                        )
                                    }.onFailure {
                                        pendingRequests.remove(requestId, deferred)
                                        deferred.complete("")
                                    }
                                }
                            }
                        }
                        if (!posted) {
                            pendingRequests.remove(requestId, deferred)
                            deferred.complete("")
                        }
                    }
                }
                deferred.await()
            }
        } finally {
            // 没有 JS HTTP 的所有权信息；这里只清理本次 native 等待，不取消其他 HTTP。
            synchronized(lifecycleLock) {
                pendingRequests.remove(requestId, deferred)
            }
            deferred.cancel()
        }
        if (resultJson.isNullOrBlank()) {
            LyraLogger.w(TAG, "自定义源请求超时或无响应: source=$sourceId quality=$quality")
            return null
        }
        return runCatching {
            val json = JSONObject(resultJson)
            if (json.optBoolean("ok")) {
                val value = json.optString("value").trim()
                // 和 lx-music 一致：只接受真正的 http(s) 直链
                value.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                    ?: run {
                        LyraLogger.w(TAG, "自定义源返回的不是直链，已忽略")
                        null
                    }
            } else {
                LyraLogger.w(TAG, "自定义源返回失败")
                null
            }
        }.getOrNull()
    }

    fun release() {
        val view: WebView?
        val ready: CompletableDeferred<Boolean>?
        val inited: CompletableDeferred<List<CustomSourceDeclaration>>?
        val requests: List<CompletableDeferred<String>>
        val calls: List<Call>
        synchronized(lifecycleLock) {
            view = webView
            webView = null
            declarations = emptyList()
            ready = readyDeferred
            readyDeferred = null
            inited = initedDeferred
            initedDeferred = null
            requests = pendingRequests.values.toList()
            pendingRequests.clear()
            pendingHttpRequests.clear()
            calls = httpCalls.values.toList()
            httpCalls.clear()
        }
        // 先移除本次快照，再完成等待者，避免恢复的协程注册新请求后被误清理。
        // 只取消这个 runtime 的 Call，不影响共享客户端上的其他请求。
        calls.forEach { it.cancel() }
        ready?.complete(false)
        inited?.complete(emptyList())
        requests.forEach { it.complete("") }
        if (view != null) {
            mainHandler.post {
                runCatching {
                    view.removeJavascriptInterface(BRIDGE_NAME)
                    view.loadUrl("about:blank")
                    view.destroy()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensureWebView() {
        if (webView != null) return
        runCatching {
            val view = WebView(context)
            view.settings.javaScriptEnabled = true
            view.settings.domStorageEnabled = true
            view.settings.allowFileAccess = true
            view.settings.allowContentAccess = true
            runCatching {
                view.settings.allowFileAccessFromFileURLs = true
                view.settings.allowUniversalAccessFromFileURLs = true
            }
            view.addJavascriptInterface(Bridge(view), BRIDGE_NAME)
            view.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    LyraLogger.i(
                        TAG,
                        "[console] level=${message.messageLevel()} line=${message.lineNumber()}（内容已省略）"
                    )
                    return true
                }
            }
            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    LyraLogger.i(TAG, "自定义源宿主页面加载完成")
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    LyraLogger.w(
                        TAG,
                        "自定义源宿主页面加载失败: host=${request?.url?.host} status=${error?.errorCode}"
                    )
                }
            }
            webView = view
            view.loadUrl(HOST_ASSET_URL)
            LyraLogger.i(TAG, "自定义源运行环境已创建")
        }.onFailure {
            LyraLogger.e(TAG, "创建自定义源运行环境失败")
            readyDeferred?.complete(false)
        }
    }

    private inner class Bridge(private val view: WebView) {
        @JavascriptInterface
        fun scriptInfo(): String = scriptInfoJson

        @JavascriptInterface
        fun log(message: String) {
            // 脚本日志可能含完整 URL、headers 或凭据，不直接输出原文。
            LyraLogger.i(TAG, "[js] 日志内容已省略（${message.length} 字符）")
        }

        @JavascriptInterface
        fun ready() {
            synchronized(lifecycleLock) {
                if (view === webView) readyDeferred?.complete(true)
            }
        }

        @JavascriptInterface
        fun md5(text: String): String = md5Hex(text)

        @JavascriptInterface
        fun inited(payloadJson: String) {
            val parsed = parseDeclarations(payloadJson)
            synchronized(lifecycleLock) {
                if (view !== webView) return
                LyraLogger.i(TAG, "自定义源已声明音源: ${parsed.size} 个")
                initedDeferred?.complete(parsed)
            }
        }

        @JavascriptInterface
        fun result(id: String, json: String) {
            synchronized(lifecycleLock) {
                if (view === webView) pendingRequests.remove(id)?.complete(json)
            }
        }

        @JavascriptInterface
        fun request(id: String, payloadJson: String) {
            performHttp(view, id, payloadJson)
        }

        @JavascriptInterface
        fun cancelRequest(id: String) {
            val call = synchronized(lifecycleLock) {
                if (view !== webView) return
                pendingHttpRequests.remove(id)
                httpCalls.remove(id)
            }
            call?.cancel()
        }
    }

    private fun performHttp(view: WebView, id: String, payloadJson: String) {
        val token = Any()
        synchronized(lifecycleLock) {
            if (view !== webView || pendingHttpRequests.putIfAbsent(id, token) != null) return
        }
        var call: Call? = null
        try {
            val payload = JSONObject(payloadJson)
            val url = payload.optString("url")
            val options = payload.optJSONObject("options") ?: JSONObject()
            val method = options.optString("method", "get").uppercase()
            val headers = options.optJSONObject("headers")
            val bodyText = options.optString("body").takeIf { it.isNotEmpty() }
            val builder = Request.Builder().url(url)
            headers?.let { headerObject ->
                headerObject.keys().forEach { key ->
                    val value = headerObject.optString(key)
                    if (value.isNotEmpty()) {
                        runCatching { builder.header(key, value) }
                    }
                }
            }
            val requestBody = bodyText?.toRequestBody("application/json".toMediaType())
            when (method) {
                "POST" -> builder.post(requestBody ?: "".toRequestBody())
                "PUT" -> builder.put(requestBody ?: "".toRequestBody())
                "DELETE" -> builder.delete(requestBody)
                else -> builder.get()
            }
            val requestCall = sourceHttpClient.newCall(builder.build())
            call = requestCall
            synchronized(lifecycleLock) {
                if (view !== webView || pendingHttpRequests[id] !== token) {
                    requestCall.cancel()
                    return
                }
                httpCalls[id] = requestCall
                requestCall.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        postHttpResult(view, id, token, httpErrorJson(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseJson = try {
                            response.use { result ->
                                synchronized(lifecycleLock) {
                                    if (view !== webView || pendingHttpRequests[id] !== token) return
                                }
                                val body = result.body?.string().orEmpty()
                                LyraLogger.i(
                                    TAG,
                                    "自定义源请求 host=${result.request.url.host} status=${result.code}"
                                )
                                val headerJson = JSONObject()
                                result.headers.toMultimap().forEach { (name, values) ->
                                    headerJson.put(name, values.joinToString(","))
                                }
                                JSONObject()
                                    .put("ok", true)
                                    .put("statusCode", result.code)
                                    .put("statusMessage", result.message)
                                    .put("headers", headerJson)
                                    .put("body", body.take(MAX_SOURCE_RESPONSE_CHARS))
                                    .toString()
                            }
                        } catch (error: Exception) {
                            httpErrorJson(error)
                        }
                        postHttpResult(view, id, token, responseJson)
                    }
                })
            }
        } catch (error: Exception) {
            // 包含 payload/Request 构建和 enqueue 失败；仍走同一可取消的异步结果通道。
            call?.cancel()
            postHttpResult(view, id, token, httpErrorJson(error))
        }
    }

    private fun httpErrorJson(error: Exception): String = JSONObject()
        .put("ok", false)
        .put("error", error.message ?: "request failed")
        .toString()

    private fun postHttpResult(view: WebView, id: String, token: Any, responseJson: String) {
        val script = "__neriHttpResult(${JSONObject.quote(id)},${JSONObject.quote(responseJson)})"
        synchronized(lifecycleLock) {
            if (view !== webView || pendingHttpRequests[id] !== token) return
            val posted = mainHandler.post {
                synchronized(lifecycleLock) {
                    // Call 保留到实际投递，abort/release 可使已经排队的回调失效。
                    if (view !== webView || !pendingHttpRequests.remove(id, token)) return@post
                    httpCalls.remove(id)
                    runCatching {
                        view.evaluateJavascript(script, null)
                    }.onFailure {
                        LyraLogger.w(TAG, "自定义源网络结果投递失败")
                    }
                }
            }
            if (!posted && pendingHttpRequests.remove(id, token)) {
                httpCalls.remove(id)?.cancel()
            }
        }
    }

    companion object {
        private const val BRIDGE_NAME = "LyraSource"

        internal fun parseDeclarations(payloadJson: String): List<CustomSourceDeclaration> {
            return runCatching {
                val sources = JSONObject(payloadJson).optJSONObject("sources")
                    ?: return@runCatching emptyList()
                buildList {
                    sources.keys().forEach { sourceId ->
                        val source = sources.optJSONObject(sourceId) ?: return@forEach
                        if (source.optString("type", "music") != "music") {
                            return@forEach
                        }
                        val qualitys = source.optJSONArray("qualitys")?.let { array ->
                            (0 until array.length()).mapNotNull { index ->
                                array.optString(index).takeIf { it.isNotBlank() }
                            }
                        }.orEmpty()
                        add(CustomSourceDeclaration(sourceId = sourceId, qualitys = qualitys))
                    }
                }
            }.getOrDefault(emptyList())
        }

        internal fun md5Hex(value: String): String {
            val digest = java.security.MessageDigest.getInstance("MD5")
            val bytes = digest.digest(value.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
