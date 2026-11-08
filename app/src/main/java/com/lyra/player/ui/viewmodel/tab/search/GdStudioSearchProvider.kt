package com.lyra.player.ui.viewmodel.tab.search

/*
 * LyraPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 LyraPlayer developers
 * https://github.com/cwuom/Lyra
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * File: com.lyra.player.ui.viewmodel.tab.search/GdStudioSearchProvider
 */

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lyra.player.R
import com.lyra.player.core.api.gdstudio.GdStudioApiClient
import com.lyra.player.core.api.gdstudio.GdStudioSong
import com.lyra.player.core.api.search.CustomChannelSearchApi
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.core.source.MusicSourceCapabilities
import com.lyra.player.core.source.MusicSourceCatalog
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.core.source.SourceSearchType
import com.lyra.player.data.model.SongItem
import com.lyra.player.ui.viewmodel.tab.ExploreSearchResult
import com.lyra.player.ui.viewmodel.tab.rankExploreSongSearchResults
import org.json.JSONObject

private const val TAG = "GdStudioSearch"

/**
 * GD 音乐台搜索 Provider。
 *
 * 目前官方仅稳定开放 netease / joox / bilibili 三个背后音源，
 * 这里统一使用 netease，保证搜索与取直链的可用性。
 */
internal object GdStudioSearchProvider : SourceSearchProvider {

    private const val GD_SOURCE = "netease"
    private const val PAGE_SIZE = 20

    override val sourceId: String = MusicSourceIds.GDSTUDIO

    override val capabilities: MusicSourceCapabilities =
        MusicSourceCatalog.capabilitiesFor(sourceId)
            ?: MusicSourceCapabilities(searchable = true, supportsSong = true)

    override suspend fun search(request: SourceSearchRequest): SourceSearchPage {
        if (request.type != SourceSearchType.SONG) return SourceSearchPage()
        val api = GdStudioApiClient(AppContainer.sharedOkHttpClient)
        val app = AppContainer.applicationContext
        val songs = withContext(Dispatchers.IO) {
            runCatching {
                api.search(
                    source = GD_SOURCE,
                    keyword = request.keyword,
                    count = PAGE_SIZE,
                    page = request.page
                )
            }.onFailure {
                LyraLogger.w(TAG, "GD 搜索失败: ${it.message}")
            }.getOrDefault(emptyList())
        }
        if (songs.isEmpty()) return SourceSearchPage(page = request.page)
        val metadata = resolveTrackMetadata(songs)
        val items = songs.map { it.toSongItem(app, metadata[it.id]) }
        val ranked = rankExploreSongSearchResults(request.matchQuery, items)
        return SourceSearchPage(
            songs = ranked,
            items = ranked.map { ExploreSearchResult.Song(it) },
            page = request.page
        )
    }

    /**
     * 搜索接口不返回时长和封面，只给 pic_id。
     *
     * GD 的 netease 结果 id 与网易云歌曲 ID 一致，因此用网易云公开详情接口
     * 批量补全时长（毫秒）和专辑封面，一次搜索只多一个请求。
     */
    private suspend fun resolveTrackMetadata(
        songs: List<GdStudioSong>
    ): Map<String, GdStudioTrackMetadata> {
        val ids = songs
            .asSequence()
            .filter { it.gdSource.equals(GD_SOURCE, ignoreCase = true) }
            .mapNotNull { it.id.toLongOrNull() }
            .distinct()
            .toList()
        if (ids.isEmpty()) return emptyMap()
        return withContext(Dispatchers.IO) {
            runCatching {
                val raw = AppContainer.neteaseClient.getSongDetailAnonymous(ids)
                val array = JSONObject(raw).optJSONArray("songs")
                    ?: return@runCatching emptyMap()
                buildMap {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val id = item.optLong("id", 0L)
                        if (id <= 0L) continue
                        val coverUrl = item.optJSONObject("album")
                            ?.optString("picUrl")
                            ?.takeIf { it.startsWith("http") }
                        put(
                            id.toString(),
                            GdStudioTrackMetadata(
                                durationMs = item.optLong("duration", 0L),
                                coverUrl = coverUrl
                            )
                        )
                    }
                }
            }.onFailure {
                LyraLogger.w(TAG, "GD 时长/封面补全失败: ${it.message}")
            }.getOrDefault(emptyMap())
        }
    }
}

/** 通过网易云详情接口补全的曲目元数据。 */
internal data class GdStudioTrackMetadata(
    val durationMs: Long = 0L,
    val coverUrl: String? = null
)

/** GD 搜索结果到通用 SongItem 的转换器。 */
internal fun GdStudioSong.toSongItem(
    app: Application,
    metadata: GdStudioTrackMetadata? = null
): SongItem {
    val fallbackAlbum = app.getString(R.string.explore_tab_gdstudio)
    return SongItem(
        id = CustomChannelSearchApi.stableChannelSongId("${MusicSourceIds.GDSTUDIO}|$gdSource|$id"),
        name = name,
        artist = artist.ifBlank { fallbackAlbum },
        album = album.ifBlank { fallbackAlbum },
        albumId = 0L,
        durationMs = metadata?.durationMs ?: 0L,
        coverUrl = metadata?.coverUrl,
        channelId = MusicSourceIds.GDSTUDIO,
        audioId = id,
        // 播放时需要用 GD 背后的音源名称取直链。
        subAudioId = gdSource
    )
}
