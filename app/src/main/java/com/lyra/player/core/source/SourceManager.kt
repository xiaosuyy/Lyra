package com.lyra.player.core.source

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
 * File: com.lyra.player.core.source/SourceManager
 * Created: 2026/9/28
 */

/**
 * 音源管理器的运行时输入。
 *
 * 不包含任何 UI / 网络状态，方便纯函数单测；由 ViewModel 从设置与
 * [com.lyra.player.core.customsource.CustomSourceManager] 组装。
 */
internal data class SourceManagerState(
    val youtubeEnabled: Boolean,
    /** 已加载且启用的脚本所声明的音源脚本 id（kw / kg / mg / tx…）。 */
    val declaredCustomSourceIds: Set<String>,
    /** 各音源脚本 id 的可用音质。 */
    val qualitiesByCustomSourceId: Map<String, List<String>> = emptyMap(),
    /** 用户在音源管理里手动关闭的音源 id。 */
    val disabledSourceIds: Set<String> = emptySet(),
    /** 用户自定义排序（id 列表）；未列出的按默认 [MusicSource.sortOrder]。 */
    val orderOverride: List<String> = emptyList()
)

/**
 * 音源管理器：决定“当前有哪些音源、各自状态如何、能否用于某种搜索”。
 *
 * 职责边界：
 * - 本类：有哪些音源 + 状态 + 能力过滤 + 排序。
 * - `SourceSearchProvider`：某个音源**具体怎么搜**。
 * - ViewModel：搜索流程编排。
 * - UI：展示。
 */
internal object SourceManager {

    /** 当前全部音源（含不可用 / 已关闭的，供音源管理页展示）。 */
    fun sources(state: SourceManagerState): List<MusicSource> {
        val base = MusicSourceCatalog.allSources(
            youtubeEnabled = state.youtubeEnabled,
            declaredCustomSourceIds = state.declaredCustomSourceIds,
            qualitiesByCustomSourceId = state.qualitiesByCustomSourceId
        )
        val withDisabled = base.map { source ->
            if (source.id in state.disabledSourceIds) {
                source.copy(enabled = false, status = MusicSourceStatus.DISABLED)
            } else {
                source
            }
        }
        return applyUserOrder(withDisabled, state.orderOverride)
    }

    /** 可用于搜索的音源（已启用、就绪且具备搜索能力）。 */
    fun searchSources(state: SourceManagerState): List<MusicSource> =
        sources(state).filter { it.canSearch }

    /** 支持某种搜索类型的可用音源。 */
    fun sourcesSupporting(state: SourceManagerState, type: SourceSearchType): List<MusicSource> =
        sources(state).filter { it.isReady && it.capabilities.supports(type) }

    private fun applyUserOrder(
        sources: List<MusicSource>,
        orderOverride: List<String>
    ): List<MusicSource> {
        if (orderOverride.isEmpty()) {
            return sources.sortedBy { it.sortOrder }
        }
        val orderIndex = orderOverride.withIndex().associate { (index, id) -> id to index }
        return sources.sortedWith(
            compareBy(
                { orderIndex[it.id] ?: Int.MAX_VALUE },
                { it.sortOrder }
            )
        )
    }
}
