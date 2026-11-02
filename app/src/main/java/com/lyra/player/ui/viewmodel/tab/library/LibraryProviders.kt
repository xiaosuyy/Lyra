package com.lyra.player.ui.viewmodel.tab.library

import com.lyra.player.R
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.ui.viewmodel.tab.AlbumSummary
import com.lyra.player.ui.viewmodel.tab.BiliPlaylist
import com.lyra.player.ui.viewmodel.tab.PlaylistSummary
import com.lyra.player.ui.viewmodel.tab.YouTubeMusicPlaylist

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
 * File: com.lyra.player.ui.viewmodel.tab.library/LibraryProviders
 * Created: 2026/9/28
 */

/**
 * 生成一个媒体库 Provider。取数与平台转换逻辑通过 lambda 注入，Registry/UI 只消费统一模型。
 */
internal fun libraryProvider(
    sourceId: String,
    capabilities: LibraryCapabilities,
    sections: () -> List<LibrarySection>,
    refresh: suspend () -> Unit = {}
): LibrarySourceProvider = object : LibrarySourceProvider {
    override val sourceId: String = sourceId
    override val capabilities: LibraryCapabilities = capabilities
    override fun sections(): List<LibrarySection> = sections()
    override suspend fun refresh() {
        refresh()
    }
}

/** 网易云媒体库 Provider（歌单 + 专辑）。 */
internal fun neteaseLibraryProvider(
    playlists: () -> List<PlaylistSummary>,
    albums: () -> List<AlbumSummary>,
    refresh: suspend () -> Unit
): LibrarySourceProvider = libraryProvider(
    sourceId = MusicSourceIds.NETEASE,
    capabilities = LibraryCapabilities(setOf(LibraryKind.PLAYLIST, LibraryKind.ALBUM)),
    refresh = refresh,
    sections = {
        listOf(
            LibrarySection(
                id = "netease:playlists",
                sourceId = MusicSourceIds.NETEASE,
                titleResId = R.string.library_tab_netease,
                kind = LibrarySectionKind.PLAYLIST,
                presentation = LibraryPresentation.PLAYLIST_LIST,
                items = playlists().map { it.toLibraryItem() }
            ),
            LibrarySection(
                id = "netease:albums",
                sourceId = MusicSourceIds.NETEASE,
                titleResId = R.string.library_tab_netease_album,
                kind = LibrarySectionKind.ALBUM,
                presentation = LibraryPresentation.ALBUM_GRID,
                items = albums().map { it.toLibraryItem() }
            )
        )
    }
)

/** Bilibili 媒体库 Provider（收藏夹 / 合集）。 */
internal fun biliLibraryProvider(
    playlists: () -> List<BiliPlaylist>,
    refresh: suspend () -> Unit
): LibrarySourceProvider = libraryProvider(
    sourceId = MusicSourceIds.BILIBILI,
    capabilities = LibraryCapabilities(setOf(LibraryKind.PLAYLIST)),
    refresh = refresh,
    sections = {
        listOf(
            LibrarySection(
                id = "bilibili:playlists",
                sourceId = MusicSourceIds.BILIBILI,
                titleResId = R.string.library_tab_bilibili,
                kind = LibrarySectionKind.PLAYLIST,
                presentation = LibraryPresentation.PLAYLIST_LIST,
                items = playlists().map { it.toLibraryItem() }
            )
        )
    }
)

/** YouTube Music 媒体库 Provider（歌单）。 */
internal fun youTubeMusicLibraryProvider(
    playlists: () -> List<YouTubeMusicPlaylist>,
    refresh: suspend () -> Unit
): LibrarySourceProvider = libraryProvider(
    sourceId = MusicSourceIds.YOUTUBE_MUSIC,
    capabilities = LibraryCapabilities(setOf(LibraryKind.PLAYLIST)),
    refresh = refresh,
    sections = {
        listOf(
            LibrarySection(
                id = "youtubeMusic:playlists",
                sourceId = MusicSourceIds.YOUTUBE_MUSIC,
                titleResId = R.string.library_tab_youtube_music,
                kind = LibrarySectionKind.PLAYLIST,
                presentation = LibraryPresentation.PLAYLIST_LIST,
                items = playlists().map { it.toLibraryItem() }
            )
        )
    }
)

// ---------------- 平台模型 → 统一模型 ----------------

private fun PlaylistSummary.toLibraryItem(): LibraryItem = LibraryItem.Playlist(
    LibraryPlaylistRef(
        sourceId = MusicSourceIds.NETEASE,
        id = id.toString(),
        name = name,
        coverUrl = picUrl.takeIf { it.isNotBlank() },
        songCount = trackCount
    )
)

private fun AlbumSummary.toLibraryItem(): LibraryItem = LibraryItem.Album(
    LibraryAlbumRef(
        sourceId = MusicSourceIds.NETEASE,
        id = id.toString(),
        name = name,
        coverUrl = picUrl.takeIf { it.isNotBlank() },
        songCount = size
    )
)

private fun BiliPlaylist.toLibraryItem(): LibraryItem = LibraryItem.Playlist(
    LibraryPlaylistRef(
        sourceId = MusicSourceIds.BILIBILI,
        id = mediaId.toString(),
        name = title,
        coverUrl = coverUrl.takeIf { it.isNotBlank() },
        songCount = count,
        subtitle = subtitle.takeIf { it.isNotBlank() }
    )
)

private fun YouTubeMusicPlaylist.toLibraryItem(): LibraryItem = LibraryItem.Playlist(
    LibraryPlaylistRef(
        sourceId = MusicSourceIds.YOUTUBE_MUSIC,
        id = playlistId.ifBlank { browseId },
        name = title,
        coverUrl = coverUrl.takeIf { it.isNotBlank() },
        songCount = trackCount,
        subtitle = subtitle.takeIf { it.isNotBlank() }
    )
)
