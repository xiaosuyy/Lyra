package com.lyra.player.ui.viewmodel.tab.home

import com.lyra.player.R
import com.lyra.player.data.settings.HomeSectionId
import com.lyra.player.ui.viewmodel.tab.HomeUiState
import com.lyra.player.ui.viewmodel.tab.NeteaseHomeSongSource

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
 * File: com.lyra.player.ui.viewmodel.tab.home/NeteaseHomeFeedGroups
 * Created: 2026/9/28
 */

/**
 * 把网易云首页的既有数据（[HomeUiState] 中的各个分区）适配成统一 [HomeFeedGroup]。
 *
 * 只改变「怎么进入 UI」，不改动网易云的取数与内容：
 * - 热力/新歌等歌曲来源 → [HomeFeedPresentation.SONG_LIST]
 * - 私人雷达歌曲 → SONG_LIST；雷达歌单 → PLAYLIST_STRIP
 * - 推荐歌单 → PLAYLIST_GRID
 */
internal object NeteaseHomeFeedGroups {

    fun build(ui: HomeUiState): Map<HomeSectionId, List<HomeFeedGroup>> {
        val trending = ui.trendingSongSections.map { section ->
            songGroup(groupKey = "netease:trending", source = section.source, songs = section.section)
        }

        val radarPersonal = ui.radarSongSections
            .filter { it.source == NeteaseHomeSongSource.PERSONAL_RADAR }
            .map { section ->
                songGroup(groupKey = "netease:radar", source = section.source, songs = section.section)
            }
        val radarOthers = ui.radarSongSections
            .filter { it.source != NeteaseHomeSongSource.PERSONAL_RADAR }
            .map { section ->
                songGroup(groupKey = "netease:radar", source = section.source, songs = section.section)
            }
        val radarPlaylists = HomeFeedGroup(
            key = "netease:radar:playlists",
            titleResId = R.string.home_netease_radar_playlists,
            icon = HomeFeedIcon.EXPLORE,
            presentation = HomeFeedPresentation.PLAYLIST_STRIP,
            sourceId = "netease",
            rotateWithRefresh = true,
            playlists = ui.radarPlaylists
        )

        val recommended = ui.playlistSections.map { section ->
            HomeFeedGroup(
                key = "netease:recommended:${section.source}",
                titleResId = section.source.titleRes,
                icon = HomeFeedIcon.STAR,
                presentation = HomeFeedPresentation.PLAYLIST_GRID,
                sourceId = "netease",
                rotateWithRefresh = true,
                playlists = section.section
            )
        }

        return mapOf(
            HomeSectionId.TRENDING to trending,
            HomeSectionId.RADAR to (radarPersonal + radarPlaylists + radarOthers),
            HomeSectionId.RECOMMENDED to recommended
        )
    }

    private fun songGroup(
        groupKey: String,
        source: NeteaseHomeSongSource,
        songs: com.lyra.player.ui.viewmodel.tab.HomeSectionState<com.lyra.player.data.model.SongItem>
    ): HomeFeedGroup = HomeFeedGroup(
        key = "$groupKey:${source.name}",
        titleResId = source.titleRes,
        icon = iconFor(source),
        presentation = HomeFeedPresentation.SONG_LIST,
        sourceId = "netease",
        rotateWithRefresh = true,
        songs = songs
    )

    private fun iconFor(source: NeteaseHomeSongSource): HomeFeedIcon = when (source) {
        NeteaseHomeSongSource.PERSONAL_RADAR -> HomeFeedIcon.RADAR
        NeteaseHomeSongSource.TOP_SOARING,
        NeteaseHomeSongSource.PERSONALIZED_NEW_SONGS,
        NeteaseHomeSongSource.TOP_HOT,
        NeteaseHomeSongSource.TOP_NEW -> HomeFeedIcon.BOLT
        NeteaseHomeSongSource.DAILY_RECOMMEND,
        NeteaseHomeSongSource.PRIVATE_FM -> HomeFeedIcon.EXPLORE
    }
}
