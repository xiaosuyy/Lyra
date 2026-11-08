package com.lyra.player.ui.viewmodel.auth

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
 * File: com.lyra.player.ui.viewmodel.auth/QishuiLoginViewModel
 * Created: 2026/11/8
 */

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lyra.player.R
import com.lyra.player.core.api.qishui.QishuiRequestException
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class QishuiLoginUiState(
    val isLoggedIn: Boolean = false,
    val nickname: String = "",
    val isVip: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val loginCompleted: Boolean = false,
    val qrLink: String = "",
    val statusMessage: String = ""
)

/**
 * 汽水音乐账号登录：走官方 Passport 网页扫码，手机确认后
 * 轮询结果里的 sessionid 换成汽水会话并校验账号。
 */
class QishuiLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val store by lazy { AppContainer.qishuiSessionStore }
    private val api by lazy { AppContainer.qishuiApiClient }
    private var qrJob: Job? = null

    private val _uiState = MutableStateFlow(QishuiLoginUiState())
    val uiState: StateFlow<QishuiLoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            store.sessionFlow.collect { session ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = session.isLoggedIn,
                        nickname = if (session.isLoggedIn) session.nickname else ""
                    )
                }
                if (session.isLoggedIn && _uiState.value.nickname.isBlank()) {
                    refreshProfileInternal()
                }
            }
        }
    }

    fun startQrLogin() {
        if (_uiState.value.isLoggedIn || qrJob?.isActive == true) {
            return
        }
        qrJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    qrLink = "",
                    statusMessage = getString(R.string.qishui_qr_loading)
                )
            }
            val qr = runCatching {
                api.createQr(Build.MODEL.orEmpty())
            }.onFailure { error ->
                LyraLogger.w("LYRA-QishuiLogin", "Create QR failed", error)
            }.getOrNull()
            if (qr == null) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = getString(
                            R.string.qishui_login_failed,
                            getString(R.string.qishui_qr_create_failed)
                        )
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    qrLink = qr.link,
                    statusMessage = getString(R.string.qishui_qr_scan_hint)
                )
            }
            var lastStatus = ""
            repeat(QR_POLL_ATTEMPTS) {
                delay(QR_POLL_INTERVAL_MS)
                var failure: Throwable? = null
                val state = runCatching { api.pollQr(qr.token) }
                    .onFailure { failure = it }
                    .getOrNull()
                if (state == null) {
                    val code = (failure as? QishuiRequestException)?.code ?: 0
                    val message = failure?.message.orEmpty()
                    LyraLogger.w("LYRA-QishuiLogin", "QR poll failed: code=$code $message")
                    when {
                        code == RATE_LIMIT_CODE || message.contains("频繁") -> {
                            _uiState.update {
                                it.copy(statusMessage = getString(R.string.qishui_qr_rate_limited))
                            }
                            delay(RATE_LIMIT_BACKOFF_MS)
                        }
                        code == NEED_VERIFY_CODE -> {
                            _uiState.update {
                                it.copy(statusMessage = getString(R.string.qishui_qr_need_verify))
                            }
                        }
                        code in TERMINAL_CODES ||
                            TERMINAL_HINTS.any { message.contains(it) } -> {
                            _uiState.update { it.copy(error = message) }
                            return@launch
                        }
                    }
                    return@repeat
                }
                if (state.status != lastStatus) {
                    lastStatus = state.status
                    LyraLogger.d(
                        "LYRA-QishuiLogin",
                        "QR poll status=${state.status} hasSession=${state.sessionId.isNotBlank()}"
                    )
                }
                if (state.sessionId.isNotBlank()) {
                    completeLogin(state.sessionId, state.cookie)
                    return@launch
                }
                when (state.status.lowercase()) {
                    "1", "scanned", "scaned" -> _uiState.update {
                        it.copy(statusMessage = getString(R.string.qishui_qr_scanned))
                    }
                    "3", "confirmed" -> _uiState.update {
                        it.copy(statusMessage = getString(R.string.qishui_qr_confirmed))
                    }
                    "2", "5", "expired", "cancelled", "canceled", "refused" -> {
                        _uiState.update {
                            it.copy(statusMessage = getString(R.string.qishui_qr_expired))
                        }
                        return@launch
                    }
                }
            }
            _uiState.update {
                it.copy(statusMessage = getString(R.string.qishui_qr_timeout))
            }
        }
    }

    fun stopQrLogin() {
        qrJob?.cancel()
        qrJob = null
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumeLoginCompleted() {
        _uiState.update { it.copy(loginCompleted = false) }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            store.clear()
            AppContainer.qishuiRepository.onLoggedOut()
            _uiState.update {
                it.copy(
                    isLoggedIn = false,
                    nickname = "",
                    isVip = false,
                    loading = false,
                    error = null,
                    qrLink = "",
                    statusMessage = ""
                )
            }
        }
    }

    private suspend fun completeLogin(sessionId: String, cookie: String) {
        if (!store.save(sessionId = sessionId, cookie = cookie)) {
            _uiState.update {
                it.copy(
                    loading = false,
                    error = getString(R.string.qishui_login_failed, "invalid session")
                )
            }
            return
        }
        val profile = runCatching {
            withContext(Dispatchers.IO) { api.profile() }
        }.onFailure { error ->
            LyraLogger.w("LYRA-QishuiLogin", "Profile exchange failed", error)
        }.getOrNull()
        if (profile == null) {
            store.clear()
            _uiState.update {
                it.copy(
                    loading = false,
                    error = getString(R.string.qishui_login_failed, "无法获取账号信息")
                )
            }
            return
        }
        store.updateProfile(profile.id, profile.nickname)
        _uiState.update {
            it.copy(
                loading = false,
                isLoggedIn = true,
                nickname = profile.nickname,
                isVip = profile.isVip,
                error = null,
                loginCompleted = true
            )
        }
        LyraLogger.d("LYRA-QishuiLogin", "Qishui login completed: userId=${profile.id}")
    }

    private suspend fun refreshProfileInternal() {
        val profile = runCatching {
            withContext(Dispatchers.IO) { api.profile() }
        }.getOrNull() ?: return
        store.updateProfile(profile.id, profile.nickname)
        _uiState.update {
            it.copy(nickname = profile.nickname, isVip = profile.isVip)
        }
    }

    private fun getString(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    private companion object {
        const val QR_POLL_INTERVAL_MS = 2_500L
        const val QR_POLL_ATTEMPTS = 120
        const val RATE_LIMIT_BACKOFF_MS = 5_000L
        const val RATE_LIMIT_CODE = 7
        const val NEED_VERIFY_CODE = 2046
        val TERMINAL_CODES = setOf(1049)
        val TERMINAL_HINTS = listOf("版本过低")
    }
}
