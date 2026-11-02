package com.lyra.player.data.local.playlist.model

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
 * File: com.lyra.player.data.local.playlist.model/LocalPlaylist
 * Updated: 2026/3/23
 */

import com.lyra.player.data.model.SongItem

data class LocalPlaylist(
    val id: Long,
    val name: String,
    val songs: MutableList<SongItem> = mutableListOf(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val customCoverUrl: String? = null,
    val songOrderVersion: Int = DISPLAY_ORDER_SONG_ORDER_VERSION
)

const val LEGACY_SONG_ORDER_VERSION = 0
const val DISPLAY_ORDER_SONG_ORDER_VERSION = 1
