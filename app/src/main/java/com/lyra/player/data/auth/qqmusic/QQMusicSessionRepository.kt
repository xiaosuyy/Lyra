package com.lyra.player.data.auth.qqmusic

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QQMusicSession(
    val cookie: String = "",
    val uin: String = "",
    val musicKey: String = ""
) {
    val isLoggedIn: Boolean
        get() = uin.isNotBlank() && musicKey.isNotBlank()
}

/** QQ 音乐登录态仅保存在本应用私有存储中。 */
class QQMusicSessionRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessionFlow = MutableStateFlow(parse(prefs.getString(KEY_COOKIE, "").orEmpty()))
    val sessionFlow: StateFlow<QQMusicSession> = _sessionFlow.asStateFlow()

    fun getSessionOnce(): QQMusicSession = _sessionFlow.value

    fun saveCookies(cookies: Map<String, String>): Boolean {
        val cookie = cookies.entries
            .filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }
            .joinToString("; ") { (key, value) -> "$key=$value" }
        return saveCookieString(cookie)
    }

    fun saveCookieString(cookie: String): Boolean {
        val session = parse(cookie)
        if (!session.isLoggedIn) return false
        prefs.edit().putString(KEY_COOKIE, cookie.trim()).apply()
        _sessionFlow.value = session
        return true
    }

    fun clear() {
        prefs.edit().clear().apply()
        _sessionFlow.value = QQMusicSession()
    }

    companion object {
        private const val PREFS_NAME = "lyra_qq_music_session"
        private const val KEY_COOKIE = "cookie"

        fun parse(cookie: String): QQMusicSession {
            val values = cookieValues(cookie)
            val rawUin = values["qqmusic_uin"]
                .orEmpty()
                .ifBlank {
                    if (values["login_type"] == "2") values["wxuin"].orEmpty()
                    else values["uin"].orEmpty()
                }
                .ifBlank { values["wxuin"].orEmpty() }
                .trim()
                .removePrefix("o")
            val musicKey = values["qm_keyst"]
                .orEmpty()
                .ifBlank { values["qqmusic_key"].orEmpty() }
            return QQMusicSession(
                cookie = cookie.trim(),
                uin = rawUin.filter(Char::isDigit),
                musicKey = musicKey.trim()
            )
        }

        fun cookieValues(cookie: String): Map<String, String> = cookie
            .split(';')
            .map(String::trim)
            .mapNotNull { entry ->
                val separator = entry.indexOf('=')
                if (separator <= 0) null
                else entry.substring(0, separator).trim() to entry.substring(separator + 1).trim()
            }
            .filter { (key, _) -> key.isNotBlank() }
            .toMap()
    }
}
