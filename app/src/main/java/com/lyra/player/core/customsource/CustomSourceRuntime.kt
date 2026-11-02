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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.lyra.player.core.logging.LyraLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
 * 通过 [Bridge] 同步桥接给原生实现，所以脚本可以像在 lx-music 里一样工作。
 */
internal class CustomSourceRuntime(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val scriptInfoJson: String
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingRequests = ConcurrentHashMap<String, (String) -> Unit>()

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
        readyDeferred = ready
        declarations = emptyList()
        withContext(Dispatchers.Main) { ensureWebView() }
        val environmentReady = withTimeoutOrNull(LOAD_TIMEOUT_MS) { ready.await() } ?: false
        if (!environmentReady) {
            return CustomSourceLoadState(false, emptyList(), "自定义源运行环境加载失败")
        }

        val inited = CompletableDeferred<List<CustomSourceDeclaration>>()
        initedDeferred = inited
        withContext(Dispatchers.Main) {
            webView?.evaluateJavascript(scriptText, null)
        }
        val loaded = withTimeoutOrNull(LOAD_TIMEOUT_MS) { inited.await() }
        initedDeferred = null
        return if (loaded == null) {
            declarations = emptyList()
            CustomSourceLoadState(false, emptyList(), "脚本没有返回音源信息（可能初始化失败）")
        } else {
            declarations = loaded
            CustomSourceLoadState(true, loaded, null)
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
        if (!isReady) return null
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
        val resultJson = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            val deferred = CompletableDeferred<String>()
            pendingRequests[requestId] = { json ->
                if (!deferred.isCompleted) {
                    deferred.complete(json)
                }
            }
            mainHandler.post {
                val view = webView
                if (view == null) {
                    pendingRequests.remove(requestId)
                    deferred.complete("")
                } else {
                    view.evaluateJavascript(
                        "__neriInvokeRequest(${JSONObject.quote(requestId)}, " +
                            "${JSONObject.quote(payload)})",
                        null
                    )
                }
            }
            deferred.await()
        }
        pendingRequests.remove(requestId)
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
                        LyraLogger.w(TAG, "自定义源返回的不是直链，已忽略: ${value.take(60)}")
                        null
                    }
            } else {
                LyraLogger.w(
                    TAG,
                    "自定义源返回失败: ${json.optString("error")} | " +
                        resultJson.take(300)
                )
                null
            }
        }.getOrNull()
    }

    fun release() {
        val view = webView
        webView = null
        declarations = emptyList()
        readyDeferred = null
        initedDeferred = null
        pendingRequests.clear()
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
            view.addJavascriptInterface(Bridge(), BRIDGE_NAME)
            view.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    LyraLogger.i(
                        TAG,
                        "[console] ${message.message()} (${message.sourceId()}:${message.lineNumber()})"
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
                        "自定义源宿主页面加载失败: ${error?.description} (${request?.url})"
                    )
                }
            }
            view.loadUrl(HOST_ASSET_URL)
            webView = view
            LyraLogger.i(TAG, "自定义源运行环境已创建")
        }.onFailure { error ->
            LyraLogger.e(TAG, "创建自定义源运行环境失败: ${error.message}")
            readyDeferred?.complete(false)
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun scriptInfo(): String = scriptInfoJson

        @JavascriptInterface
        fun log(message: String) {
            LyraLogger.i(TAG, "[js] $message")
        }

        @JavascriptInterface
        fun ready() {
            readyDeferred?.complete(true)
        }

        @JavascriptInterface
        fun md5(text: String): String = md5Hex(text)

        @JavascriptInterface
        fun inited(payloadJson: String) {
            val parsed = parseDeclarations(payloadJson)
            LyraLogger.i(
                TAG,
                "自定义源已声明音源: " + parsed.joinToString(",") {
                    it.sourceId + "(" + it.qualitys.joinToString("/") + ")"
                }
            )
            initedDeferred?.complete(parsed)
        }

        @JavascriptInterface
        fun result(id: String, json: String) {
            pendingRequests.remove(id)?.invoke(json)
        }

        @JavascriptInterface
        fun request(payloadJson: String): String = performHttp(payloadJson)
    }

    private fun performHttp(payloadJson: String): String {
        return try {
            val payload = JSONObject(payloadJson)
            val url = payload.optString("url")
            val options = payload.optJSONObject("options") ?: JSONObject()
            val method = options.optString("method", "get").uppercase()
            val headers = options.optJSONObject("headers")
            val bodyText = options.optString("body").takeIf { it.isNotEmpty() }
            val builder = Request.Builder().url(url)
            LyraLogger.i(TAG, "自定义源请求头: ${headers?.toString()?.take(300)}")
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
            val response = httpClient.newBuilder()
                .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
                .newCall(builder.build())
                .execute()
            response.use { result ->
                val body = result.body?.string().orEmpty()
                LyraLogger.i(TAG, "自定义源请求 ${result.code}: $url")
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
            LyraLogger.w(TAG, "自定义源网络请求失败: ${error.message}")
            JSONObject()
                .put("ok", false)
                .put("error", error.message ?: "request failed")
                .toString()
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
