package moe.ouom.neriplayer.ui.screen.tab.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibraryAlbumRef
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibraryItem
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibraryPlaylistRef
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibraryPresentation
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibrarySection
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
 * File: moe.ouom.neriplayer.ui.screen.tab.library/LibraryUnifiedList
 * Created: 2026/9/28
 */

/**
 * 统一媒体库渲染器：只根据 [LibrarySection.presentation] / [LibraryItem] 渲染，不关心平台。
 */
@Composable
internal fun LibraryUnifiedContent(
    sections: List<LibrarySection>,
    onPlaylistClick: (LibraryPlaylistRef) -> Unit,
    onAlbumClick: (LibraryAlbumRef) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = NeriSpacing.SM)
    ) {
        sections.forEach { section ->
            item(key = "${section.id}:header") {
                Text(
                    text = stringResource(section.titleResId),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = NeriSpacing.LG, vertical = 10.dp)
                )
            }
            when (section.presentation) {
                LibraryPresentation.PLAYLIST_LIST -> section.items.forEach { item ->
                    if (item is LibraryItem.Playlist) {
                        item(key = "${section.id}:${item.ref.id}") {
                            LibraryPlaylistRow(
                                ref = item.ref,
                                onClick = { onPlaylistClick(item.ref) }
                            )
                        }
                    }
                }

                LibraryPresentation.ALBUM_GRID -> section.items.forEach { item ->
                    if (item is LibraryItem.Album) {
                        item(key = "${section.id}:${item.ref.id}") {
                            LibraryAlbumRow(
                                ref = item.ref,
                                onClick = { onAlbumClick(item.ref) }
                            )
                        }
                    }
                }

                // 该来源暂未提供这些形式的内容
                LibraryPresentation.SONG_LIST,
                LibraryPresentation.ARTIST_GRID -> Unit
            }
            if (section.loading) {
                item(key = "${section.id}:loading") {
                    Text(
                        text = stringResource(R.string.home_loading),
                        style = LocalNeriTextStyles.current.captionSecondary,
                        modifier = Modifier.padding(horizontal = NeriSpacing.LG, vertical = NeriSpacing.SM)
                    )
                }
            }
            if (!section.error.isNullOrBlank()) {
                item(key = "${section.id}:error") {
                    Text(
                        text = section.error,
                        color = LocalNeriColors.current.error,
                        style = LocalNeriTextStyles.current.captionSecondary,
                        modifier = Modifier.padding(horizontal = NeriSpacing.LG, vertical = NeriSpacing.SM)
                    )
                }
            }
            item(key = "${section.id}:spacer") { Spacer(Modifier.height(NeriSpacing.SM)) }
        }
    }
}

@Composable
private fun LibraryPlaylistRow(
    ref: LibraryPlaylistRef,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            AsyncImage(
                model = ref.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(NeriRadius.Small))
            )
        },
        headlineContent = {
            Text(text = ref.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            val subtitle = ref.subtitle?.takeIf { it.isNotBlank() }
                ?: if (ref.songCount > 0) "${ref.songCount}" else null
            if (subtitle != null) {
                Text(text = subtitle, style = LocalNeriTextStyles.current.captionSecondary)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun LibraryAlbumRow(
    ref: LibraryAlbumRef,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            AsyncImage(
                model = ref.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(NeriRadius.Small))
            )
        },
        headlineContent = {
            Text(text = ref.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            val subtitle = ref.artistName?.takeIf { it.isNotBlank() }
                ?: if (ref.songCount > 0) "${ref.songCount}" else null
            if (subtitle != null) {
                Text(text = subtitle, style = LocalNeriTextStyles.current.captionSecondary)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
