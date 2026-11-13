package com.lyra.player.ui.viewmodel.tab.home

import kotlin.random.Random
import com.lyra.player.data.model.stableKey
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
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.lyra.player.ui.viewmodel.tab.home/HomeRandomization
 * Created: 2026/9/28
 */

/**
 * 「最近展示」记录：内存态、有限窗口、不持久化。
 *
 * 用于 Home 换批时优先排除近期出现过的 item，避免连续刷新看到几乎相同的内容。
 */
internal class RecentlyShownStore(
    private val windowSize: Int = HomeRandomizationDefaults.RecentlyShownWindow
) {
    private val order = LinkedHashSet<String>()

    /** 当前窗口内的 id 快照（用于一次换批时过滤）。 */
    fun snapshot(): Set<String> = order.toSet()

    /** 记录新展示的 id：重复出现会被移到最新位置，超出窗口按最旧淘汰。 */
    fun record(ids: Iterable<String>) {
        ids.forEach { id ->
            if (id.isNotBlank()) {
                order.remove(id)
                order.add(id)
            }
        }
        while (order.size > windowSize) {
            val iterator = order.iterator()
            if (!iterator.hasNext()) break
            iterator.next()
            iterator.remove()
        }
    }

    fun size(): Int = order.size
}

internal object HomeRandomizationDefaults {
    /** RecentlyShown 窗口大小。 */
    const val RecentlyShownWindow = 40

    /** 每个 Section 每次展示的批次大小。 */
    const val BatchSize = 3

    /**
     * 参与批次随机的分区：首页除「继续播放」外的全部内容分区
     * （热门/新歌、个人雷达、雷达歌单、推荐歌单等）。
     *
     * 只有 CONTINUE（最近播放）保持业务排序语义，不随机。
     */
    val RandomizedSections: Set<HomeSectionId> =
        HomeSectionId.entries.filter { it != HomeSectionId.CONTINUE }.toSet()
}

/** 一次 Home 换批的上下文：generation 变化即换批；recentIds 为该批开始时生效的近期展示快照。 */
internal data class HomeBatchContext(
    val generation: Int = 0,
    val recentIds: Set<String> = emptySet()
)

/**
 * 对合并后的 Home 分组应用批次随机（仅限 [HomeRandomizationDefaults.RandomizedSections]）。
 *
 * generation <= 0 时不做随机（首次进入前保持 Provider 原始顺序）。
 */
internal fun applyHomeRandomization(
    merged: Map<HomeSectionId, List<HomeFeedGroup>>,
    context: HomeBatchContext,
    baseSeed: Long,
    batchSize: Int = HomeRandomizationDefaults.BatchSize
): Map<HomeSectionId, List<HomeFeedGroup>> {
    if (context.generation <= 0) return merged
    return merged.mapValues { (section, groups) ->
        if (section !in HomeRandomizationDefaults.RandomizedSections) return@mapValues groups
        groups.map { group -> group.randomizedBatch(context, baseSeed, batchSize) }
    }
}

private fun HomeFeedGroup.randomizedBatch(
    context: HomeBatchContext,
    baseSeed: Long,
    batchSize: Int
): HomeFeedGroup {
    if (!batchRandomize) return this
    val random = Random(homeBatchSeed(baseSeed, context.generation, key))
    val randomizedSongs = if (songs.items.isNotEmpty()) {
        songs.copy(
            items = selectRandomizedBatch(
                items = songs.items,
                count = batchSize,
                idOf = { it.stableKey() },
                excludedIds = context.recentIds,
                random = random
            )
        )
    } else {
        songs
    }
    val randomizedPlaylists = if (playlists.items.isNotEmpty()) {
        playlists.copy(
            items = selectRandomizedBatch(
                items = playlists.items,
                count = batchSize,
                idOf = { it.id.toString() },
                excludedIds = context.recentIds,
                random = random
            )
        )
    } else {
        playlists
    }
    return copy(songs = randomizedSongs, playlists = randomizedPlaylists)
}

/**
 * 批次选择种子。
 *
 * 同一次 Home 会话（同一 generation）内必须稳定：只要 [groupKey] 与 [generation] 不变，
 * 结果可复现，因此 Compose 重组 / 其它上游状态变化不会导致内容变化。
 */
internal fun homeBatchSeed(
    baseSeed: Long,
    generation: Int,
    groupKey: String
): Long = baseSeed * 31L + generation.toLong() * 1_000_003L + groupKey.hashCode()

/**
 * 从候选池中随机选择一批内容（内容批次随机，而不只是顺序随机）。
 *
 * 规则：
 * 1. 批次内按 [idOf] 去重（绝不重复）
 * 2. 优先选择不在 [excludedIds]（近期展示）中的项
 * 3. 数量不足时，从近期展示项中补足（允许有限重复）
 * 4. 候选池不足时返回可用全部，不会死循环、不抛异常
 */
internal fun <T> selectRandomizedBatch(
    items: List<T>,
    count: Int,
    idOf: (T) -> String,
    excludedIds: Set<String>,
    random: Random
): List<T> {
    if (count <= 0 || items.isEmpty()) return emptyList()
    val deduped = items.distinctBy(idOf)
    val preferred = deduped.filter { idOf(it) !in excludedIds }.shuffled(random)
    val fallback = deduped.filter { idOf(it) in excludedIds }.shuffled(random)

    val picked = ArrayList<T>(minOf(count, deduped.size))
    for (item in preferred) {
        if (picked.size >= count) break
        picked.add(item)
    }
    if (picked.size < count) {
        for (item in fallback) {
            if (picked.size >= count) break
            picked.add(item)
        }
    }
    return picked
}
