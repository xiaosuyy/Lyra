package com.lyra.player.ui.viewmodel.tab.home

import java.util.concurrent.ConcurrentHashMap
import com.lyra.player.data.settings.HomeSectionId

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
 * File: com.lyra.player.ui.viewmodel.tab.home/HomeFeedProvider
 * Created: 2026/9/28
 */

/**
 * 首页内容能力抽象。
 *
 * Provider 负责把「平台数据」转换成统一 [HomeFeedGroup]；Home UI 只消费 Group，
 * 不关心具体平台。只有实现了本接口的音源才被视为「提供首页内容」。
 */
internal interface HomeFeedProvider {

    /** 音源稳定标识，见 [com.lyra.player.core.source.MusicSourceIds]。 */
    val sourceId: String

    /** 该音源能提供的首页分区（首页能力）。 */
    val supportedSections: Set<HomeSectionId>

    fun supports(section: HomeSectionId): Boolean = section in supportedSections

    /**
     * 加载某分区的首页内容，并转换成统一 [HomeFeedGroup]。
     *
     * @param windowOffset 刷新时向后挪动的歌曲数量，用于「刷新=换一批」。
     */
    suspend fun loadSection(
        section: HomeSectionId,
        windowOffset: Int,
        limit: Int
    ): List<HomeFeedGroup>
}

/** `sourceId -> HomeFeedProvider` 注册表。 */
internal object HomeFeedRegistry {

    private val providers = ConcurrentHashMap<String, HomeFeedProvider>()

    fun register(provider: HomeFeedProvider) {
        providers[provider.sourceId] = provider
    }

    fun providerFor(sourceId: String?): HomeFeedProvider? =
        sourceId?.let { providers[it] }

    fun all(): List<HomeFeedProvider> = providers.values.toList()

    fun clear() {
        providers.clear()
    }
}
