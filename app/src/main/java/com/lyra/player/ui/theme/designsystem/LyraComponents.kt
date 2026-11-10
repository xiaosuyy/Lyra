package com.lyra.player.ui.theme.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lyra.player.data.model.SongItem
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayCoverUrl
import com.lyra.player.data.model.displayName

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
 * File: com.lyra.player.ui.theme.designsystem/LyraComponents
 * Created: 2026/9/28
 */

// ---------------- Buttons ----------------

@Composable
fun LyraPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(LyraIconSize.TouchTarget),
        enabled = enabled,
        shape = RoundedCornerShape(LyraRadius.Full)
    ) {
        content()
    }
}

@Composable
fun LyraSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(LyraIconSize.TouchTarget),
        enabled = enabled,
        shape = RoundedCornerShape(LyraRadius.Full)
    ) {
        content()
    }
}

@Composable
fun LyraTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        content()
    }
}

@Composable
fun LyraIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalNeriColors.current.primaryText
) {
    IconButton(onClick = onClick, modifier = modifier.size(LyraIconSize.TouchTarget), enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(LyraIconSize.Large),
            tint = tint
        )
    }
}

@Composable
fun LyraFloatingIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(LyraIconSize.TouchTarget),
        enabled = enabled,
        shape = RoundedCornerShape(LyraRadius.Full)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(LyraIconSize.Large)
        )
    }
}

// ---------------- Cards ----------------

enum class LyraCardKind { Generic, Album, Playlist, Artist }

@Composable
fun LyraMediaCard(
    title: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    kind: LyraCardKind = LyraCardKind.Generic,
    overlay: (@Composable BoxScope.() -> Unit)? = null
) {
    val shape = RoundedCornerShape(LyraRadius.Large)
    Column(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(LocalNeriColors.current.elevatedSurface)
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            overlay?.invoke(this)
        }
        Spacer(Modifier.height(LyraSpacing.SM))
        Text(
            text = title,
            style = LocalNeriTextStyles.current.caption,
            color = LocalNeriColors.current.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = LocalNeriTextStyles.current.captionSecondary,
                color = LocalNeriColors.current.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LyraAlbumCard(title: String, coverUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) =
    LyraMediaCard(title, coverUrl, onClick, modifier, subtitle, LyraCardKind.Album)

@Composable
fun LyraPlaylistCard(title: String, coverUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) =
    LyraMediaCard(title, coverUrl, onClick, modifier, subtitle, LyraCardKind.Playlist)

@Composable
fun LyraArtistCard(title: String, coverUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) =
    LyraMediaCard(
        title = title,
        coverUrl = coverUrl,
        onClick = onClick,
        modifier = modifier,
        subtitle = subtitle,
        kind = LyraCardKind.Artist,
        overlay = null
    )

// ---------------- SectionHeader ----------------

@Composable
fun LyraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = LocalNeriColors.current.primaryText.copy(alpha = 0.60f),
    moreText: String? = null,
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LyraSpacing.LG, vertical = LyraSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(LyraIconSize.Medium)
                    .padding(end = LyraSpacing.XS)
            )
            Spacer(Modifier.width(LyraSpacing.XS))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = LocalNeriTextStyles.current.title,
                color = LocalNeriColors.current.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = LocalNeriTextStyles.current.captionSecondary,
                    color = LocalNeriColors.current.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (moreText != null && onMoreClick != null) {
            LyraTextButton(onClick = onMoreClick) {
                Text(text = moreText, style = LocalNeriTextStyles.current.caption)
            }
        }
    }
}

// ---------------- SongRow ----------------

/**
 * 统一歌曲行：封面 / 名称 / 艺术家 / 来源（弱化）/ 更多 / 当前播放高亮。
 * 视觉保持干净，来源只作为次要信息，不做醒目标签。
 */
@Composable
fun LyraSongRow(
    song: SongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    sourceLabel: String? = null,
    showArtist: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onMore: (() -> Unit)? = null
) {
    val colors = LocalNeriColors.current
    val textStyles = LocalNeriTextStyles.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LyraRadius.Medium))
            .clickable(onClick = onClick)
            .padding(horizontal = LyraSpacing.LG, vertical = LyraSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.displayCoverUrl(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(LyraRadius.Small))
                .background(colors.elevatedSurface)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.displayName(),
                style = textStyles.body,
                color = if (isCurrent) colors.accent else colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showArtist) {
                Text(
                    text = song.displayArtist(),
                    style = textStyles.captionSecondary,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (sourceLabel != null) {
            Spacer(Modifier.width(LyraSpacing.SM))
            Text(
                text = sourceLabel,
                style = textStyles.captionSecondary,
                color = colors.tertiaryText,
                maxLines = 1
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(LyraSpacing.SM))
            trailing()
        }
        if (onMore != null) {
            IconButton(onClick = onMore, modifier = Modifier.size(LyraIconSize.TouchTarget)) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                    modifier = Modifier.size(LyraIconSize.Medium),
                    tint = colors.tertiaryText
                )
            }
        }
    }
}
