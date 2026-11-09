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
 * File: com.lyra.player.ui.screen.tab.settings.component/SettingsLyricsSection
 * Updated: 2026/4/13
 */

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import com.lyra.player.R
import com.lyra.player.data.settings.FloatingLyricsPreferences
import com.lyra.player.data.settings.SettingsRepository
import com.lyra.player.data.settings.generated.AutoSettingsRepository
import com.lyra.player.data.settings.generated.AutoSettingsScopes
import com.lyra.player.data.settings.generated.AutoSettingsSwitchItems
import com.lyra.player.ui.screen.tab.settings.page.MiuixSettingsSectionCard
import com.lyra.player.ui.screen.tab.settings.page.MiuixSettingsSectionIntro

@Composable
internal fun SettingsLyricsSection(
    expanded: Boolean,
    arrowRotation: Float,
    onExpandedChange: (Boolean) -> Unit,
    showHeader: Boolean = true,
    autoSettingsRepository: AutoSettingsRepository,
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    floatingLyricsPreferences: FloatingLyricsPreferences,
    onFloatingLyricsPreferencesChange: (FloatingLyricsPreferences) -> Unit,
    lyricsAppearanceContent: @Composable () -> Unit,
    cardIndex: Int? = null,
    highlightTargetId: String? = null,
    highlightPulse: Int = 0,
    onHighlightFinished: (() -> Unit)? = null
) {
    fun shouldShowCard(index: Int): Boolean = cardIndex == null || cardIndex == index

    if (showHeader) {
        ExpandableHeader(
            icon = Icons.Outlined.Subtitles,
            title = stringResource(R.string.settings_lyrics_offset),
            subtitleCollapsed = stringResource(R.string.settings_lyrics_offset_expand),
            subtitleExpanded = stringResource(R.string.settings_login_platforms_collapse),
            expanded = expanded,
            onToggle = { onExpandedChange(!expanded) },
            arrowRotation = arrowRotation
        )
    }

    LazyAnimatedVisibility(visible = expanded || !showHeader) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (showHeader) 16.dp else 0.dp,
                    end = if (showHeader) 8.dp else 0.dp,
                    bottom = if (showHeader) 8.dp else 0.dp
                )
        ) {
            if (shouldShowCard(0)) LyricsDetailCard(
                showCard = !showHeader,
                highlightPulse = highlightPulse,
            ) {
                MiuixSettingsSectionIntro(
                    title = stringResource(R.string.settings_lyrics_floating_section),
                )
                SettingsFloatingLyricsSection(
                    preferences = floatingLyricsPreferences,
                    onPreferencesChange = onFloatingLyricsPreferencesChange,
                    highlightTargetId = highlightTargetId,
                    highlightPulse = highlightPulse,
                    onHighlightFinished = onHighlightFinished
                )
            }
            if (cardIndex == null) LyricsDetailGap(showHeader)
            if (shouldShowCard(1)) LyricsDetailCard(
                showCard = !showHeader,
                highlightPulse = highlightPulse,
            ) {
                MiuixSettingsSectionIntro(
                    title = stringResource(R.string.settings_lyrics_source_section),
                )
                AutoSettingsSwitchItems(
                    repository = autoSettingsRepository,
                    scope = scope,
                    sectionScope = AutoSettingsScopes.lyrics,
                    highlightTargetId = highlightTargetId,
                    highlightPulse = highlightPulse,
                    onHighlightFinished = onHighlightFinished
                )
                DynamicIslandLyricsSetting(
                    repository = settingsRepository,
                    highlightTargetId = highlightTargetId,
                    highlightPulse = highlightPulse,
                    onHighlightFinished = onHighlightFinished
                )
            }
            if (cardIndex == null) LyricsDetailGap(showHeader)
            if (shouldShowCard(2)) LyricsDetailCard(
                showCard = !showHeader,
                highlightPulse = highlightPulse,
            ) {
                lyricsAppearanceContent()
            }
        }
    }
}

@Composable
private fun LyricsDetailCard(
    showCard: Boolean,
    highlightPulse: Int = 0,
    content: @Composable () -> Unit
) {
    if (showCard) {
        MiuixSettingsSectionCard(
            highlighted = false,
            highlightPulse = highlightPulse,
            onHighlightFinished = null,
            content = content
        )
    } else {
        content()
    }
}

@Composable
private fun LyricsDetailGap(showHeader: Boolean) {
    if (!showHeader) {
        Spacer(Modifier.height(12.dp))
    }
}


