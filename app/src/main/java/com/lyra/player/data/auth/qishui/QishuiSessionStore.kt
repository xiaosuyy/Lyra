@file:Suppress("DEPRECATION")

package com.lyra.player.data.auth.qishui

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
 * File: com.lyra.player.data.auth.qishui/QishuiSessionStore
 * Created: 2026/11/8
 */

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lyra.player.core.logging.LyraLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val QISHUI_AUTH_PREFS = "qishui_auth_secure_prefs"

data class QishuiSession(
    val sessionId: String = "",
    /** 完整 passport cookie 头（sessionid + sid_tt/uid_tt 等）。 */
    val cookie: String = "",
    val userId: String = "",
    val nickname: String = ""
) {
    val isLoggedIn: Boolean
        get() = sessionId.isNotBlank() || cookie.isNotBlank()
}

/**
 * 汽水音乐登录态。与会话相关的凭据只落在独立的加密偏好里，
 * 不复用网易云/哔哩哔哩的存储，避免相互覆盖。
 */
class QishuiSessionStore(context: Context) {
    private val appContext = context.applicationContext
    @Volatile
    private var prefs: SharedPreferences
    private val lock = Any()
    private val _sessionFlow = MutableStateFlow(QishuiSession())

    val sessionFlow: StateFlow<QishuiSession>
        get() = _sessionFlow.asStateFlow()

    init {
        prefs = openEncryptedPrefsWithRecovery()
        _sessionFlow.value = readLocked()
    }

    fun getSessionOnce(): QishuiSession = _sessionFlow.value

    fun save(
        sessionId: String,
        cookie: String,
        userId: String = "",
        nickname: String = ""
    ): Boolean {
        val safeSessionId = sessionId.trim()
        if (safeSessionId.isBlank() || safeSessionId.any { it.isWhitespace() || it == ';' }) {
            LyraLogger.w("LYRA-QishuiSession", "Rejected invalid session id")
            return false
        }
        val safeCookie = cookie.trim().takeIf {
            it.isNotBlank() && it.none { ch -> ch == '\n' || ch == '\r' }
        }.orEmpty()
        synchronized(lock) {
            prefs.edit {
                putString(KEY_SESSION_ID, safeSessionId)
                putString(KEY_COOKIE, safeCookie)
                putString(KEY_USER_ID, userId)
                putString(KEY_NICKNAME, nickname)
            }
            _sessionFlow.value = readLocked()
        }
        LyraLogger.d("LYRA-QishuiSession", "Saved Qishui session: userId=$userId")
        return true
    }

    fun updateProfile(userId: String, nickname: String) {
        synchronized(lock) {
            if (userId.isNotBlank()) prefs.edit { putString(KEY_USER_ID, userId) }
            if (nickname.isNotBlank()) prefs.edit { putString(KEY_NICKNAME, nickname) }
            _sessionFlow.value = readLocked()
        }
    }

    fun clear() {
        synchronized(lock) {
            prefs.edit {
                remove(KEY_SESSION_ID)
                remove(KEY_COOKIE)
                remove(KEY_USER_ID)
                remove(KEY_NICKNAME)
            }
            _sessionFlow.value = readLocked()
        }
        LyraLogger.d("LYRA-QishuiSession", "Cleared Qishui session")
    }

    private fun readLocked(): QishuiSession {
        return QishuiSession(
            sessionId = prefs.getString(KEY_SESSION_ID, "").orEmpty(),
            cookie = prefs.getString(KEY_COOKIE, "").orEmpty(),
            userId = prefs.getString(KEY_USER_ID, "").orEmpty(),
            nickname = prefs.getString(KEY_NICKNAME, "").orEmpty()
        )
    }

    private fun openEncryptedPrefsWithRecovery(): SharedPreferences {
        return runCatching { createEncryptedPrefs() }.getOrElse { error ->
            LyraLogger.w(
                "LYRA-QishuiSession",
                "Failed to open Qishui secure prefs, recreating storage.",
                error
            )
            runCatching { appContext.deleteSharedPreferences(QISHUI_AUTH_PREFS) }
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            QISHUI_AUTH_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private companion object {
        const val KEY_SESSION_ID = "sessionid"
        const val KEY_COOKIE = "cookie"
        const val KEY_USER_ID = "user_id"
        const val KEY_NICKNAME = "nickname"
    }
}
