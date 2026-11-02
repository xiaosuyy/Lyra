package com.lyra.player.ui.viewmodel.tab.home

import com.lyra.player.data.model.SongItem
import com.lyra.player.ui.viewmodel.tab.HomeSectionState
import com.lyra.player.ui.viewmodel.tab.PlaylistSummary

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
 * File: com.lyra.player.ui.viewmodel.tab.home/HomeFeedGroup
 * Created: 2026/9/28
 */

/** 首页分组的表现形式，Home Renderer 据此选择渲染组件（与平台无关）。 */
internal enum class HomeFeedPresentation {
    /** 歌曲列表（横向分页歌曲卡片）。 */
    SONG_LIST,

    /** 歌单网格。 */
    PLAYLIST_GRID,

    /** 歌单横条（雷达歌单）。 */
    PLAYLIST_STRIP
}

/** 分组标题图标（与平台无关，由 Renderer 映射为具体矢量图）。 */
internal enum class HomeFeedIcon {
    BOLT,
    RADAR,
    EXPLORE,
    STAR,
    MUSIC
}

/**
 * 统一首页分组。
 *
 * 网易云、QQ、自定义音源产出的首页内容都收敛成这个模型；
 * Home UI 只根据 [presentation] / [icon] 渲染，不关心来源平台。
 */
internal data class HomeFeedGroup(
    val key: String,
    val titleResId: Int,
    val icon: HomeFeedIcon,
    val presentation: HomeFeedPresentation,
    /** 来源音源 id（用于播放 / 跳转 / 诊断），仅作透传。 */
    val sourceId: String? = null,
    /** 刷新时是否整体轮换一屏（网易云分区的现有行为）。 */
    val rotateWithRefresh: Boolean = false,
    val songs: HomeSectionState<SongItem> = HomeSectionState(),
    val playlists: HomeSectionState<PlaylistSummary> = HomeSectionState()
)
