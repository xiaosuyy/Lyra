package moe.ouom.neriplayer.ui.viewmodel.tab.library

import java.util.concurrent.ConcurrentHashMap

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
 * File: moe.ouom.neriplayer.ui.viewmodel.tab.library/LibrarySourceProvider
 * Created: 2026/9/28
 */

/**
 * 媒体库数据能力抽象。
 *
 * Provider 负责按来源取数并转换成统一 [LibrarySection] / [LibraryItem]；
 * Library UI 只消费统一模型，不关心具体平台。
 */
internal interface LibrarySourceProvider {

    /** 音源稳定标识，见 [moe.ouom.neriplayer.core.source.MusicSourceIds] / [LOCAL_SOURCE_ID]。 */
    val sourceId: String

    /** 该来源能提供的媒体库内容能力。 */
    val capabilities: LibraryCapabilities

    /** 当前来源的媒体库分区快照（同步，供 UI 组合）。 */
    fun sections(): List<LibrarySection>

    /** 加载该来源的全部媒体库分区（默认取快照）。 */
    suspend fun loadSections(): List<LibrarySection> = sections()

    /**
     * 触发该来源的刷新；不支持刷新的来源无需覆写（默认空实现）。
     */
    suspend fun refresh() {}
}

/** `sourceId -> LibrarySourceProvider` 注册表。 */
internal object LibrarySourceRegistry {

    private val providers = ConcurrentHashMap<String, LibrarySourceProvider>()

    fun register(provider: LibrarySourceProvider) {
        providers[provider.sourceId] = provider
    }

    fun providerFor(sourceId: String?): LibrarySourceProvider? =
        sourceId?.let { providers[it] }

    fun all(): List<LibrarySourceProvider> = providers.values.toList()

    fun clear() {
        providers.clear()
    }
}
