package moe.ouom.neriplayer.ui.viewmodel.tab.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.core.logging.NPLogger
import moe.ouom.neriplayer.core.player.PlayerManager.neteaseClient
import moe.ouom.neriplayer.core.source.MusicSourceCapabilities
import moe.ouom.neriplayer.core.source.MusicSourceIds
import moe.ouom.neriplayer.core.source.SourceSearchType
import moe.ouom.neriplayer.ui.viewmodel.tab.ExploreSearchResult
import moe.ouom.neriplayer.ui.viewmodel.tab.NeteaseExploreSearchType
import moe.ouom.neriplayer.ui.viewmodel.tab.ParsedNeteaseSearchResult
import moe.ouom.neriplayer.ui.viewmodel.tab.hasMoreExploreSearchResults
import moe.ouom.neriplayer.ui.viewmodel.tab.parseNeteaseSearchResults
import moe.ouom.neriplayer.ui.viewmodel.tab.parseNeteaseSongDetailSongs
import moe.ouom.neriplayer.ui.viewmodel.tab.rankExploreSongSearchResults
import moe.ouom.neriplayer.ui.viewmodel.tab.searchSongItems

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
 * File: moe.ouom.neriplayer.ui.viewmodel.tab.search/NeteaseSearchProvider
 * Created: 2026/9/28
 */

/**
 * 网易云音乐搜索 Provider（3B 第一个迁移的平台）。
 *
 * 逻辑与原 `ExploreViewModel.fetchNeteaseSearchPage` 等价：
 * 登录态 weapi 搜索 → 空结果时走免登录公开接口 → 补全封面/时长 → 歌曲结果本地排序。
 */
internal object NeteaseSearchProvider : SourceSearchProvider {

    private const val TAG = "NeteaseSearchProvider"
    private const val PAGE_SIZE = 30

    override val sourceId: String = MusicSourceIds.NETEASE

    override val capabilities: MusicSourceCapabilities = MusicSourceCapabilities(
        searchable = true,
        supportsSong = true,
        supportsArtist = true,
        supportsPlaylist = true
    )

    override fun supports(type: SourceSearchType): Boolean = when (type) {
        SourceSearchType.SONG,
        SourceSearchType.ARTIST,
        SourceSearchType.PLAYLIST -> true
        else -> false
    }

    override suspend fun search(request: SourceSearchRequest): SourceSearchPage {
        val type = request.type.toNeteaseType() ?: return SourceSearchPage(page = request.page)
        val offset = (request.page - 1).coerceAtLeast(0) * PAGE_SIZE

        val raw = withContext(Dispatchers.IO) {
            neteaseClient.searchSongs(
                keyword = request.keyword,
                limit = PAGE_SIZE,
                offset = offset,
                type = type.apiType,
                usePersistedCookies = true
            )
        }
        var parsed = parseNeteaseSearchResults(raw, type)
        if (parsed.items.isEmpty()) {
            // 网易云未登录时 weapi 可能返回空结果，改用免登录的公开搜索接口
            val anonymousRaw = runCatching {
                withContext(Dispatchers.IO) {
                    neteaseClient.searchSongsAnonymous(
                        keyword = request.keyword,
                        limit = PAGE_SIZE,
                        offset = offset,
                        type = type.apiType
                    )
                }
            }.getOrNull()
            if (!anonymousRaw.isNullOrBlank()) {
                val anonymousParsed = parseNeteaseSearchResults(anonymousRaw, type)
                if (anonymousParsed.items.isNotEmpty()) {
                    NPLogger.i(
                        TAG,
                        "网易云免登录搜索命中: keyword=${request.keyword}, count=${anonymousParsed.items.size}"
                    )
                    parsed = hydrateAnonymousDetails(anonymousParsed)
                }
            }
        }

        val items = if (type == NeteaseExploreSearchType.SONG) {
            rankExploreSongSearchResults(
                request.matchQuery,
                parsed.items.mapNotNull { (it as? ExploreSearchResult.Song)?.song }
            ).map { ExploreSearchResult.Song(it) }
        } else {
            parsed.items
        }

        return SourceSearchPage(
            songs = searchSongItems(items),
            items = items,
            page = request.page,
            hasMore = hasMoreExploreSearchResults(
                totalCount = parsed.totalCount,
                loadedCount = offset + parsed.items.size,
                pageItemCount = parsed.items.size,
                pageSize = PAGE_SIZE
            )
        )
    }

    /**
     * 免登录搜索接口不返回专辑封面，这里用歌曲详情接口把封面和时长补上。
     */
    private suspend fun hydrateAnonymousDetails(
        result: ParsedNeteaseSearchResult
    ): ParsedNeteaseSearchResult {
        val songs = result.items.mapNotNull { (it as? ExploreSearchResult.Song)?.song }
        val missing = songs.filter { it.coverUrl.isNullOrBlank() || it.durationMs <= 0L }
        if (missing.isEmpty()) {
            return result
        }
        val detailRaw = runCatching {
            withContext(Dispatchers.IO) {
                neteaseClient.getSongDetailAnonymous(missing.map { it.id })
            }
        }.onFailure {
            NPLogger.w(TAG, "网易云免登录歌曲详情补全失败: ${it.message}")
        }.getOrNull()
        if (detailRaw.isNullOrBlank()) {
            return result
        }
        val details = parseNeteaseSongDetailSongs(detailRaw).associateBy { it.id }
        if (details.isEmpty()) {
            return result
        }
        NPLogger.i(TAG, "网易云免登录搜索结果已补全封面/时长: ${details.size} 首")
        return result.copy(
            items = result.items.map { item ->
                val song = (item as? ExploreSearchResult.Song)?.song
                val detail = song?.let { details[it.id] }
                if (song == null || detail == null) {
                    item
                } else {
                    ExploreSearchResult.Song(
                        song.copy(
                            album = song.album.takeIf { it.isNotBlank() } ?: detail.album,
                            albumId = song.albumId.takeIf { it > 0L } ?: detail.albumId,
                            durationMs = song.durationMs.takeIf { it > 0L } ?: detail.durationMs,
                            coverUrl = song.coverUrl ?: detail.coverUrl
                        )
                    )
                }
            }
        )
    }
}

/** 统一搜索类型 → 网易云搜索类型（不支持的返回 null）。 */
internal fun SourceSearchType.toNeteaseType(): NeteaseExploreSearchType? = when (this) {
    SourceSearchType.SONG -> NeteaseExploreSearchType.SONG
    SourceSearchType.ARTIST -> NeteaseExploreSearchType.ARTIST
    SourceSearchType.PLAYLIST -> NeteaseExploreSearchType.PLAYLIST
    SourceSearchType.ALBUM,
    SourceSearchType.RANKING -> null
}
