package moe.ouom.neriplayer.ui.theme.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.data.model.SongItem

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * File: moe.ouom.neriplayer.ui.theme.designsystem/DesignSystemGallery
 * Created: 2026/9/28
 */

/**
 * Design System 画布：集中展示 Typography / Color / Button / Card / SongRow / SectionHeader。
 * 仅用于 Preview 与后续调试，不接入导航、不影响业务。
 */
@Composable
fun DesignSystemGallery(
    dark: Boolean = false,
    dynamicAccent: Color? = null
) {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = if (dynamicAccent != null) base.copy(primary = dynamicAccent) else base
    val colors = neriColorsFrom(scheme, dark)
    val typography = remember { neriTypography() }
    val textStyles = remember(typography) { neriTextStylesFrom(typography) }

    CompositionLocalProvider(
        LocalNeriColors provides colors,
        LocalNeriTextStyles provides textStyles
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .verticalScroll(rememberScrollState())
                    .padding(NeriSpacing.LG),
                verticalArrangement = Arrangement.spacedBy(NeriSpacing.LG)
            ) {
                Text("Typography", style = textStyles.title, color = colors.primaryText)
                Text("Display", style = textStyles.display, color = colors.primaryText)
                Text("Large Title", style = textStyles.largeTitle, color = colors.primaryText)
                Text("Title", style = textStyles.title, color = colors.primaryText)
                Text("Headline", style = textStyles.headline, color = colors.primaryText)
                Text("Body", style = textStyles.body, color = colors.primaryText)
                Text("Body Secondary", style = textStyles.bodySecondary, color = colors.secondaryText)
                Text("Caption", style = textStyles.caption, color = colors.primaryText)
                Text("Caption Secondary", style = textStyles.captionSecondary, color = colors.tertiaryText)

                NeriSectionHeader(title = "Colors", subtitle = "Semantic tokens")

                Text("Colors", style = textStyles.title, color = colors.primaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(NeriSpacing.SM)) {
                    ColorSwatch("Accent", colors.accent)
                    ColorSwatch("Surface", colors.surface)
                    ColorSwatch("Elevated", colors.elevatedSurface)
                    ColorSwatch("Error", colors.error)
                }

                Text("Buttons", style = textStyles.title, color = colors.primaryText)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NeriSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeriPrimaryButton(onClick = {}) { Text("Play") }
                    NeriSecondaryButton(onClick = {}) { Text("Follow") }
                    NeriIconButton(icon = Icons.Filled.PlayArrow, contentDescription = "play", onClick = {})
                    NeriIconButton(icon = Icons.Filled.Shuffle, contentDescription = "shuffle", onClick = {})
                    NeriIconButton(icon = Icons.Filled.Favorite, contentDescription = "favorite", onClick = {})
                }

                Text("Cards", style = textStyles.title, color = colors.primaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(NeriSpacing.MD)) {
                    NeriAlbumCard(
                        title = "Album",
                        subtitle = "Artist",
                        coverUrl = null,
                        onClick = {},
                        modifier = Modifier.size(140.dp)
                    )
                    NeriPlaylistCard(
                        title = "Playlist",
                        subtitle = "12 songs",
                        coverUrl = null,
                        onClick = {},
                        modifier = Modifier.size(140.dp)
                    )
                }

                NeriSectionHeader(
                    title = "Songs",
                    subtitle = "Unified SongRow",
                    moreText = "More",
                    onMoreClick = {}
                )
                NeriSongRow(song = sampleSong("正在播放的歌", "Artist A"), isCurrent = true, onClick = {}, onMore = {})
                NeriSongRow(song = sampleSong("普通歌曲", "Artist B"), sourceLabel = "QQ", onClick = {}, onMore = {})
                NeriSongRow(song = sampleSong("另一首", "Artist C"), onClick = {})

                Text("Spacing", style = textStyles.title, color = colors.primaryText)
                Text(
                    "XS ${NeriSpacing.XS.value.toInt()} · SM ${NeriSpacing.SM.value.toInt()} · MD " +
                        "${NeriSpacing.MD.value.toInt()} · LG ${NeriSpacing.LG.value.toInt()} · XL " +
                        "${NeriSpacing.XL.value.toInt()} · XXL ${NeriSpacing.XXL.value.toInt()}",
                    style = textStyles.caption,
                    color = colors.secondaryText
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(NeriRadius.Medium))
                .background(color)
        )
        Spacer(Modifier.height(NeriSpacing.XS))
        Text(
            text = label,
            style = LocalNeriTextStyles.current.captionSecondary,
            color = LocalNeriColors.current.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun sampleSong(name: String, artist: String) = SongItem(
    id = name.hashCode().toLong(),
    name = name,
    artist = artist,
    album = "Album",
    albumId = 0L,
    durationMs = 200_000L,
    coverUrl = null
)

@Preview(name = "DesignSystem Light", showBackground = true)
@Composable
private fun DesignSystemGalleryLightPreview() {
    DesignSystemGallery(dark = false)
}

@Preview(name = "DesignSystem Dark", showBackground = true, uiMode = 0x20)
@Composable
private fun DesignSystemGalleryDarkPreview() {
    DesignSystemGallery(dark = true)
}

@Preview(name = "DesignSystem Dynamic", showBackground = true)
@Composable
private fun DesignSystemGalleryDynamicPreview() {
    DesignSystemGallery(dark = false, dynamicAccent = Color(0xFF2E5AAC))
}
