package com.lyra.player.ui.viewmodel.tab.library

import com.lyra.player.data.model.SongItem

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
 * File: com.lyra.player.ui.viewmodel.tab.library/LibraryModels
 * Created: 2026/9/28
 */

/** 媒体库内容类型（用于表达能力，而不是判断平台）。 */
internal enum class LibraryKind {
    SONG,
    PLAYLIST,
    ALBUM,
    ARTIST
}

/** 音源提供媒体库的能力。 */
internal data class LibraryCapabilities(
    val kinds: Set<LibraryKind> = emptySet()
) {
    fun supports(kind: LibraryKind): Boolean = kind in kinds
}

/** 统一歌单引用（跨平台）。 */
internal data class LibraryPlaylistRef(
    val sourceId: String,
    val id: String,
    val name: String,
    val coverUrl: String? = null,
    val songCount: Int = 0,
    val subtitle: String? = null
)

/** 统一专辑引用（跨平台）。 */
internal data class LibraryAlbumRef(
    val sourceId: String,
    val id: String,
    val name: String,
    val coverUrl: String? = null,
    val artistName: String? = null,
    val songCount: Int = 0
)

/** 统一艺术家引用（跨平台）。 */
internal data class LibraryArtistRef(
    val sourceId: String,
    val id: String,
    val name: String,
    val coverUrl: String? = null
)

/**
 * 统一媒体库条目。UI 只消费本模型，`sourceId` 用于播放 / 详情 / 收藏 / 删除 / 下载 / 跳转。
 */
internal sealed interface LibraryItem {
    val sourceId: String

    data class Song(val song: SongItem) : LibraryItem {
        override val sourceId: String get() = song.channelId ?: LOCAL_SOURCE_ID
    }

    data class Playlist(val ref: LibraryPlaylistRef) : LibraryItem {
        override val sourceId: String get() = ref.sourceId
    }

    data class Album(val ref: LibraryAlbumRef) : LibraryItem {
        override val sourceId: String get() = ref.sourceId
    }

    data class Artist(val ref: LibraryArtistRef) : LibraryItem {
        override val sourceId: String get() = ref.sourceId
    }
}

/** 媒体库分区语义（用于标题/入口归类，不表示平台）。 */
internal enum class LibrarySectionKind {
    LOCAL,
    FAVORITE,
    DOWNLOAD,
    HISTORY,
    PLAYLIST,
    ALBUM,
    ARTIST
}

/** 分区的渲染形式，Library Renderer 据此选择组件（与平台无关）。 */
internal enum class LibraryPresentation {
    SONG_LIST,
    PLAYLIST_LIST,
    ALBUM_GRID,
    ARTIST_GRID
}

/** 统一媒体库分区。 */
internal data class LibrarySection(
    val id: String,
    val sourceId: String,
    val titleResId: Int,
    val kind: LibrarySectionKind,
    val presentation: LibraryPresentation,
    val items: List<LibraryItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

/** 本地来源的稳定标识（本地 / 下载等无平台来源）。 */
internal const val LOCAL_SOURCE_ID = "local"
