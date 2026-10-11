package moe.ouom.neriplayer.core.source

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
 * File: moe.ouom.neriplayer.core.source/MusicSourceIds
 * Created: 2026/9/28
 */

/**
 * UI / 搜索层使用的音源稳定标识。
 *
 * 与 lx-music 脚本的音源 id（kw / kg / mg / wy / tx）是两套词汇：
 * 这里是与 [moe.ouom.neriplayer.data.model.SongItem.channelId] 对齐的“平台 id”。
 * 两套词汇之间的映射统一放在
 * [moe.ouom.neriplayer.core.customsource.customSourceIdForChannelId]。
 */
internal object MusicSourceIds {
    const val NETEASE = "netease"
    const val BILIBILI = "bilibili"
    const val YOUTUBE_MUSIC = "youtubeMusic"
    const val KUWO = "kuwo"
    const val KUGOU = "kugou"
    const val MIGU = "migu"
    const val TENCENT = "tencent"
}
