package com.lyra.player.ui.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lyra.player.R
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class NeteaseAccountProfile(
    val userId: Long = 0L,
    val nickname: String = "",
    val avatarUrl: String = ""
)

data class NeteaseLoginUiState(
    val phone: String = "",
    val captcha: String = "",
    val codeSent: Boolean = false,
    val sending: Boolean = false,
    val loggingIn: Boolean = false,
    val countdownSec: Int = 0,
    val isLoggedIn: Boolean = false,
    val profile: NeteaseAccountProfile? = null,
    val loadingProfile: Boolean = false,
    val error: String? = null,
    val loginCompleted: Boolean = false
)

/**
 * 网易云账号登录。仅用于首页个性化推荐，不改变搜索、曲库等其它入口的使用方式。
 */
class NeteaseLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val cookieRepo by lazy { AppContainer.neteaseCookieRepo }
    private val client by lazy { AppContainer.neteaseClient }

    private val _uiState = MutableStateFlow(NeteaseLoginUiState())
    val uiState: StateFlow<NeteaseLoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val loggedIn = cookieRepo.getCookiesOnce()["MUSIC_U"].orEmpty().isNotBlank()
            _uiState.update { it.copy(isLoggedIn = loggedIn) }
            if (loggedIn) refreshProfileInternal()
        }
        viewModelScope.launch(Dispatchers.IO) {
            cookieRepo.cookieFlow.collect { cookies ->
                val loggedIn = cookies["MUSIC_U"].orEmpty().isNotBlank()
                _uiState.update {
                    it.copy(
                        isLoggedIn = loggedIn,
                        profile = if (loggedIn) it.profile else null
                    )
                }
                if (loggedIn && _uiState.value.profile == null) {
                    refreshProfileInternal()
                }
            }
        }
    }

    fun onPhoneChange(value: String) {
        _uiState.update {
            it.copy(phone = value.filter(Char::isDigit).take(20), error = null)
        }
    }

    fun onCaptchaChange(value: String) {
        _uiState.update {
            it.copy(captcha = value.filter(Char::isDigit).take(10), error = null)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumeLoginCompleted() {
        _uiState.update { it.copy(loginCompleted = false) }
    }

    fun sendCaptcha() {
        val phone = _uiState.value.phone.trim()
        if (!isValidPhone(phone)) {
            _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.login_phone_invalid)) }
            return
        }
        if (_uiState.value.sending || _uiState.value.countdownSec > 0) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(sending = true, error = null) }
            try {
                val response = client.sendCaptcha(phone)
                val json = JSONObject(response)
                if (json.optInt("code", -1) == 200) {
                    _uiState.update {
                        it.copy(codeSent = true, sending = false)
                    }
                    startCountdown()
                } else {
                    _uiState.update {
                        it.copy(
                            sending = false,
                            error = getApplication<Application>().getString(
                                R.string.netease_login_code_send_failed,
                                json.optString("msg").ifBlank { "error" }
                            )
                        )
                    }
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        sending = false,
                        error = getApplication<Application>().getString(
                            R.string.netease_login_code_send_failed,
                            error.message ?: "error"
                        )
                    )
                }
            }
        }
    }

    fun login() {
        val state = _uiState.value
        val phone = state.phone.trim()
        val captcha = state.captcha.trim()
        if (!isValidPhone(phone)) {
            _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.login_phone_invalid)) }
            return
        }
        if (captcha.isBlank()) {
            _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.login_enter_code)) }
            return
        }
        if (state.loggingIn) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(loggingIn = true, error = null) }
            try {
                var loginResponse = client.loginByCaptcha(phone = phone, captcha = captcha)
                var loginJson = JSONObject(loginResponse)
                LyraLogger.d(
                    "LYRA-NeteaseLogin",
                    "eapi login response code=${loginJson.optInt("code", -1)} " +
                        "msg=${loginJson.optString("msg")}"
                )
                if (loginJson.optInt("code", -1) != 200) {
                    loginResponse = client.loginByCaptchaWeapi(phone = phone, captcha = captcha)
                    loginJson = JSONObject(loginResponse)
                    LyraLogger.d(
                        "LYRA-NeteaseLogin",
                        "weapi login response code=${loginJson.optInt("code", -1)} " +
                            "msg=${loginJson.optString("msg")}"
                    )
                }
                if (loginJson.optInt("code", -1) != 200) {
                    val code = loginJson.optInt("code", -1)
                    val message = loginJson.optString("msg").ifBlank { "code=$code" }
                    _uiState.update {
                        it.copy(
                            loggingIn = false,
                            error = getApplication<Application>().getString(
                                R.string.netease_login_failed,
                                message
                            )
                        )
                    }
                    return@launch
                }

                runCatching { client.ensureWeapiSession() }
                val cookies = client.getCookies().toMutableMap()
                loginJson.optString("cookie")
                    .takeIf { it.isNotBlank() }
                    ?.split(';')
                    ?.map(String::trim)
                    ?.filter { it.contains('=') }
                    ?.forEach { pair ->
                        val index = pair.indexOf('=')
                        if (index > 0) {
                            val key = pair.substring(0, index).trim()
                            val value = pair.substring(index + 1).trim()
                            if (key.isNotBlank() && value.isNotBlank()) {
                                cookies[key] = value
                            }
                        }
                    }
                if (cookies["MUSIC_U"].orEmpty().isBlank()) {
                    _uiState.update {
                        it.copy(
                            loggingIn = false,
                            error = getApplication<Application>().getString(R.string.auth_cookie_invalid)
                        )
                    }
                    return@launch
                }
                client.setPersistedCookies(cookies)
                if (!cookieRepo.saveCookies(cookies)) {
                    _uiState.update {
                        it.copy(
                            loggingIn = false,
                            error = getApplication<Application>().getString(R.string.auth_cookie_invalid)
                        )
                    }
                    return@launch
                }
                val profile = loadProfile()
                _uiState.update {
                    it.copy(
                        loggingIn = false,
                        codeSent = false,
                        captcha = "",
                        isLoggedIn = true,
                        profile = profile,
                        loginCompleted = true
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        loggingIn = false,
                        error = getApplication<Application>().getString(
                            R.string.netease_login_failed,
                            error.message ?: "error"
                        )
                    )
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch(Dispatchers.IO) { refreshProfileInternal() }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { client.logout() }
            cookieRepo.clear()
            _uiState.update {
                it.copy(
                    isLoggedIn = false,
                    profile = null,
                    codeSent = false,
                    captcha = "",
                    error = null
                )
            }
        }
    }

    private suspend fun refreshProfileInternal() {
        _uiState.update { it.copy(loadingProfile = true) }
        val profile = runCatching { loadProfile() }.getOrNull()
        _uiState.update {
            it.copy(loadingProfile = false, profile = profile ?: it.profile)
        }
    }

    private suspend fun loadProfile(): NeteaseAccountProfile? = withContext(Dispatchers.IO) {
        runCatching {
            val raw = client.getCurrentUserAccount()
            val profile = JSONObject(raw).optJSONObject("profile") ?: return@runCatching null
            NeteaseAccountProfile(
                userId = profile.optLong("userId", 0L),
                nickname = profile.optString("nickname").trim(),
                avatarUrl = profile.optString("avatarUrl")
                    .ifBlank { profile.optString("avatarImgUrl") }
                    .trim()
            )
        }.getOrNull()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            var remaining = 60
            while (remaining > 0) {
                _uiState.update { it.copy(countdownSec = remaining) }
                delay(1_000)
                remaining -= 1
            }
            _uiState.update { it.copy(countdownSec = 0) }
        }
    }

    private fun isValidPhone(phone: String): Boolean =
        phone.length in 6..20 && phone.all(Char::isDigit)
}
