package com.lyra.player.core.api.qishui

/*
 * LyraPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 LyraPlayer developers
 * https://github.com/cwuom/Lyra
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.lyra.player.core.api.qishui/QishuiPassportPoller
 * Created: 2026/11/8
 */

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lyra.player.core.logging.LyraLogger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class QishuiPollRequest(
    val token: String,
    val postUrl: String,
    val getUrl: String,
    val body: String,
    val headers: Map<String, String>,
    val cookieSeed: Map<String, String>
)

internal data class QishuiPollResponse(
    val httpStatus: Int,
    val body: String,
    val cookie: String
)

/**
 * ByteDance passport 的 check_qrconnect 在扫码后会校验动态 `a_bogus/msToken` 签名，
 * 普通 HTTP 轮询一律返回 error_code=7（访问太频繁），而且登录态 Cookie 只落在
 * 浏览器 CookieJar 里。这里用一个隐藏 WebView 加载安全组件（bdms）在页面内发
 * 同源 XHR 完成签名与跳转，再从 CookieManager 读取扫码后的会话 Cookie。
 *
 * 方案与 qsyy 的 Android 端 PassportPoller 一致；页面与 SDK 胶水层随 APK 打包。
 */
internal class QishuiPassportPoller {

    private val main = Handler(Looper.getMainLooper())
    private val mutex = Mutex()

    private var activity: Activity? = null
    private var webView: WebView? = null
    private var viewToken = ""
    private var pageReady = false
    private var readyCallback: (() -> Unit)? = null
    private var pendingPoll: PendingPoll? = null

    private class PendingPoll(
        val continuation: CancellableContinuation<QishuiPollResponse>
    ) {
        var timeoutTask: Runnable? = null
    }

    fun attach(activity: Activity) {
        this.activity = activity
    }

    fun detach() {
        stop()
        activity = null
    }

    fun stop() {
        main.post {
            failPending(IOException("汽水音乐登录已取消"))
            destroyView()
        }
    }

    suspend fun poll(request: QishuiPollRequest): QishuiPollResponse = mutex.withLock {
        withContext(Dispatchers.Main.immediate) { ensureView(request) }
        awaitPageReady()
        val effective = withContext(Dispatchers.Main.immediate) { withLiveCsrf(request) }
        suspendCancellableCoroutine { continuation ->
            main.post {
                if (!continuation.isActive) return@post
                val pending = PendingPoll(continuation)
                pendingPoll = pending
                pending.timeoutTask = Runnable {
                    finishPending(pending, IOException("汽水音乐登录轮询超时"))
                }.also { main.postDelayed(it, POLL_TIMEOUT_MS) }
                evaluatePoll(
                    effective,
                    onMissingView = {
                        finishPending(pending, IOException("登录页面已关闭，请重新扫码"))
                    }
                )
            }
            continuation.invokeOnCancellation {
                main.post {
                    pendingPoll?.takeIf { it.continuation === continuation }?.let(::clearPending)
                }
            }
        }
    }

    private suspend fun awaitPageReady() {
        if (pageReady) return
        suspendCancellableCoroutine { continuation ->
            main.post {
                if (!continuation.isActive) return@post
                if (pageReady) {
                    continuation.resume(Unit)
                } else {
                    readyCallback = {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
            continuation.invokeOnCancellation { main.post { readyCallback = null } }
        }
    }

    /**
     * 扫码后 passport 会校验 CSRF 请求头与实际 Cookie 是否一致；
     * WebView 里的 Cookie 会被页面响应刷新，不能沿用建码时 OkHttp 收到的旧值。
     */
    private fun withLiveCsrf(request: QishuiPollRequest): QishuiPollRequest {
        if (!request.headers.containsKey(CSRF_HEADER)) return request
        val jar = liveCookieHeader()
        val live = cookieValue(jar, "passport_csrf_token")
            .ifBlank { cookieValue(jar, "passport_csrf_token_default") }
        val seed = request.headers[CSRF_HEADER].orEmpty()
        LyraLogger.d(
            "LYRA-QishuiLogin",
            "poll csrf live=${live.length} seed=${seed.length} match=${live == seed} " +
                "jarKeys=${jar.split(';').map { it.substringBefore('=').trim() }.filter { it.isNotBlank() }}"
        )
        if (live.isBlank()) return request
        return request.copy(headers = request.headers + (CSRF_HEADER to live))
    }

    private fun liveCookieHeader(): String {
        val cookies = CookieManager.getInstance()
        cookies.flush()
        return listOf(ORIGIN, BFF_ORIGIN)
            .mapNotNull { cookies.getCookie(it) }
            .filter { it.isNotBlank() }
            .joinToString("; ")
    }

    private fun cookieValue(header: String, name: String): String =
        header.split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith("$name=") }
            ?.substringAfter('=')
            .orEmpty()

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensureView(request: QishuiPollRequest) {
        val host = activity ?: throw IOException("登录页面已关闭，请重新扫码")
        if (webView != null && viewToken == request.token) return
        destroyView()
        val view = WebView(host).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            alpha = 0f
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.blockNetworkImage = true
            settings.userAgentString = PASSPORT_USER_AGENT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(view, true)
        request.cookieSeed.forEach { (name, value) ->
            if (name.startsWith("sessionid") || value.isBlank()) return@forEach
            val pair = "$name=$value; Path=/; Domain=.qishui.com"
            cookies.setCookie(ORIGIN, pair)
            cookies.setCookie(BFF_ORIGIN, pair)
        }
        cookies.flush()
        view.addJavascriptInterface(Bridge(), "LyraQr")
        view.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val path = request.url?.path ?: return null
                if (path == SIGNER_PATH) {
                    return assetResponse("text/html", "qishui/qr-signer.html")
                }
                if (path.endsWith("/sdk-glue.js")) {
                    return assetResponse("text/javascript", "qishui/sdk-glue.js")
                }
                return null
            }

            override fun onPageFinished(view: WebView, url: String) {
                pageReady = true
                readyCallback?.invoke()
                readyCallback = null
            }
        }
        runCatching {
            host.findViewById<ViewGroup>(android.R.id.content)?.addView(view)
        }.onFailure {
            view.destroy()
            throw IOException("无法初始化汽水音乐登录组件", it)
        }
        webView = view
        viewToken = request.token
        pageReady = false
        view.loadUrl(PAGE_URL)
    }

    private fun evaluatePoll(request: QishuiPollRequest, onMissingView: () -> Unit) {
        val view = webView ?: return onMissingView()
        val options = JSONObject().apply {
            put("postUrl", request.postUrl)
            put("getUrl", request.getUrl)
            put("body", request.body)
            put("headers", JSONObject().apply {
                request.headers.forEach { (name, value) -> put(name, value) }
            })
        }
        val js = "window.qsyyWhenReady(20000)" +
            ".then(function(){return window.qsyyPassportPoll($options);})" +
            ".then(function(r){LyraQr.onPoll(JSON.stringify(r));})" +
            ".catch(function(e){LyraQr.onError(String(e&&e.message||e));});"
        view.evaluateJavascript(js, null)
    }

    private fun complete(raw: String?) {
        val result = runCatching {
            val payload = JSONObject(raw.orEmpty())
            // signer 页面返回 {status, json, text, ...}，json 为解析后的对象。
            val body = payload.optString("text")
                .ifBlank { payload.optJSONObject("json")?.toString().orEmpty() }
            val httpStatus = payload.optInt("status", 0)
            val error = payload.optString("error")
            val signedUrl = payload.optString("responseURL").toHttpUrlOrNull()
            LyraLogger.d(
                "LYRA-QishuiLogin",
                "poll raw http=$httpStatus error=$error signed=${payload.optBoolean("signed")} " +
                    "aBogus=${signedUrl?.queryParameter("a_bogus")?.length} " +
                    "msToken=${signedUrl?.queryParameter("msToken")?.length} " +
                    "urlKeys=${signedUrl?.queryParameterNames?.joinToString(",")} " +
                    "len=${body.length} body=${body.take(240)}"
            )
            if (body.isBlank() && httpStatus == 0) {
                throw IOException(error.ifBlank { "汽水音乐安全组件未响应" })
            }
            QishuiPollResponse(
                httpStatus = httpStatus,
                body = body,
                cookie = harvestCookies(payload.optString("documentCookie"))
            )
        }
        main.post {
            val pending = pendingPoll ?: return@post
            if (!result.isSuccess) {
                finishPending(
                    pending,
                    result.exceptionOrNull() ?: IOException("汽水音乐登录失败")
                )
                return@post
            }
            clearPending(pending)
            if (pending.continuation.isActive) pending.continuation.resume(result.getOrThrow())
        }
    }

    private fun harvestCookies(documentCookie: String): String {
        val cookies = CookieManager.getInstance()
        cookies.flush()
        val parts = linkedMapOf<String, String>()
        listOf(ORIGIN, BFF_ORIGIN).forEach { origin ->
            cookies.getCookie(origin)?.split(';')?.forEach { raw ->
                val pair = raw.trim()
                val separator = pair.indexOf('=')
                if (separator > 0) {
                    parts[pair.substring(0, separator).trim()] =
                        pair.substring(separator + 1).trim()
                }
            }
        }
        documentCookie.split(';').forEach { raw ->
            val pair = raw.trim()
            val separator = pair.indexOf('=')
            if (separator > 0) {
                parts[pair.substring(0, separator).trim()] =
                    parts[pair.substring(0, separator).trim()] ?: pair.substring(separator + 1).trim()
            }
        }
        return parts.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun failPending(error: Throwable) {
        pendingPoll?.let { finishPending(it, error) }
    }

    private fun finishPending(pending: PendingPoll, error: Throwable) {
        clearPending(pending)
        if (pending.continuation.isActive) pending.continuation.resumeWithException(error)
    }

    private fun clearPending(pending: PendingPoll) {
        if (pendingPoll === pending) pendingPoll = null
        pending.timeoutTask?.let { main.removeCallbacks(it) }
        pending.timeoutTask = null
    }

    private fun destroyView() {
        readyCallback = null
        pageReady = false
        viewToken = ""
        val view = webView ?: return
        webView = null
        (view.parent as? ViewGroup)?.removeView(view)
        view.stopLoading()
        view.loadUrl("about:blank")
        view.destroy()
    }

    private fun assetResponse(mime: String, path: String): WebResourceResponse? {
        val bytes = runCatching {
            (activity ?: return null).assets.open(path).use { it.readBytes() }
        }.getOrNull() ?: return null
        return WebResourceResponse(mime, "utf-8", ByteArrayInputStream(bytes))
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onPoll(raw: String) = complete(raw)

        @JavascriptInterface
        fun onError(message: String) = complete(
            JSONObject().put("status", 0).put("error", message).toString()
        )
    }

    private companion object {
        const val ORIGIN = "https://api.qishui.com"
        const val BFF_ORIGIN = "https://bff-pc.qishui.com"
        const val SIGNER_PATH = "/qsyy-poll.html"
        const val PAGE_URL = "https://api.qishui.com$SIGNER_PATH"
        const val POLL_TIMEOUT_MS = 30_000L
        const val CSRF_HEADER = "x-tt-passport-csrf-token"
        const val PASSPORT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) SodaMusic/3.2.1 Chrome/136.0.7103.59 " +
                "Electron/36.4.0 Safari/537.36"
    }
}
