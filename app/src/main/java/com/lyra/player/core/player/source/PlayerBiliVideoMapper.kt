package com.lyra.player.core.player.source

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
 * File: com.lyra.player.core.player.source/PlayerBiliVideoMapper
 * Updated: 2026/3/23
 */

import com.lyra.player.core.api.bili.buildBiliSongAlbum
import com.lyra.player.ui.viewmodel.playlist.BiliVideoItem
import com.lyra.player.data.model.SongItem

internal fun BiliVideoItem.toSongItem(): SongItem {
    return SongItem(
        id = id,
        name = title,
        artist = uploader,
        album = buildBiliSongAlbum(bvid = bvid),
        albumId = 0,
        durationMs = durationSec * 1000L,
        coverUrl = coverUrl,
        channelId = "bilibili",
        audioId = id.toString()
    )
}
