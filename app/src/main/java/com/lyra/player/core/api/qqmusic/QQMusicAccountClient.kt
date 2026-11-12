package com.lyra.player.core.api.qqmusic

import com.lyra.player.core.di.AppContainer
import com.lyra.player.util.network.awaitResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

data class QQMusicAccountProfile(
    val uin: String = "",
    val nickname: String = "",
    val avatarUrl: String? = null
)

/** QQ 音乐账号资料，用于设置页账号卡片展示。 */
class QQMusicAccountClient(
    private val httpClient: OkHttpClient = AppContainer.sharedOkHttpClient
) {

    suspend fun fetchProfile(cookie: String, uin: String, musicKey: String): QQMusicAccountProfile =
        withContext(Dispatchers.IO) {
            require(uin.isNotBlank() && musicKey.isNotBlank()) { "QQ音乐登录态不完整" }
            val url = "https://c6.y.qq.com/rsc/fcgi-bin/fcg_get_profile_homepage.fcg"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("g_tk", hash33(musicKey).toString())
                .addQueryParameter("format", "json")
                .addQueryParameter("inCharset", "utf-8")
                .addQueryParameter("outCharset", "utf-8")
                .addQueryParameter("notice", "0")
                .addQueryParameter("cid", "205360838")
                .addQueryParameter("needNewCode", "0")
                .addQueryParameter("loginUin", uin)
                .addQueryParameter("hostUin", "0")
                .addQueryParameter("userid", uin)
                .addQueryParameter("reqfrom", "1")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Referer", "https://y.qq.com/")
                .header("Cookie", cookie)
                .build()
            val raw = httpClient.newCall(request).awaitResponse { response ->
                if (!response.isSuccessful) throw IOException("QQ音乐资料请求失败: ${response.code}")
                response.body.string()
            }
            val json = JSONObject(raw)
            val code = json.optInt("code", -1)
            if (code != 0) {
                throw IOException(
                    json.optString("message")
                        .ifBlank { json.optString("msg") }
                        .ifBlank { "QQ音乐登录状态无效（$code）" }
                )
            }
            val data = json.optJSONObject("data") ?: json
            val creator = data.optJSONObject("creator")
                ?: data.optJSONObject("user")
                ?: data.optJSONObject("profile")
            val nickname = sequenceOf(
                creator?.optString("nick"),
                creator?.optString("nickname"),
                creator?.optString("name"),
                data.optString("nick"),
                data.optString("nickname")
            ).filterNotNull().firstOrNull { it.isNotBlank() } ?: "QQ音乐用户 $uin"
            val avatar = sequenceOf(
                creator?.optString("headpic"),
                creator?.optString("avatar"),
                creator?.optString("avatarUrl"),
                data.optString("headpic"),
                data.optString("avatar")
            ).filterNotNull().firstOrNull { it.isNotBlank() }?.let(::secureUrl)
            QQMusicAccountProfile(uin = uin, nickname = nickname, avatarUrl = avatar)
        }

    private fun secureUrl(value: String): String = when {
        value.startsWith("//") -> "https:$value"
        value.startsWith("http://", ignoreCase = true) -> "https://${value.substringAfter("://")}"
        else -> value
    }

    private fun hash33(value: String, seed: Long = 5381L): Long {
        var hash = seed
        value.forEach { char ->
            hash += (hash shl 5) + char.code
            hash = hash and 0xFFFF_FFFFL
        }
        return hash and 0x7FFF_FFFFL
    }
}
