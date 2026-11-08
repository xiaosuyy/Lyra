package com.lyra.player.core.api.qishui

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
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.lyra.player.core.api.qishui/QishuiApiClient
 * Created: 2026/11/8
 */

import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.data.auth.qishui.QishuiSession
import com.lyra.player.util.network.hostMatchesAnyDomain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

internal data class QishuiQr(
    val token: String,
    /** light/invoke/scan_login 链接，由客户端本地渲染成二维码。 */
    val link: String
)

internal data class QishuiQrState(
    val status: String,
    val sessionId: String,
    val cookie: String
)

/** Passport 返回非 0 错误码；error_code 7 表示轮询过于频繁，2023 表示需抖音确认。 */
internal class QishuiRequestException(
    val code: Int,
    message: String
) : IOException(message)

internal data class QishuiProfile(
    val id: String,
    val nickname: String,
    val isVip: Boolean
)

internal data class QishuiPlaylist(
    val id: String,
    val title: String,
    val trackCount: Int
)

/**
 * 汽水音乐非官方接口适配。协议字段来自 guowenye/qishui-api（MIT）。
 *
 * 所有请求都显式选择 host/UA：移动端 API 走 `beta-luna.douyin.com`，
 * PC/账号接口走 `api.qishui.com`，passport 与 H5 页面使用浏览器 UA。
 */
internal class QishuiApiClient(
    private val sessionProvider: () -> QishuiSession,
    httpClient: OkHttpClient
) {
    private val http = httpClient.newBuilder()
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private var qrToken = ""
    private val qrCookies = linkedMapOf<String, String>()
    private val qrDeviceId = randomDigits(16)
    private val qrInstallId = randomDigits(15)

    private fun currentSession(): QishuiSession = sessionProvider()

    private fun requireLogin(): QishuiSession {
        val session = currentSession()
        if (!session.isLoggedIn) throw IOException("请先登录汽水音乐")
        return session
    }

    private fun credentialsOf(session: QishuiSession): String =
        session.cookie.takeIf(String::isNotBlank)
            ?: session.sessionId.takeIf(String::isNotBlank)?.let { "sessionid=$it" }
            ?: ""

    private suspend fun request(
        path: String,
        params: Map<String, String> = emptyMap(),
        body: RequestBody? = null,
        app: Boolean = false,
        cookie: String? = null
    ): Pair<JSONObject, String> = withContext(Dispatchers.IO) {
        val isH5 = path.contains("/h5/")
        val isPassport = path.startsWith("/passport")
        val base = if (app) APP_ORIGIN else PC_ORIGIN
        val mergedParams = when {
            !app && !isH5 && !isPassport -> pcAppParams() + params
            else -> params
        }
        val url = base.toHttpUrl().newBuilder()
            .encodedPath(path)
            .apply { mergedParams.forEach { (key, value) -> addQueryParameter(key, value) } }
            .build()
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header(
                "User-Agent",
                when {
                    isPassport -> PASSPORT_USER_AGENT
                    isH5 -> WEB_USER_AGENT
                    app -> APP_USER_AGENT
                    else -> PC_APP_USER_AGENT
                }
            )
        if (!app && !isH5 && !isPassport) {
            builder.header("Content-Type", "application/json; charset=utf-8")
            builder.header("x-luna-background-type", "foreground")
            builder.header("x-luna-is-background-req", "0")
            builder.header("x-luna-is-local-user", "1")
        }
        val credentials = cookie ?: credentialsOf(currentSession())
        if (credentials.isNotBlank()) builder.header("Cookie", credentials)
        if (isPassport) {
            // passport 的 axios 实例配置了 xsrfCookieName=passport_csrf_token，
            // 后续请求必须回传同名请求头，否则扫码确认后会被防刷接口拦下。
            val csrf = cookieValue(credentials, "passport_csrf_token")
                .ifBlank { cookieValue(credentials, "passport_csrf_token_default") }
            if (csrf.isNotBlank()) builder.header("x-tt-passport-csrf-token", csrf)
        }
        if (body != null) builder.post(body)
        http.newCall(builder.build()).execute().use { response ->
            val passportRedirect = isPassport && response.code in 300..399
            if (!response.isSuccessful && !passportRedirect) {
                throw IOException("汽水音乐请求失败：HTTP ${response.code}（$path）")
            }
            val text = response.body.string()
            // passport 偶发空响应，保留给轮询逻辑回落 GET，不要直接失败。
            if (text.isBlank() && !passportRedirect && !isPassport) {
                throw IOException("汽水音乐返回了空响应，请稍后重试")
            }
            val json = if (text.isBlank()) {
                JSONObject()
            } else {
                runCatching { JSONObject(text) }.getOrElse {
                    throw IOException("汽水音乐返回了无法解析的数据")
                }
            }
            val info = json.optJSONObject("status_info")
            val data = json.optJSONObject("data")
            val code = json.optInt(
                "status_code",
                json.optInt("error_code", info?.optInt("status_code", 0) ?: 0)
            )
            val dataCode = data?.optInt("error_code", 0) ?: 0
            // 扫码确认成功后 passport 仍可能附带 error_code（例如限流 7），
            // 此时必须以 status=3 为准，否则会丢掉 redirect_url 里的登录态。
            val confirmed = isConfirmedStatus(QishuiParsing.text(data, "status"))
            if ((code != 0 || dataCode != 0) && !confirmed) {
                val effectiveCode = code.takeIf { it != 0 } ?: dataCode
                val message = data?.optString("description").orEmpty()
                    .ifBlank { info?.optString("status_msg").orEmpty() }
                    .ifBlank { json.optString("message") }
                LyraLogger.w(
                    "LYRA-QishuiApi",
                    "passport error code=$effectiveCode message=$message"
                )
                throw QishuiRequestException(
                    effectiveCode,
                    if (message.isBlank()) {
                        "汽水音乐请求未获通过（$effectiveCode）"
                    } else {
                        "$message（错误码 $effectiveCode）"
                    }
                )
            }
            // 原样保留 Set-Cookie：OkHttp 的域感知解析会丢掉父域下发的 Cookie，
            // passport 扫码握手必须把它们回传给轮询请求。
            val cookies = response.headers.values("Set-Cookie")
                .mapNotNull { header ->
                    header.substringBefore(';').trim().takeIf { it.contains('=') }
                }
                .joinToString("; ")
            json to cookies
        }
    }

    private suspend fun postJson(path: String, body: JSONObject): Pair<JSONObject, String> =
        request(
            path = path,
            body = body.toString().toRequestBody(JSON),
            app = true
        )

    private fun pcAppParams(): Map<String, String> {
        val now = System.currentTimeMillis().toString()
        return linkedMapOf(
            "aid" to AID,
            "app_name" to "luna_pc",
            "region" to "cn",
            "geo_region" to "cn",
            "os_region" to "cn",
            "sim_region" to "",
            "device_id" to now,
            "cdid" to "",
            "iid" to now,
            "version_name" to "3.3.0",
            "version_code" to "30030000",
            "channel" to "official",
            "build_mode" to "master",
            "network_carrier" to "",
            "ac" to "wifi",
            "tz_name" to "Asia/Shanghai",
            "resolution" to "",
            "device_platform" to "windows",
            "device_type" to "Windows",
            "os_version" to "Windows 11",
            "fp" to now
        )
    }

    private suspend fun getPc(path: String, params: Map<String, String> = emptyMap()): JSONObject =
        request(path = path, params = params).first

    // ---------------------------------------------------------------- 登录

    suspend fun profile(): QishuiProfile? {
        val session = currentSession()
        if (!session.isLoggedIn) return null
        val response = getPc("/luna/pc/me", mapOf("aid" to AID))
        val user = response.optJSONObject("my_info") ?: response.optJSONObject("user") ?: response
        val id = QishuiParsing.text(user, "id").ifBlank { session.userId }
        if (id.isBlank()) throw IOException("汽水音乐账号状态已失效，请重新登录")
        return QishuiProfile(
            id = id,
            nickname = QishuiParsing.text(user, "nickname").ifBlank { "汽水音乐用户" },
            isVip = user.optBoolean("is_vip", false)
        )
    }

    // ------------------------------------------------------------ 扫码登录

    /**
     * 汽水音乐 PC 端扫码走的是网页版 passport（`is_from_ttaccountsdk=1`），
     * 二维码内容必须由本地拼成 `bff-pc.qishui.com/light/invoke/scan_login`：
     * passport 下发的 `qrcode_index_url` 指向已下线的 sdk-next 页面，
     * 直接展示会 404，也无法被汽水音乐 App 识别。
     */
    suspend fun createQr(computerName: String): QishuiQr {
        qrToken = ""
        qrCookies.clear()
        val (body, cookies) = request(
            path = "/passport/web/get_qrcode/",
            params = passportParams() + qrClientParams() + qrFlags(),
            cookie = ""
        )
        val data = body.optJSONObject("data") ?: body
        val token = QishuiParsing.text(data, "token")
        if (token.isBlank()) throw IOException("汽水音乐未返回登录二维码")
        qrToken = token
        collectCookies(cookies)
        LyraLogger.d(
            "LYRA-QishuiLogin",
            "QR created tokenLength=${token.length} hasCookies=${cookies.isNotBlank()}"
        )
        return QishuiQr(token = token, link = scanLoginUrl(token, computerName))
    }

    suspend fun pollQr(token: String): QishuiQrState {
        if (token.isBlank() || token != qrToken) {
            throw IOException("登录二维码已刷新，请重新扫码")
        }
        val params = passportParams() + qrClientParams() + qrFlags() +
            mapOf("token" to token, "is_new_login" to "1")
        // 网页 SDK 先发 POST 表单，服务端未返回可用 JSON 时再回落 GET。
        val form = listOf(
            "need_logo" to "false",
            "need_short_url" to "false",
            "is_frontier" to "false",
            "token" to token,
            "is_new_login" to "1",
            "next" to PC_LOGIN_NEXT
        ).joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
        var response = request(
            path = "/passport/web/check_qrconnect/",
            params = params,
            body = form.toRequestBody(FORM),
            cookie = cookieHeader()
        )
        collectCookies(response.second)
        var data = response.first.optJSONObject("data") ?: response.first
        if (!data.has("status")) {
            response = request(
                path = "/passport/web/check_qrconnect/",
                params = params,
                cookie = cookieHeader()
            )
            collectCookies(response.second)
            data = response.first.optJSONObject("data") ?: response.first
        }
        val status = QishuiParsing.text(data, "status")
        val redirectUrl = QishuiParsing.text(data, "redirect_url")
            .ifBlank { QishuiParsing.text(data, "redirectUrl") }
        if (isConfirmedStatus(status) && redirectUrl.isNotBlank()) {
            // 确认后 sessionid 只出现在 redirect_url 的重定向链里，
            // check_qrconnect 自身不会下发登录态 Cookie。
            withContext(Dispatchers.IO) { followLoginRedirects(redirectUrl) }
            LyraLogger.d(
                "LYRA-QishuiLogin",
                "QR confirmed redirectHost=${redirectUrl.toHttpUrlOrNull()?.host} " +
                    "cookieKeys=${qrCookies.keys.joinToString(",")}"
            )
        }
        val merged = cookieHeader()
        val sessionId = cookieValue(merged, "sessionid")
            .ifBlank { cookieValue(merged, "sessionid_ss") }
            .ifBlank { QishuiParsing.text(data, "sessionid") }
            .ifBlank { cookieValue(QishuiParsing.text(data, "session_cookie"), "sessionid") }
        return QishuiQrState(
            status = status,
            sessionId = sessionId,
            cookie = merged
        )
    }

    /**
     * passport 网页 SDK（2.4.13 normal）的完整初始化参数。
     * 缺少 is_from_ttaccountsdk 时 get_qrcode 直接返回 4031「版本过低」；
     * 这是一组被网页登录实际使用的固定值，改动会影响确认阶段的风控判定。
     */
    private fun passportParams(): Map<String, String> = linkedMapOf(
        "passport_jssdk_version" to "2.4.13",
        "passport_jssdk_type" to "normal",
        "is_from_ttaccountsdk" to "1",
        "aid" to AID,
        "language" to "zh",
        "account_sdk_source" to "web",
        "account_sdk_source_info" to "",
        "p_js_v" to "2.4.13",
        "p_js_t" to "pro",
        "p_zt" to "3.3.5",
        "p_ver" to "1.0.29",
        "request_host" to "app://resources",
        "p_bd" to "1.0.0.41",
        "biz_trace_id" to System.currentTimeMillis().toString(),
        "is_new_login" to "1",
        "is_from_iesaccountsaas" to "1"
    )

    private fun qrClientParams(): Map<String, String> = linkedMapOf(
        "device_id" to qrDeviceId,
        "install_id" to qrInstallId,
        "did" to qrDeviceId,
        "iid" to qrInstallId,
        "device_platform" to "PC",
        "version_code" to "3.3.0"
    )

    private fun qrFlags(): Map<String, String> = linkedMapOf(
        "next" to PC_LOGIN_NEXT,
        "need_logo" to "false",
        "need_short_url" to "false",
        "is_frontier" to "false"
    )

    private fun isConfirmedStatus(status: String): Boolean {
        return status.equals("3", ignoreCase = true) ||
            status.equals("confirmed", ignoreCase = true) ||
            status.equals("success", ignoreCase = true)
    }

    private fun collectCookies(setCookieHeaders: String) {
        setCookieHeaders.split(';').forEach { raw ->
            val pair = raw.trim()
            val separator = pair.indexOf('=')
            if (separator <= 0) return@forEach
            qrCookies[pair.substring(0, separator).trim()] =
                pair.substring(separator + 1).trim()
        }
    }

    private fun cookieHeader(): String =
        qrCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

    /**
     * 登录确认后的跳转链会逐跳下发 sessionid 等 Cookie，
     * 只允许在字节系登录域内跳转，避免被服务端响应带去任意地址。
     */
    private fun followLoginRedirects(startUrl: String) {
        var target = startUrl
        repeat(MAX_LOGIN_REDIRECTS) {
            val url = target.toHttpUrlOrNull() ?: return
            if (url.scheme != "https" || !hostMatchesAnyDomain(url.host, LOGIN_COOKIE_DOMAINS)) {
                LyraLogger.w("LYRA-QishuiLogin", "Blocked login redirect host: ${url.host}")
                return
            }
            val builder = Request.Builder()
                .url(url)
                .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
                .header("User-Agent", PASSPORT_USER_AGENT)
            val cookie = cookieHeader()
            if (cookie.isNotBlank()) builder.header("Cookie", cookie)
            val csrf = cookieValue(cookie, "passport_csrf_token")
                .ifBlank { cookieValue(cookie, "passport_csrf_token_default") }
            if (csrf.isNotBlank()) builder.header("x-tt-passport-csrf-token", csrf)
            target = http.newCall(builder.build()).execute().use { response ->
                response.headers.values("Set-Cookie").forEach { header ->
                    val pair = header.substringBefore(';').trim()
                    val separator = pair.indexOf('=')
                    if (separator > 0) {
                        qrCookies[pair.substring(0, separator).trim()] =
                            pair.substring(separator + 1).trim()
                    }
                }
                if (response.code in 300..399) {
                    response.header("Location")?.let { url.resolve(it)?.toString() }.orEmpty()
                } else {
                    ""
                }
            }
            if (target.isBlank()) {
                LyraLogger.d(
                    "LYRA-QishuiLogin",
                    "Login redirect chain finished, hasSession=${qrCookies.containsKey("sessionid")}"
                )
                return
            }
        }
    }

    private fun randomDigits(length: Int): String {
        val builder = StringBuilder(length)
        repeat(length) { index ->
            val digit = if (index == 0) {
                1 + (0..7).random()
            } else {
                (0..9).random()
            }
            builder.append(digit)
        }
        return builder.toString()
    }

    private fun cookieValue(header: String, name: String): String =
        header.split(';')
            .map(String::trim)
            .firstOrNull { it.startsWith("$name=") }
            ?.substringAfter('=')
            .orEmpty()

    // ------------------------------------------------------------ 推荐/曲库

    suspend fun discoverRadios(): List<QishuiRadio> {
        val (body, _) = postJson("/luna/discover", JSONObject())
        return QishuiParsing.radiosFromDiscover(body)
    }

    suspend fun radioTracks(radioId: String, count: Int = 10): List<QishuiTrack> {
        val (body, _) = postJson(
            "/luna/feed/radio/tracks",
            JSONObject()
                .put("radio_id", radioId)
                .put("count", count.coerceIn(1, 50))
                .put("cursor", "")
        )
        return QishuiParsing.tracksFromFeed(body)
    }

    suspend fun collectionTracks(): List<QishuiTrack> {
        requireLogin()
        val body = getPc("/luna/pc/me/collection/mixed", mapOf("aid" to AID))
        return QishuiParsing.tracksFromCollection(body)
    }

    suspend fun userPlaylists(): List<QishuiPlaylist> {
        requireLogin()
        val body = getPc(
            "/luna/pc/me/playlist",
            mapOf(
                "aid" to AID,
                "iid" to "27960026095955",
                "version_code" to "30020100"
            )
        )
        return QishuiParsing.objects(body.optJSONArray("playlists")).mapNotNull { playlist ->
            val id = QishuiParsing.text(playlist, "id").takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            QishuiPlaylist(
                id = id,
                title = QishuiParsing.text(playlist, "title")
                    .ifBlank { QishuiParsing.text(playlist, "name") },
                trackCount = playlist.optInt(
                    "count_tracks",
                    playlist.optInt("track_count", 0)
                )
            )
        }
    }

    suspend fun playlistTracks(playlistId: String, count: Int = 30): List<QishuiTrack> {
        val (body, _) = postJson(
            "/luna/playlist/detail",
            JSONObject()
                .put("playlist_id", playlistId)
                .put("count", count.coerceIn(1, 100))
        )
        return QishuiParsing.tracksFromResources(body, "media_resources")
    }

    // ---------------------------------------------------------------- 播放

    /**
     * 已登录时先尝试完整播放；失败后回落到 H5 试听接口。
     * 返回的 [QishuiAudio.isPreview] 由调用方决定是否走网易云兜底。
     */
    suspend fun resolveAudio(track: QishuiTrack): QishuiAudio? {
        val session = currentSession()
        if (session.isLoggedIn) {
            try {
                val (body, _) = postJson(
                    "/luna/media-player",
                    JSONObject()
                        .put("media_id", track.id)
                        .put("media_type", "track")
                        .put("queue_type", "")
                        .put("enable_refresh_api", false)
                        .put("enable_dash", false)
                )
                val infos = QishuiParsing.objects(body.optJSONArray("player_infos"))
                val info = infos.firstOrNull {
                    QishuiParsing.text(it, "media_id") == track.id
                } ?: infos.singleOrNull()
                if (info != null) {
                    val normalized = JSONObject().put("track_player", info)
                    QishuiParsing.audio(
                        body = normalized,
                        fullDurationMs = track.durationMs.takeIf { it > 0L }
                    )?.let { return it }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                // 账号音质受限时继续尝试 H5 试听
            }
        }
        return runCatching {
            val response = request(
                path = "/luna/h5/seo_track",
                params = mapOf("track_id" to track.id, "device_platform" to "web")
            ).first
            QishuiParsing.audio(
                body = response,
                fullDurationMs = track.durationMs.takeIf { it > 0L }
            )
        }.getOrNull()
    }

    companion object {
        const val AID = "386088"
        const val PC_ORIGIN = "https://api.qishui.com"
        const val PC_LOGIN_NEXT = "https://api.qishui.com"
        const val APP_ORIGIN = "https://beta-luna.douyin.com"
        private const val MAX_LOGIN_REDIRECTS = 8
        private val LOGIN_COOKIE_DOMAINS = setOf(
            "qishui.com",
            "douyin.com",
            "bytedance.com",
            "bytedance.net"
        )
        const val WEB_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        const val PASSPORT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) SodaMusic/3.1.0 Chrome/136.0.7103.59 " +
                "Electron/36.4.0-rs.22.release.main.1 TTElectron/36.4.0-rs.22.release.main.1 " +
                "Safari/537.36"
        const val PC_APP_USER_AGENT = "LunaPC/3.3.0(359450208)"
        const val APP_USER_AGENT = "Luna/19.1.0 Android"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val FORM = "application/x-www-form-urlencoded".toMediaType()
    }
}

/**
 * 上游 `qrcode`/`qrcode_index_url` 仍指向已下线的 `ucenter_web/app/sdk-next`（404），
 * 桌面客户端改用 passport token 拼 `light/invoke/scan_login` 后本地出码。
 */
internal fun scanLoginUrl(token: String, computerName: String): String {
    fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    return "https://bff-pc.qishui.com/light/invoke/scan_login" +
        "?token=${encode(token)}&os=Windows&computer_name=${encode(computerName.ifBlank { "Lyra" })}"
}
