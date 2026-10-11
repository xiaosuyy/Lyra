package moe.ouom.neriplayer.ui.viewmodel.tab.home

import moe.ouom.neriplayer.core.source.MusicSourceCatalog
import moe.ouom.neriplayer.core.source.MusicSourceIds

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
 * File: moe.ouom.neriplayer.ui.viewmodel.tab.home/HomeSourceCatalog
 * Created: 2026/9/28
 */

/** 首页来源选择项。 */
internal data class HomeSourceOption(
    val sourceId: String,
    val labelResId: Int
)

/**
 * 首页可选来源目录。
 *
 * 规则：
 * - 网易云首页由 HomeViewModel 内置提供（没有 Provider），始终是合法来源；
 * - 其余来源必须实现并注册 [HomeFeedProvider] 才会出现（表达首页能力）；
 * - 只保留当前「已启用 / 就绪」的音源；
 * - 按 [MusicSourceCatalog.sortOrderFor] 排序，新增来源无需改 UI。
 */
internal object HomeSourceCatalog {

    fun options(availableSourceIds: Set<String>): List<HomeSourceOption> {
        val ids = LinkedHashSet<String>()
        if (MusicSourceIds.NETEASE in availableSourceIds) {
            ids += MusicSourceIds.NETEASE
        }
        HomeFeedRegistry.all().forEach { provider ->
            if (provider.sourceId in availableSourceIds) {
                ids += provider.sourceId
            }
        }
        return ids
            .map { id -> HomeSourceOption(id, MusicSourceCatalog.nameResIdFor(id) ?: 0) }
            .sortedBy { MusicSourceCatalog.sortOrderFor(it.sourceId) }
    }
}

/**
 * 解析某分区实际生效的首页来源：配置的来源若当前不可用（未启用 / 未就绪），
 * 回落到内置网易云，避免继续展示已禁用的音源。
 */
internal fun resolveEffectiveHomeSourceId(
    configuredSourceId: String?,
    availableSourceIds: Set<String>
): String = configuredSourceId?.takeIf { it in availableSourceIds } ?: MusicSourceIds.NETEASE
