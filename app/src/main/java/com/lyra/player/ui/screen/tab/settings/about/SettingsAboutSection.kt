package com.lyra.player.ui.screen.tab.settings.about

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
 * File: com.lyra.player.ui.screen.tab.settings.about/SettingsAboutSection
 * Updated: 2026/3/23
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lyra.player.BuildConfig
import com.lyra.player.R
import com.lyra.player.ui.screen.tab.settings.component.settingsItemClickable
import com.lyra.player.ui.viewmodel.AppUpdateViewModel
import com.lyra.player.util.format.convertTimestampToDate
import kotlin.math.roundToInt

internal fun LazyListScope.settingsAboutSection(
    devModeEnabled: Boolean,
    onVersionClick: () -> Unit,
    onCopyValue: (String) -> Unit = {}
) {
    item {
        SettingsAboutContent(
            devModeEnabled = devModeEnabled,
            onVersionClick = onVersionClick,
            onCopyValue = onCopyValue
        )
    }
}

@Composable
internal fun SettingsAboutContent(
    devModeEnabled: Boolean,
    onVersionClick: () -> Unit,
    onCopyValue: (String) -> Unit
) {
    SettingsAboutIntroItem()
    SettingsBuildUuidItem(onCopyValue)
    SettingsVersionItem(
        devModeEnabled = devModeEnabled,
        onVersionClick = onVersionClick,
        onCopyValue = onCopyValue
    )
    SettingsBuildTimeItem(onCopyValue)
    SettingsUpdateSection()
}

@Composable
private fun SettingsUpdateSection() {
    val context = LocalContext.current
    val updateViewModel: AppUpdateViewModel = viewModel()
    val state by updateViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val busy = state.isChecking || state.isDownloading
        SettingsUpdateButton(
            text = stringResource(
                if (state.isChecking) R.string.settings_checking_update
                else R.string.settings_check_update
            ),
            enabled = !busy,
            onClick = { updateViewModel.checkForUpdates(context) }
        )

        state.update?.let { update ->
            val releaseLabel = stringResource(
                R.string.update_release_label,
                stringResource(R.string.app_name),
                update.versionLabel
            )
            Text(
                text = stringResource(
                    R.string.update_available_title,
                    update.versionLabel,
                    releaseLabel
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            SettingsUpdateButton(
                text = if (state.isDownloading) {
                    stringResource(
                        R.string.update_downloading_action,
                        (state.downloadProgress * 100).roundToInt()
                    )
                } else {
                    stringResource(R.string.update_download_action, update.versionLabel)
                },
                enabled = !busy,
                onClick = { updateViewModel.downloadAndInstall(context) }
            )
            if (update.changelog.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.update_changelog_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = update.changelog,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val statusText = when {
            state.isUpToDate -> stringResource(R.string.update_up_to_date)
            state.message != null -> state.message
            else -> null
        }
        statusText?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingsUpdateButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun SettingsAboutIntroItem() {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.settings_about),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.settings_about),
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = { Text(stringResource(R.string.about_app_footer)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsBuildUuidItem(onCopyValue: (String) -> Unit) {
    val buildUuid = BuildConfig.BUILD_UUID

    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Verified,
                contentDescription = stringResource(R.string.settings_build_uuid),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.settings_build_uuid),
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = { Text(buildUuid) },
        modifier = androidx.compose.ui.Modifier.settingsItemClickable {
            onCopyValue(buildUuid)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsVersionItem(
    devModeEnabled: Boolean,
    onVersionClick: () -> Unit,
    onCopyValue: (String) -> Unit
) {
    val suffix = if (devModeEnabled) {
        " (${stringResource(R.string.settings_version_debug_suffix)})"
    } else {
        ""
    }
    val versionName = "${BuildConfig.VERSION_NAME}$suffix"

    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Update,
                contentDescription = stringResource(R.string.settings_version),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.common_version),
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = { Text(versionName) },
        modifier = androidx.compose.ui.Modifier.settingsItemClickable {
            onCopyValue(versionName)
            onVersionClick()
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsBuildTimeItem(onCopyValue: (String) -> Unit) {
    val buildTime = convertTimestampToDate(BuildConfig.BUILD_TIMESTAMP)

    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = stringResource(R.string.settings_build_time),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.common_build_time),
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = { Text(buildTime) },
        modifier = androidx.compose.ui.Modifier.settingsItemClickable {
            onCopyValue(buildTime)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
