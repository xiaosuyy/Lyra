package moe.ouom.neriplayer.ui.viewmodel.tab.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.core.player.PlayerManager.biliClient
import moe.ouom.neriplayer.core.source.MusicSourceCapabilities
import moe.ouom.neriplayer.core.source.MusicSourceIds
import moe.ouom.neriplayer.core.source.SourceSearchType
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
 * File: moe.ouom.neriplayer.ui.viewmodel.tab.search/BilibiliSearchProvider
 * Created: 2026/9/28
 */

/**
 * Bilibili 搜索 Provider（3B 第二个迁移的平台）。
 *
 * 等价于原 `ExploreViewModel.searchBilibili` / `fetchBilibiliSearchPage`：
 * 视频关键词搜索（分页）→ 映射为通用 [moe.ouom.neriplayer.data.model.SongItem] → 本地排序。
 * Explore 里的 Bilibili 只做视频搜索，收藏夹/合集来自链接识别，因此仅支持歌曲搜索。
 */
internal object BilibiliSearchProvider : SourceSearchProvider {

    override val sourceId: String = MusicSourceIds.BILIBILI

    override val capabilities: MusicSourceCapabilities = MusicSourceCapabilities(
        searchable = true,
        supportsSong = true
    )

    override fun supports(type: SourceSearchType): Boolean = type == SourceSearchType.SONG

    override suspend fun search(request: SourceSearchRequest): SourceSearchPage {
        val searchPage = withContext(Dispatchers.IO) {
            biliClient.searchVideos(keyword = request.keyword, page = request.page)
        }
        val songs = rankExploreSongSearchResults(
            query = request.matchQuery,
            songs = searchPage.items.map { it.toSongItem() }
        )
        return SourceSearchPage(
            songs = songs,
            items = songs.map { ExploreSearchResult.Song(it) },
            page = searchPage.page,
            hasMore = searchPage.page < searchPage.numPages && searchPage.items.isNotEmpty()
        )
    }
}
