package com.lyra.player.ui.viewmodel.tab.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lyra.player.R
import com.lyra.player.core.api.qqmusic.QQMusicHomeClient
import com.lyra.player.core.api.qqmusic.QQMusicHomePlaylist
import com.lyra.player.core.api.search.CustomChannelCoverCache
import com.lyra.player.core.api.search.CustomChannelSong
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.data.settings.HomeSectionId
import com.lyra.player.ui.viewmodel.tab.HomeSectionState
import com.lyra.player.ui.viewmodel.tab.PlaylistSummary
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
 * File: com.lyra.player.ui.viewmodel.tab.home/QQMusicHomeFeedProvider
 */

/**
 * QQ 音乐首页推荐内容 Provider。
 *
 * 推荐页结构对齐 QQ 音乐：新歌首发 / 雷达 / 推荐歌单；
 * 登录后雷达按账号口味生成，未登录时退回官方匿名推荐。
 */
internal class QQMusicHomeFeedProvider(
    private val client: QQMusicHomeClient = QQMusicHomeClient()
) : HomeFeedProvider {

    override val sourceId: String = MusicSourceIds.TENCENT

    override val supportedSections: Set<HomeSectionId> = setOf(
        HomeSectionId.TRENDING,
        HomeSectionId.RECOMMENDED
    )

    override suspend fun loadSection(
        section: HomeSectionId,
        windowOffset: Int,
        limit: Int
    ): List<HomeFeedGroup> {
        if (!supports(section) || limit <= 0) return emptyList()
        return when (section) {
            HomeSectionId.TRENDING -> loadTrending(windowOffset, limit)
            HomeSectionId.RECOMMENDED -> loadRecommended(windowOffset, limit)
            HomeSectionId.RADAR, HomeSectionId.CONTINUE -> emptyList()
        }
    }

    /** 每日 30 首（登录）+ 新歌首发。 */
    private suspend fun loadTrending(windowOffset: Int, limit: Int): List<HomeFeedGroup> {
        val groups = ArrayList<HomeFeedGroup>(2)
        loadDailySongs(windowOffset, limit)?.let(groups::add)
        loadNewSongs(windowOffset, limit)?.let(groups::add)
        return groups
    }

    /** 为你精选（登录）+ 排行榜 + 推荐歌单。 */
    private suspend fun loadRecommended(windowOffset: Int, limit: Int): List<HomeFeedGroup> {
        val groups = ArrayList<HomeFeedGroup>(3)
        loadForYouPlaylists(windowOffset, limit)?.let(groups::add)
        loadToplists(limit)?.let(groups::add)
        loadRecommendPlaylists(windowOffset, limit)?.let(groups::add)
        return groups
    }

    private suspend fun loadDailySongs(windowOffset: Int, limit: Int): HomeFeedGroup? {
        val session = AppContainer.qqMusicSessionRepo.getSessionOnce()
        if (!session.isLoggedIn) return null
        val songs = withContext(Dispatchers.IO) { client.getDailySongs(session, limit) }
        val rotated = rotate(songs, windowOffset).take(limit)
        if (rotated.isEmpty()) return null
        return HomeFeedGroup(
            key = "qq:$sourceId:${HomeSectionId.TRENDING.name}:DAILY",
            titleResId = R.string.home_qq_daily_songs,
            icon = HomeFeedIcon.STAR,
            presentation = HomeFeedPresentation.SONG_LIST,
            sourceId = sourceId,
            songs = HomeSectionState(items = rotated.toSongItems())
        )
    }

    private suspend fun loadNewSongs(windowOffset: Int, limit: Int): HomeFeedGroup? {
        val pool = withContext(Dispatchers.IO) { client.getNewSongs(limit = limit * 4) }
        val rotated = rotate(pool, windowOffset).take(limit)
        if (rotated.isEmpty()) return null
        return HomeFeedGroup(
            key = "qq:$sourceId:${HomeSectionId.TRENDING.name}",
            titleResId = R.string.home_qq_new_songs,
            icon = HomeFeedIcon.BOLT,
            presentation = HomeFeedPresentation.SONG_LIST,
            sourceId = sourceId,
            songs = HomeSectionState(items = rotated.toSongItems())
        )
    }

    private suspend fun loadForYouPlaylists(windowOffset: Int, limit: Int): HomeFeedGroup? {
        val session = AppContainer.qqMusicSessionRepo.getSessionOnce()
        if (!session.isLoggedIn) return null
        val playlists = withContext(Dispatchers.IO) { client.getForYouPlaylists(session, limit) }
        if (playlists.isEmpty()) return null
        return HomeFeedGroup(
            key = "qq:$sourceId:${HomeSectionId.RECOMMENDED.name}:FORYOU",
            titleResId = R.string.home_qq_for_you,
            icon = HomeFeedIcon.MUSIC,
            presentation = HomeFeedPresentation.PLAYLIST_GRID,
            sourceId = sourceId,
            playlists = HomeSectionState(items = playlists.map(::toPlaylistSummary))
        )
    }

    /** 排行榜卡片用负数 id 编码榜单 id，点击时再还原成榜单详情。 */
    private suspend fun loadToplists(limit: Int): HomeFeedGroup? {
        val toplists = withContext(Dispatchers.IO) { client.getToplists() }
        if (toplists.isEmpty()) return null
        return HomeFeedGroup(
            key = "qq:$sourceId:${HomeSectionId.RECOMMENDED.name}:TOPLIST",
            titleResId = R.string.home_qq_toplist,
            icon = HomeFeedIcon.EXPLORE,
            presentation = HomeFeedPresentation.PLAYLIST_GRID,
            sourceId = sourceId,
            playlists = HomeSectionState(
                items = toplists.map { toplist ->
                    PlaylistSummary(
                        id = -toplist.id,
                        name = toplist.name,
                        picUrl = toplist.coverUrl.orEmpty(),
                        playCount = toplist.listenCount,
                        trackCount = toplist.trackCount
                    )
                }.take(limit)
            )
        )
    }

    private suspend fun loadRecommendPlaylists(windowOffset: Int, limit: Int): HomeFeedGroup? {
        val from = (windowOffset / limit).let { step -> step % RECOMMEND_PLAYLIST_PAGE_CYCLE } * limit
        val playlists = withContext(Dispatchers.IO) {
            client.getRecommendPlaylists(limit = limit, from = from)
        }
        if (playlists.isEmpty()) return null
        return HomeFeedGroup(
            key = "qq:$sourceId:${HomeSectionId.RECOMMENDED.name}",
            titleResId = R.string.home_qq_recommend_playlists,
            icon = HomeFeedIcon.EXPLORE,
            presentation = HomeFeedPresentation.PLAYLIST_GRID,
            sourceId = sourceId,
            playlists = HomeSectionState(items = playlists.map(::toPlaylistSummary))
        )
    }

    private fun toPlaylistSummary(playlist: QQMusicHomePlaylist) = PlaylistSummary(
        id = playlist.id,
        name = playlist.name,
        picUrl = playlist.coverUrl.orEmpty(),
        playCount = playlist.playCount,
        trackCount = playlist.trackCount
    )

    private suspend fun List<CustomChannelSong>.toSongItems() =
        CustomChannelCoverCache.ensureLocalCovers(AppContainer.applicationContext, this)
            .map { it.toSongItem(AppContainer.applicationContext, sourceId) }

    private fun rotate(source: List<CustomChannelSong>, offset: Int): List<CustomChannelSong> {
        if (source.isEmpty() || offset <= 0) return source
        val start = ((offset % source.size) + source.size) % source.size
        return source.drop(start) + source.take(start)
    }

    private companion object {
        const val RECOMMEND_PLAYLIST_PAGE_CYCLE = 6
    }
}
