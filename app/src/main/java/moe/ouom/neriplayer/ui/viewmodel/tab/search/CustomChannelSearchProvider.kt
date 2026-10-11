package moe.ouom.neriplayer.ui.viewmodel.tab.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.api.search.CustomChannelCoverCache
import moe.ouom.neriplayer.core.api.search.CustomChannelSearchApi
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.core.source.MusicSourceCapabilities
import moe.ouom.neriplayer.core.source.MusicSourceCatalog
import moe.ouom.neriplayer.core.source.MusicSourceIds
import moe.ouom.neriplayer.core.source.SourceSearchType
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.viewmodel.tab.ExploreSearchResult
import moe.ouom.neriplayer.ui.viewmodel.tab.rankExploreSongSearchResults
import moe.ouom.neriplayer.ui.viewmodel.tab.toSongItem

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
 * File: moe.ouom.neriplayer.ui.viewmodel.tab.search/CustomChannelSearchProvider
 * Created: 2026/9/28
 */

/**
 * 自定义渠道搜索 Provider（酷我 / 酷狗 / 咪咕 / QQ，3B 第四个迁移项）。
 *
 * 等价于原 `ExploreViewModel.searchCustomChannel`：歌曲搜索会先把渠道封面本地化，
 * 再映射为通用 SongItem 并本地排序；歌手 / 歌单按各平台实际能力提供。
 *
 * 播放直链仍由自定义音源脚本解析（见 CustomSourceManager），这里只负责搜索。
 */
internal class CustomChannelSearchProvider private constructor(
    override val sourceId: String
) : SourceSearchProvider {

    override val capabilities: MusicSourceCapabilities =
        MusicSourceCatalog.capabilitiesFor(sourceId) ?: MusicSourceCapabilities(searchable = true)

    override suspend fun search(request: SourceSearchRequest): SourceSearchPage {
        val api = CustomChannelSearchApi(AppContainer.sharedOkHttpClient)
        val app = AppContainer.applicationContext
        return when (request.type) {
            SourceSearchType.SONG -> {
                val songs = withContext(Dispatchers.IO) {
                    when (sourceId) {
                        MusicSourceIds.KUWO -> api.searchKuwo(request.keyword)
                        MusicSourceIds.KUGOU -> api.searchKugou(request.keyword)
                        MusicSourceIds.TENCENT -> api.searchTencent(request.keyword)
                        else -> api.searchMigu(request.keyword)
                    }
                }
                // 渠道封面用应用自己的网络栈先下载到本地，再交给图片组件
                val withCovers = CustomChannelCoverCache.ensureLocalCovers(app, songs)
                val items = withCovers.map { it.toSongItem(app, sourceId) }
                val ranked = rankExploreSongSearchResults(request.matchQuery, items)
                SourceSearchPage(
                    songs = ranked,
                    items = ranked.map { ExploreSearchResult.Song(it) }
                )
            }

            SourceSearchType.ARTIST -> {
                val artists = withContext(Dispatchers.IO) {
                    when (sourceId) {
                        MusicSourceIds.KUGOU -> api.searchKugouArtists(request.keyword)
                        MusicSourceIds.TENCENT -> api.searchTencentArtists(request.keyword)
                        MusicSourceIds.MIGU -> api.searchMiguArtists(request.keyword)
                        else -> emptyList()
                    }
                }
                SourceSearchPage(items = artists.map { ExploreSearchResult.ChannelArtist(it) })
            }

            SourceSearchType.PLAYLIST -> {
                if (sourceId == MusicSourceIds.TENCENT) {
                    // QQ 匿名接口没有歌单关键词搜索，不拿热门歌单充数，直接说明情况
                    SourceSearchPage(
                        items = listOf(
                            ExploreSearchResult.Notice(
                                title = app.getString(R.string.explore_search_type_playlist),
                                message = app.getString(R.string.explore_channel_playlist_unsupported)
                            )
                        )
                    )
                } else {
                    val playlists = withContext(Dispatchers.IO) {
                        when (sourceId) {
                            MusicSourceIds.KUGOU -> api.searchKugouPlaylists(request.keyword)
                            MusicSourceIds.MIGU -> api.searchMiguPlaylists(request.keyword)
                            else -> emptyList()
                        }
                    }
                    SourceSearchPage(
                        items = playlists.map { ExploreSearchResult.ChannelPlaylist(it) }
                    )
                }
            }

            else -> SourceSearchPage()
        }
    }

    override suspend fun fetchPlaylistSongs(playlistId: String): List<SongItem> {
        if (!capabilities.supportsPlaylistSongs || playlistId.isBlank()) return emptyList()
        val api = CustomChannelSearchApi(AppContainer.sharedOkHttpClient)
        val app = AppContainer.applicationContext
        val songs = withContext(Dispatchers.IO) {
            when (sourceId) {
                MusicSourceIds.KUGOU -> api.fetchKugouPlaylistSongs(playlistId)
                MusicSourceIds.TENCENT -> api.fetchTencentPlaylistSongs(playlistId)
                else -> emptyList()
            }
        }
        NPLogger.i(
            "CustomChannelSearch",
            "fetchPlaylistSongs source=$sourceId id=$playlistId count=${songs.size}"
        )
        if (songs.isEmpty()) return emptyList()
        val withCovers = CustomChannelCoverCache.ensureLocalCovers(app, songs)
        return withCovers.map { it.toSongItem(app, sourceId) }
    }

    companion object {
        /** 应用已知的渠道音源 Provider 列表。 */
        val all: List<CustomChannelSearchProvider> = listOf(
            CustomChannelSearchProvider(MusicSourceIds.KUWO),
            CustomChannelSearchProvider(MusicSourceIds.KUGOU),
            CustomChannelSearchProvider(MusicSourceIds.MIGU),
            CustomChannelSearchProvider(MusicSourceIds.TENCENT)
        )
    }
}
