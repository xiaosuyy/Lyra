package com.lyra.player.ui.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lyra.player.R
import com.lyra.player.core.api.qqmusic.QQMusicAccountProfile
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.data.auth.qqmusic.QQMusicSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class QQMusicLoginUiState(
    val isLoggedIn: Boolean = false,
    val profile: QQMusicAccountProfile? = null,
    val loadingProfile: Boolean = false,
    val loggingIn: Boolean = false,
    val error: String? = null,
    val loginCompleted: Boolean = false
)

/** QQ 音乐账号登录。 */
class QQMusicLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionRepo by lazy { AppContainer.qqMusicSessionRepo }
    private val accountClient by lazy { AppContainer.qqMusicAccountClient }

    private val _uiState = MutableStateFlow(QQMusicLoginUiState())
    val uiState: StateFlow<QQMusicLoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionRepo.getSessionOnce()
            _uiState.update { it.copy(isLoggedIn = session.isLoggedIn) }
            if (session.isLoggedIn) refreshProfileInternal()
        }
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepo.sessionFlow.collect { session ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = session.isLoggedIn,
                        profile = if (session.isLoggedIn) it.profile else null
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumeLoginCompleted() {
        _uiState.update { it.copy(loginCompleted = false) }
    }

    fun loginWithCookies(cookieJson: String) {
        val cookies = runCatching {
            val json = JSONObject(cookieJson)
            buildMap {
                json.keys().forEach { key ->
                    val value = json.optString(key)
                    if (key.isNotBlank() && value.isNotBlank()) put(key, value)
                }
            }
        }.getOrElse {
            _uiState.update { it.copy(error = localized(R.string.auth_cookie_invalid)) }
            return
        }
        val cookie = cookies.entries.joinToString("; ") { (key, value) -> "$key=$value" }
        val session = QQMusicSessionRepository.parse(cookie)
        if (!session.isLoggedIn) {
            _uiState.update { it.copy(error = localized(R.string.qq_login_cookie_incomplete)) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(loggingIn = true, error = null) }
            try {
                val profile = accountClient.fetchProfile(
                    cookie = cookie,
                    uin = session.uin,
                    musicKey = session.musicKey
                )
                if (!sessionRepo.saveCookieString(cookie)) {
                    _uiState.update {
                        it.copy(loggingIn = false, error = localized(R.string.qq_login_cookie_incomplete))
                    }
                    return@launch
                }
                LyraLogger.d("LYRA-QQLogin", "QQ music login saved uin=${session.uin.take(4)}***")
                _uiState.update {
                    it.copy(
                        loggingIn = false,
                        isLoggedIn = true,
                        profile = profile,
                        loginCompleted = true
                    )
                }
            } catch (error: Exception) {
                LyraLogger.d("LYRA-QQLogin", "QQ music login failed: ${error.message}")
                _uiState.update {
                    it.copy(
                        loggingIn = false,
                        error = localized(R.string.qq_login_failed, error.message ?: "error")
                    )
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch(Dispatchers.IO) { refreshProfileInternal() }
    }

    fun logout() {
        sessionRepo.clear()
        _uiState.update {
            it.copy(isLoggedIn = false, profile = null, error = null)
        }
    }

    private suspend fun refreshProfileInternal() {
        val session = sessionRepo.getSessionOnce()
        if (!session.isLoggedIn) return
        _uiState.update { it.copy(loadingProfile = true) }
        val profile = runCatching {
            accountClient.fetchProfile(session.cookie, session.uin, session.musicKey)
        }.onFailure {
            LyraLogger.d("LYRA-QQLogin", "QQ profile load failed: ${it.message}")
        }.getOrNull()
        _uiState.update { it.copy(loadingProfile = false, profile = profile ?: it.profile) }
    }

    private fun localized(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)
}
