package com.lyra.player.activity.auth

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
 * File: com.lyra.player.activity.auth/QQMusicWebLoginActivity
 */

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.lyra.player.R
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.data.auth.web.ForegroundWebLoginGuard
import com.lyra.player.ui.feedback.showNeriViewSnackbar
import com.lyra.player.util.network.hostMatchesAnyDomain
import com.lyra.player.util.network.isAllowedMainFrameRequest
import com.lyra.player.util.platform.lockPortraitIfPhone
import org.json.JSONObject

/**
 * 内置 WebView 登录 QQ 音乐。
 *
 * 使用桌面 UA 打开官方站点，扫码或账号密码登录完成后读取
 * uin + qm_keyst 并返回给设置页保存。
 */
class QQMusicWebLoginActivity : ComponentActivity() {

    companion object {
        const val RESULT_COOKIE = "result_cookie_map_json"
        private const val LOG_TAG = "LYRA-QQLogin"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val LOGIN_URL = "https://y.qq.com/"
        private val ALLOWED_LOGIN_DOMAINS = setOf(
            "qq.com",
            "gtimg.cn",
            "weixin.qq.com",
            "weixinbridge.com"
        )
        private val COOKIE_READ_URLS = listOf(
            "https://y.qq.com",
            "https://c6.y.qq.com",
            "https://u.y.qq.com",
            "https://music.qq.com",
            "https://login.y.qq.com",
            "https://qqmusic.qq.com"
        )
        private val LOGIN_COOKIE_DOMAINS = listOf(
            "https://y.qq.com/",
            "https://u.y.qq.com/",
            "https://c.y.qq.com/",
            "https://c6.y.qq.com/",
            "https://i.y.qq.com/",
            "https://login.y.qq.com/",
            "https://music.qq.com/",
            "https://qqmusic.qq.com/",
            "https://qq.com/",
            "https://graph.qq.com/",
            "https://ssl.ptlogin2.qq.com/",
            "https://ptlogin2.qq.com/",
            "https://ptlogin2.music.qq.com/",
            "https://xui.ptlogin2.qq.com/",
            "https://ui.ptlogin2.qq.com/"
        )
        private val LOGIN_COOKIE_NAMES = setOf(
            "uin", "p_uin", "luin", "o_cookie", "qqmusic_uin", "wxuin", "login_type",
            "qm_keyst", "qqmusic_key", "skey", "p_skey", "pt4_token", "ptcz", "qrsig",
            "lskey", "music_key", "yqq_stat"
        )
    }

    private lateinit var webView: WebView
    private lateinit var toolbar: MaterialToolbar
    private var foregroundWebLoginToken: AutoCloseable? = null
    private var hasReturned = false
    private val loginCompletionWatcher = WebLoginCompletionWatcher(::maybeReturnIfLoggedIn)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortraitIfPhone()
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        foregroundWebLoginToken = ForegroundWebLoginGuard.enter("qqmusic")

        val root = CoordinatorLayout(this).apply {
            fitsSystemWindows = false
            setBackgroundColor(
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorSurface,
                    Color.WHITE
                )
            )
        }

        val appBar = AppBarLayout(this).apply {
            layoutParams = CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorSurface,
                    Color.WHITE
                )
            )
        }
        toolbar = MaterialToolbar(this).apply {
            title = getString(R.string.qq_web_login)
            setNavigationIcon(R.drawable.ic_arrow_back_24)
            setNavigationOnClickListener { finish() }
            inflateMenu(R.menu.menu_netease_web_login)
            setOnMenuItemClickListener { onToolbarMenu(it) }
        }
        appBar.addView(toolbar)

        webView = WebView(this).apply {
            layoutParams = CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                behavior = AppBarLayout.ScrollingViewBehavior()
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                allowFileAccess = false
                allowContentAccess = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                userAgentString = DESKTOP_UA
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = InnerClient()
            webChromeClient = WebChromeClient()
        }
        webView.resumeTimers()

        root.addView(webView)
        root.addView(appBar)
        appBar.bringToFront()

        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            appBar.updatePadding(top = status.top)
            webView.updatePadding(bottom = nav.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (this@QQMusicWebLoginActivity::webView.isInitialized && webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )

        clearPreviousQQWebCookies()
        webView.clearCache(true)
        webView.clearHistory()
        loginCompletionWatcher.start()
        webView.loadUrl(LOGIN_URL)
    }

    override fun onResume() {
        super.onResume()
        if (this::webView.isInitialized) {
            webView.resumeTimers()
            webView.onResume()
        }
    }

    override fun onPause() {
        if (this::webView.isInitialized) {
            CookieManager.getInstance().flush()
            webView.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        loginCompletionWatcher.stop()
        if (this::webView.isInitialized) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        foregroundWebLoginToken?.close()
        foregroundWebLoginToken = null
        super.onDestroy()
    }

    private fun onToolbarMenu(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                webView.reload()
                true
            }
            R.id.action_read_cookie -> {
                readAndReturnCookies()
                true
            }
            else -> false
        }
    }

    private fun readAndReturnCookies() {
        try {
            CookieManager.getInstance().flush()
            val map = readCookieMap()
            if (!isCompleteLoginCookieMap(map)) {
                showNeriViewSnackbar(
                    webView,
                    getString(R.string.qq_login_cookie_incomplete),
                    Snackbar.LENGTH_SHORT
                )
                return
            }
            returnCookies(map)
        } catch (e: Throwable) {
            showNeriViewSnackbar(
                webView,
                getString(R.string.snackbar_read_failed, e.message ?: e.javaClass.simpleName),
                Snackbar.LENGTH_LONG
            )
        }
    }

    private fun isCompleteLoginCookieMap(map: Map<String, String>): Boolean {
        val hasUin = map["uin"].orEmpty().isNotBlank() || map["qqmusic_uin"].orEmpty().isNotBlank()
        val hasKey = map["qm_keyst"].orEmpty().isNotBlank() || map["qqmusic_key"].orEmpty().isNotBlank()
        return hasUin && hasKey
    }

    private fun returnCookies(map: Map<String, String>) {
        if (hasReturned) return
        hasReturned = true
        val json = JSONObject(map as Map<*, *>).toString()
        setResult(RESULT_OK, Intent().putExtra(RESULT_COOKIE, json))
        LyraLogger.d(LOG_TAG, "Login OK, cookie keys=${map.keys}")
        finish()
    }

    private fun cookieStringToMap(raw: String): MutableMap<String, String> {
        val map = linkedMapOf<String, String>()
        raw.split(';')
            .map { it.trim() }
            .filter { it.isNotBlank() && it.contains('=') }
            .forEach { part ->
                val idx = part.indexOf('=')
                val key = part.substring(0, idx).trim()
                val value = part.substring(idx + 1).trim()
                if (key.isNotEmpty()) map[key] = value
            }
        return map
    }

    private fun readCookieMap(): Map<String, String> {
        val manager = CookieManager.getInstance()
        val merged = COOKIE_READ_URLS
            .mapNotNull { manager.getCookie(it) }
            .filter { it.isNotBlank() }
            .joinToString("; ")
        if (merged.isBlank()) return emptyMap()
        return cookieStringToMap(merged)
    }

    private fun clearPreviousQQWebCookies() {
        runCatching {
            val manager = CookieManager.getInstance()
            for (domain in LOGIN_COOKIE_DOMAINS) {
                for (name in LOGIN_COOKIE_NAMES) {
                    manager.setCookie(domain, "$name=; Max-Age=0; Path=/; SameSite=Lax")
                    manager.setCookie(
                        domain,
                        "$name=; Max-Age=0; Path=/; Domain=.qq.com; SameSite=Lax"
                    )
                }
            }
            manager.flush()
        }
    }

    private fun maybeReturnIfLoggedIn(): Boolean {
        if (hasReturned) return true
        CookieManager.getInstance().flush()
        val cookies = readCookieMap()
        if (!isCompleteLoginCookieMap(cookies)) {
            return false
        }
        returnCookies(cookies)
        return true
    }

    private inner class InnerClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val currentRequest = request ?: return false
            if (!isAllowedMainFrameRequest(currentRequest) { isAllowedLoginUri(it) }) {
                LyraLogger.w(LOG_TAG, "Blocked unexpected navigation: ${currentRequest.url}")
                return true
            }
            if (currentRequest.isForMainFrame) {
                loginCompletionWatcher.scheduleCheck()
            }
            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            loginCompletionWatcher.scheduleCheck()
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            view?.post { loginCompletionWatcher.scheduleCheck() }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            loginCompletionWatcher.scheduleCheck()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                LyraLogger.w(
                    LOG_TAG,
                    "Web error code=${error?.errorCode} desc=${error?.description} url=${request.url}"
                )
            }
        }
    }

    private fun isAllowedLoginUri(uri: Uri?): Boolean {
        val resolvedUri = uri ?: return false
        if (resolvedUri.toString() == "about:blank") return true
        val scheme = resolvedUri.scheme.orEmpty()
        if (!scheme.equals("https", ignoreCase = true) && !scheme.equals("http", ignoreCase = true)) {
            return false
        }
        return hostMatchesAnyDomain(resolvedUri.host, ALLOWED_LOGIN_DOMAINS)
    }
}
