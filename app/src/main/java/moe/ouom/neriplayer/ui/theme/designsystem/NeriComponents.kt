package moe.ouom.neriplayer.ui.theme.designsystem

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
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.displayArtist
import moe.ouom.neriplayer.data.model.displayCoverUrl
import moe.ouom.neriplayer.data.model.displayName

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
 * File: moe.ouom.neriplayer.ui.theme.designsystem/NeriComponents
 * Created: 2026/9/28
 */

// ---------------- Buttons ----------------

@Composable
fun NeriPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(NeriIconSize.TouchTarget),
        enabled = enabled,
        shape = RoundedCornerShape(NeriRadius.Full)
    ) {
        content()
    }
}

@Composable
fun NeriSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(NeriIconSize.TouchTarget),
        enabled = enabled,
        shape = RoundedCornerShape(NeriRadius.Full)
    ) {
        content()
    }
}

@Composable
fun NeriTextButton(
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
fun NeriIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalNeriColors.current.primaryText
) {
    IconButton(onClick = onClick, modifier = modifier.size(NeriIconSize.TouchTarget), enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(NeriIconSize.Large),
            tint = tint
        )
    }
}

@Composable
fun NeriFloatingIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(NeriIconSize.TouchTarget),
        enabled = enabled,
        shape = RoundedCornerShape(NeriRadius.Full)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(NeriIconSize.Large)
        )
    }
}

// ---------------- Cards ----------------

enum class NeriCardKind { Generic, Album, Playlist, Artist }

@Composable
fun NeriMediaCard(
    title: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    kind: NeriCardKind = NeriCardKind.Generic,
    overlay: (@Composable BoxScope.() -> Unit)? = null
) {
    val shape = RoundedCornerShape(NeriRadius.Large)
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
        Spacer(Modifier.height(NeriSpacing.SM))
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
fun NeriAlbumCard(title: String, coverUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) =
    NeriMediaCard(title, coverUrl, onClick, modifier, subtitle, NeriCardKind.Album)

@Composable
fun NeriPlaylistCard(title: String, coverUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) =
    NeriMediaCard(title, coverUrl, onClick, modifier, subtitle, NeriCardKind.Playlist)

@Composable
fun NeriArtistCard(title: String, coverUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) =
    NeriMediaCard(
        title = title,
        coverUrl = coverUrl,
        onClick = onClick,
        modifier = modifier,
        subtitle = subtitle,
        kind = NeriCardKind.Artist,
        overlay = null
    )

// ---------------- SectionHeader ----------------

@Composable
fun NeriSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = LocalNeriColors.current.accent,
    moreText: String? = null,
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NeriSpacing.LG, vertical = NeriSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(NeriIconSize.Medium)
                    .padding(end = NeriSpacing.XS)
            )
            Spacer(Modifier.width(NeriSpacing.XS))
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
            NeriTextButton(onClick = onMoreClick) {
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
fun NeriSongRow(
    song: SongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    sourceLabel: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onMore: (() -> Unit)? = null
) {
    val colors = LocalNeriColors.current
    val textStyles = LocalNeriTextStyles.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NeriRadius.Medium))
            .clickable(onClick = onClick)
            .padding(horizontal = NeriSpacing.LG, vertical = NeriSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.displayCoverUrl(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(NeriRadius.Small))
                .background(colors.elevatedSurface)
        )
        Spacer(Modifier.width(NeriSpacing.MD))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.displayName(),
                style = textStyles.body,
                color = if (isCurrent) colors.accent else colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.displayArtist(),
                style = textStyles.captionSecondary,
                color = colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (sourceLabel != null) {
            Spacer(Modifier.width(NeriSpacing.SM))
            Text(
                text = sourceLabel,
                style = textStyles.captionSecondary,
                color = colors.tertiaryText,
                maxLines = 1
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(NeriSpacing.SM))
            trailing()
        }
        if (onMore != null) {
            IconButton(onClick = onMore, modifier = Modifier.size(NeriIconSize.TouchTarget)) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                    modifier = Modifier.size(NeriIconSize.Medium),
                    tint = colors.tertiaryText
                )
            }
        }
    }
}
