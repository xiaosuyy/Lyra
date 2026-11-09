package com.lyra.player.ui.screen.tab.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.lyra.player.R
import com.lyra.player.data.settings.AutoSettingsSchema
import com.lyra.player.data.settings.SettingsRepository
import com.lyra.player.data.update.UpdateDownloadSource
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsChoiceRow
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsDialog
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsTextButton
import kotlinx.coroutines.launch

@Composable
internal fun UpdateDownloadSourceSetting(
    repository: SettingsRepository
) {
    val scope = rememberCoroutineScope()
    val source by repository.updateDownloadSourceFlow.collectAsState(
        initial = UpdateDownloadSource.GitHub
    )
    var showDialog by remember { mutableStateOf(false) }

    AutoSettingSpecListItem(
        setting = AutoSettingsSchema.network.updateDownloadSource,
        supportingContent = { Text(updateDownloadSourceLabel(source)) },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onClick = { showDialog = true }
    )

    if (!showDialog) return

    MiuixSettingsDialog(
        onDismissRequest = { showDialog = false },
        title = { Text(stringResource(R.string.settings_update_download_source)) },
        text = {
            Column {
                UpdateDownloadSource.entries.forEach { option ->
                    MiuixSettingsChoiceRow(
                        title = updateDownloadSourceLabel(option),
                        selected = option == source,
                        onClick = {
                            scope.launch { repository.setUpdateDownloadSource(option) }
                            showDialog = false
                        }
                    )
                }
            }
        },
        confirmButton = {
            MiuixSettingsTextButton(
                onClick = { showDialog = false },
                text = { Text(stringResource(R.string.action_close)) }
            )
        }
    )
}

@Composable
private fun updateDownloadSourceLabel(source: UpdateDownloadSource): String = stringResource(
    when (source) {
        UpdateDownloadSource.GitHub -> R.string.settings_update_download_source_github
        UpdateDownloadSource.GhProxy -> R.string.settings_update_download_source_ghproxy
    }
)
