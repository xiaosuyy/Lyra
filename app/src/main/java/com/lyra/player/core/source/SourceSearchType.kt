package com.lyra.player.core.source

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
 * File: com.lyra.player.core.source/SourceSearchType
 * Created: 2026/9/28
 */

/** 搜索类型，对应 [MusicSourceCapabilities] 中的各项能力。 */
enum class SourceSearchType {
    SONG,
    ARTIST,
    ALBUM,
    PLAYLIST,
    RANKING
}

/** 能力与搜索类型的对应关系，统一在此定义，避免各处重复。 */
internal fun MusicSourceCapabilities.supports(type: SourceSearchType): Boolean = when (type) {
    SourceSearchType.SONG -> supportsSong
    SourceSearchType.ARTIST -> supportsArtist
    SourceSearchType.ALBUM -> supportsAlbum
    SourceSearchType.PLAYLIST -> supportsPlaylist
    SourceSearchType.RANKING -> supportsRanking
}
