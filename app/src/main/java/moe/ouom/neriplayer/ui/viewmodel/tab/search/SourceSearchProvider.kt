package moe.ouom.neriplayer.ui.viewmodel.tab.search

import java.util.concurrent.ConcurrentHashMap
import moe.ouom.neriplayer.core.source.MusicSourceCapabilities
import moe.ouom.neriplayer.core.source.SourceSearchType
import moe.ouom.neriplayer.core.source.supports as capabilitiesSupport
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.viewmodel.tab.ExploreSearchResult

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
 * File: moe.ouom.neriplayer.ui.viewmodel.tab.search/SourceSearchProvider
 * Created: 2026/9/28
 */

/** 一次统一搜索请求。 */
internal data class SourceSearchRequest(
    /** 音源稳定标识，见 [moe.ouom.neriplayer.core.source.MusicSourceIds]。 */
    val sourceId: String,
    val keyword: String,
    /** 用于本地排序的匹配词（可能经过链接识别等处理）。 */
    val matchQuery: String,
    val type: SourceSearchType,
    val page: Int = 1
)

/** 统一搜索结果页。 */
internal data class SourceSearchPage(
    val songs: List<SongItem> = emptyList(),
    val items: List<ExploreSearchResult> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false
)

/**
 * 统一的“音源搜索能力”抽象。
 *
 * UI / ViewModel 只依赖本接口，不再关心“网易云怎么搜 / B站怎么搜”。
 * 每个平台（含自定义渠道）在 3B 阶段提供各自的实现，并注册到 [SourceSearchRegistry]。
 *
 * 本接口是迁移目标：现有的 `when(source)` 分支将逐个迁移到这里，迁移完成后删除。
 */
internal interface SourceSearchProvider {

    /** 该 Provider 负责的音源 id，见 [moe.ouom.neriplayer.core.source.MusicSourceIds]。 */
    val sourceId: String

    /** 该音源的搜索能力，应与 [moe.ouom.neriplayer.core.source.MusicSourceCatalog] 中的定义一致。 */
    val capabilities: MusicSourceCapabilities

    /** 是否支持某种搜索类型。默认复用能力表，个别平台如需额外限制可覆写。 */
    fun supports(type: SourceSearchType): Boolean = capabilities.capabilitiesSupport(type)

    /**
     * 执行搜索。实现方需自行处理分页与错误；协程取消应向上传播
     * （不要吞掉 [kotlinx.coroutines.CancellationException]）。
     */
    suspend fun search(request: SourceSearchRequest): SourceSearchPage

    /**
     * 按歌单 id 拉取歌单内歌曲（打开渠道歌单详情用）。
     *
     * 默认不支持：仅当 [capabilities] 的
     * [MusicSourceCapabilities.supportsPlaylistSongs] 为 true 时，由对应 Provider 覆写。
     */
    suspend fun fetchPlaylistSongs(playlistId: String): List<SongItem> = emptyList()
}

/**
 * Provider 注册表：`sourceId -> SourceSearchProvider`。
 *
 * 3B 阶段每个平台注册一个 Provider；新增支持搜索的音源只需注册 Provider + 增加
 * [moe.ouom.neriplayer.core.source.MusicSource]，无需修改搜索 UI。
 */
internal object SourceSearchRegistry {

    private val providers = ConcurrentHashMap<String, SourceSearchProvider>()

    fun register(provider: SourceSearchProvider) {
        providers[provider.sourceId] = provider
    }

    fun unregister(sourceId: String) {
        providers.remove(sourceId)
    }

    fun providerFor(sourceId: String): SourceSearchProvider? = providers[sourceId]

    fun all(): List<SourceSearchProvider> = providers.values.toList()

    fun clear() {
        providers.clear()
    }
}
