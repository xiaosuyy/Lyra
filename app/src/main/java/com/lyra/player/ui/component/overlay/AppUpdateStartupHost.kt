package com.lyra.player.ui.component.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lyra.player.R
import com.lyra.player.data.update.AppUpdateInfo
import com.lyra.player.ui.viewmodel.AppUpdateViewModel
import kotlin.math.roundToInt

/** 启动时静默检查云端 APK，发现新版本后弹出居中的更新说明。 */
@Composable
internal fun AppUpdateStartupHost() {
    val context = LocalContext.current
    val viewModel: AppUpdateViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkForUpdatesOnStartup(context)
    }

    val update = state.update
    if (state.showStartupDialog && update != null) {
        StartupUpdateDialog(
            update = update,
            isDownloading = state.isDownloading,
            downloadProgress = state.downloadProgress,
            message = state.message,
            onInstallNow = { viewModel.downloadAndInstall(context) },
            onLater = viewModel::dismissStartupDialog
        )
    }
}

@Composable
private fun StartupUpdateDialog(
    update: AppUpdateInfo,
    isDownloading: Boolean,
    downloadProgress: Float,
    message: String?,
    onInstallNow: () -> Unit,
    onLater: () -> Unit
) {
    DensityScaledAlertDialog(
        onDismissRequest = onLater,
        title = {
            Text(stringResource(R.string.update_dialog_title, update.versionLabel))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (update.changelog.isBlank()) {
                    Text(
                        text = stringResource(R.string.update_dialog_no_changelog),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.update_changelog_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = update.changelog,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (isDownloading) {
                    Text(
                        text = stringResource(
                            R.string.update_downloading_action,
                            (downloadProgress * 100).roundToInt()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                message?.let { errorText ->
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        // 左「立即更新」、右「稍后」：M3 中 dismissButton 位于 confirmButton 左侧
        dismissButton = {
            Text(
                text = stringResource(R.string.update_dialog_install_now),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(enabled = !isDownloading, onClick = onInstallNow)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        },
        confirmButton = {
            Text(
                text = stringResource(R.string.update_dialog_later),
                modifier = Modifier
                    .clickable(onClick = onLater)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    )
}
