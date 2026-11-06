package com.lyra.player.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyra.player.BuildConfig
import com.lyra.player.R
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.data.update.ApkInstaller
import com.lyra.player.data.update.AppUpdateInfo
import com.lyra.player.data.update.AppUpdateSource
import com.lyra.player.data.update.GitHubReleaseChecker
import com.lyra.player.data.update.PublicUpdateChecker
import com.lyra.player.data.update.UpdateChecker
import com.lyra.player.data.update.WebDavUpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val update: AppUpdateInfo? = null,
    val isUpToDate: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val message: String? = null,
    val showStartupDialog: Boolean = false
)

class AppUpdateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState

    private var checker: UpdateChecker? = null
    private var downloadedApk: File? = null
    private var downloadedForFileName: String? = null
    private var startupCheckStarted = false

    /** 每次启动静默检查一次；没有可用更新源或检查失败时什么都不显示。 */
    fun checkForUpdatesOnStartup(context: Context) {
        if (startupCheckStarted) return
        startupCheckStarted = true
        val appContext = context.applicationContext
        val checker = resolveChecker(appContext) ?: return

        viewModelScope.launch {
            checker.findLatestUpdate().onSuccess { update ->
                if (update == null) return@onSuccess
                val current = _uiState.value
                if (current.isChecking || current.isDownloading || current.update != null) {
                    return@onSuccess
                }
                _uiState.value = current.copy(
                    update = update,
                    showStartupDialog = true
                )
            }
        }
    }

    fun dismissStartupDialog() {
        _uiState.value = _uiState.value.copy(
            showStartupDialog = false,
            message = null
        )
    }

    fun checkForUpdates(context: Context) {
        val current = _uiState.value
        if (current.isChecking || current.isDownloading) return
        val appContext = context.applicationContext
        val checker = resolveChecker(appContext)
        if (checker == null) {
            _uiState.value = AppUpdateUiState(
                message = appContext.getString(R.string.update_source_required)
            )
            return
        }

        _uiState.value = AppUpdateUiState(isChecking = true)
        viewModelScope.launch {
            checker.findLatestUpdate().fold(
                onSuccess = { update ->
                    _uiState.value = if (update == null) {
                        AppUpdateUiState(isUpToDate = true)
                    } else {
                        AppUpdateUiState(update = update)
                    }
                },
                onFailure = { error ->
                    LyraLogger.w(TAG, "Update check failed: ${error.message}")
                    _uiState.value = AppUpdateUiState(
                        message = appContext.getString(R.string.update_check_failed)
                    )
                }
            )
        }
    }

    fun downloadAndInstall(context: Context) {
        val current = _uiState.value
        val update = current.update ?: return
        if (current.isDownloading) return

        val appContext = context.applicationContext
        val cachedApk = downloadedApk
        if (cachedApk?.isFile == true && downloadedForFileName == update.fileName) {
            launchInstaller(context, cachedApk)
            return
        }


        val checker = resolveChecker(appContext) ?: return
        _uiState.value = current.copy(
            isDownloading = true,
            downloadProgress = 0f,
            message = null
        )
        viewModelScope.launch {
            checker.downloadApk(update) { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }.fold(
                onSuccess = { file ->
                    downloadedApk = file
                    downloadedForFileName = update.fileName
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        downloadProgress = 1f
                    )
                    launchInstaller(context, file)
                },
                onFailure = { error ->
                    LyraLogger.w(TAG, "Update download failed: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        downloadProgress = 0f,
                        message = appContext.getString(R.string.update_download_failed)
                    )
                }
            )
        }
    }

    /** 优先公开更新源（无需登录），否则退回已保存的 WebDAV 账号。 */
    private fun resolveChecker(context: Context): UpdateChecker? {
        checker?.let { return it }
        val repo = AppUpdateSource.GITHUB_RELEASES_REPO
            .ifBlank { BuildConfig.UPDATE_REPO }
        val publicUrl = AppUpdateSource.PUBLIC_UPDATE_URL
        val resolved: UpdateChecker? = when {
            GitHubReleaseChecker.isValidRepo(repo) -> GitHubReleaseChecker(context, repo)
            publicUrl.isNotBlank() -> PublicUpdateChecker(context, publicUrl)
            else -> WebDavUpdateChecker(context).takeIf { it.isConfigured() }
        }
        checker = resolved
        return resolved
    }

    private fun launchInstaller(context: Context, apkFile: File) {
        when (ApkInstaller.install(context, apkFile)) {
            ApkInstaller.Result.LAUNCHED -> {
                _uiState.value = _uiState.value.copy(
                    message = null,
                    showStartupDialog = false
                )
            }

            ApkInstaller.Result.NEEDS_UNKNOWN_SOURCE_PERMISSION -> {
                _uiState.value = _uiState.value.copy(
                    message = context.getString(R.string.update_install_permission_required)
                )
            }

            ApkInstaller.Result.FAILED -> {
                _uiState.value = _uiState.value.copy(
                    message = context.getString(R.string.update_install_failed)
                )
            }
        }
    }

    private companion object {
        private const val TAG = "AppUpdateViewModel"
    }
}
