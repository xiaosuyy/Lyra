package com.lyra.player.ui.viewmodel.tab.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lyra.player.core.api.search.CustomChannelCoverCache
import com.lyra.player.core.api.search.CustomChannelSearchApi
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.data.settings.HomeSectionId
import com.lyra.player.ui.viewmodel.tab.HomeSectionState
import com.lyra.player.ui.viewmodel.tab.toSongItem

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
 * File: com.lyra.player.ui.viewmodel.tab.home/ChannelHomeFeedProvider
 * Created: 2026/9/28
 */

/**
 * 渠道音源的首页内容 Provider（当前为 QQ 音乐榜单）。
 *
 * 原逻辑散落在 [com.lyra.player.ui.screen.tab.HomeScreen] 中，这里统一收编为 Provider：
 * Home UI 只调用 [loadSection]，不再直接触碰平台接口。
 */
internal class ChannelHomeFeedProvider(
    override val sourceId: String,
    private val topListIdForSection: (HomeSectionId) -> Int
) : HomeFeedProvider {

    override val supportedSections: Set<HomeSectionId> = setOf(
        HomeSectionId.TRENDING,
        HomeSectionId.RADAR,
        HomeSectionId.RECOMMENDED
    )

    override suspend fun loadSection(
        section: HomeSectionId,
        windowOffset: Int,
        limit: Int
    ): List<HomeFeedGroup> {
        if (!supports(section) || limit <= 0) return emptyList()
        val api = CustomChannelSearchApi(AppContainer.sharedOkHttpClient)
        val app = AppContainer.applicationContext
        val pool = withContext(Dispatchers.IO) {
            api.searchTencentTopList(topListIdForSection(section))
        }
        if (pool.isEmpty()) return emptyList()
        // 每刷新一次往下挪一屏，保证「刷新=换一批」，而不是重复同一批歌
        val start = if (pool.size <= limit) {
            0
        } else {
            ((windowOffset % pool.size) + pool.size) % pool.size
        }
        val raw = pool.drop(start).take(limit)
        if (raw.isEmpty()) return emptyList()
        val withCovers = CustomChannelCoverCache.ensureLocalCovers(app, raw)
        return listOf(
            HomeFeedGroup(
                key = "channel:${sourceId}:${section.name}",
                titleResId = section.titleResId,
                icon = homeSectionIcon(section),
                presentation = HomeFeedPresentation.SONG_LIST,
                sourceId = sourceId,
                songs = HomeSectionState(items = withCovers.map { it.toSongItem(app, sourceId) })
            )
        )
    }

    companion object {
        /** QQ 音乐榜单来源。 */
        fun tencent(): ChannelHomeFeedProvider =
            ChannelHomeFeedProvider(MusicSourceIds.TENCENT) { section ->
                when (section) {
                    HomeSectionId.TRENDING -> 26  // 热歌榜
                    HomeSectionId.RADAR -> 27     // 新歌榜
                    HomeSectionId.RECOMMENDED -> 4 // 巅峰榜·流行指数
                    HomeSectionId.CONTINUE -> 26
                }
            }
    }
}
