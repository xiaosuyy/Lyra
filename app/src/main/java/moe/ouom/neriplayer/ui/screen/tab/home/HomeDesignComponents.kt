package moe.ouom.neriplayer.ui.screen.tab.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.ouom.neriplayer.ui.theme.designsystem.LocalNeriColors
import moe.ouom.neriplayer.ui.theme.designsystem.LocalNeriTextStyles
import moe.ouom.neriplayer.ui.theme.designsystem.NeriRadius
import moe.ouom.neriplayer.ui.theme.designsystem.NeriSpacing

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
 * File: moe.ouom.neriplayer.ui.screen.tab.home/HomeDesignComponents
 * Created: 2026/9/28
 */

/**
 * Home Hero：大型封面 + 标题/副标题，使用非常克制的 Dynamic Color 氛围（低透明强调色底）。
 * 不做廉价大渐变，也不把整页染成高饱和颜色。
 */
@Composable
internal fun HomeHero(
    title: String,
    subtitle: String?,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeriColors.current
    val textStyles = LocalNeriTextStyles.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NeriRadius.XLarge))
            .background(colors.accent.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(NeriSpacing.LG)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(NeriRadius.Large))
                    .background(colors.elevatedSurface)
            )
            Spacer(Modifier.width(NeriSpacing.LG))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = textStyles.largeTitle,
                    color = colors.primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.width(NeriSpacing.XS))
                    Text(
                        text = subtitle,
                        style = textStyles.bodySecondary,
                        color = colors.secondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
