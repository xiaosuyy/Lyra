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
 * File: com.lyra.player.core.source/MusicSourceIds
 * Created: 2026/9/28
 */

/**
 * UI / 搜索层使用的音源稳定标识。
 *
 * 与 lx-music 脚本的音源 id（kw / kg / mg / wy / tx）是两套词汇：
 * 这里是与 [com.lyra.player.data.model.SongItem.channelId] 对齐的“平台 id”。
 * 两套词汇之间的映射统一放在
 * [com.lyra.player.core.customsource.customSourceIdForChannelId]。
 */
internal object MusicSourceIds {
    const val NETEASE = "netease"
    const val BILIBILI = "bilibili"
    const val YOUTUBE_MUSIC = "youtubeMusic"
    const val KUWO = "kuwo"
    const val KUGOU = "kugou"
    const val MIGU = "migu"
    const val TENCENT = "tencent"

    /** GD 音乐台 API 内置音源。 */
    const val GDSTUDIO = "gdstudio"
}
