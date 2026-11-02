package com.lyra.player.ui.screen.tab.settings.state

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
 * File: com.lyra.player.ui.screen.tab.settings.state/SettingsFlowCompat
 * Updated: 2026/3/23
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import com.lyra.player.R

/** 兼容性：统一走生命周期感知收集，避免设置页后台继续订阅 */
@Composable
internal fun <T> StateFlow<T>.collectAsStateWithLifecycleCompat(): State<T> {
    return collectAsStateWithLifecycle()
}

/** 格式化同步时间 */
@Composable
internal fun formatSyncTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutesAgo = (diff / 60_000).toInt()
    val hoursAgo = (diff / 3_600_000).toInt()
    val daysAgo = (diff / 86_400_000).toInt()

    return when {
        diff < 60_000 -> stringResource(R.string.time_just_now)
        diff < 3_600_000 -> pluralStringResource(R.plurals.time_minutes_ago, minutesAgo, minutesAgo)
        diff < 86_400_000 -> pluralStringResource(R.plurals.time_hours_ago, hoursAgo, hoursAgo)
        else -> pluralStringResource(R.plurals.time_days_ago, daysAgo, daysAgo)
    }
}
