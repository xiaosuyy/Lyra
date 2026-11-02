package com.lyra.player.ui.screen.tab.settings.component

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
 * File: com.lyra.player.ui.screen.tab.settings.component/NotificationPermissionSetting
 * Created: 2026/11/3
 */

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lyra.player.R
import com.lyra.player.core.startup.permission.StartupNotificationPermission
import com.lyra.player.ui.feedback.AppFeedback

private val NotificationPermissionItemShape = RoundedCornerShape(18.dp)

/**
 * 手动申请播放通知权限的入口。
 * 首次启动引导移除后，这里是全应用唯一的 POST_NOTIFICATIONS 运行时申请点。
 */
@Composable
fun NotificationPermissionSetting(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val permission = StartupNotificationPermission.permission
    val permissionSupported = StartupNotificationPermission.isSupported()
    var isGranted by remember {
        mutableStateOf(
            !permissionSupported || ContextCompat.checkSelfPermission(context, permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val openSystemNotificationSettings = {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        runCatching { context.startActivity(intent) }.onFailure {
            AppFeedback.show(context = context, message = context.getString(R.string.settings_notification_permission_unavailable))
        }
    }
    val requestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
        if (!granted) {
            AppFeedback.show(
                context = context,
                message = context.getString(R.string.settings_notification_permission_denied_hint)
            )
        }
    }

    val descriptionRes = when {
        !permissionSupported -> R.string.settings_notification_permission_not_required
        isGranted -> R.string.settings_notification_permission_granted
        else -> R.string.settings_notification_permission_denied
    }

    ListItem(
        modifier = modifier
            .clip(NotificationPermissionItemShape)
            .clickable {
                if (!permissionSupported || isGranted) {
                    openSystemNotificationSettings()
                } else {
                    requestLauncher.launch(permission)
                }
            },
        headlineContent = { Text(stringResource(R.string.settings_notification_permission_title)) },
        supportingContent = { Text(stringResource(descriptionRes)) },
        leadingContent = {
            Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null)
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}
