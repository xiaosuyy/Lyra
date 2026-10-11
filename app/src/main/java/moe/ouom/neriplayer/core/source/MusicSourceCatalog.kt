package moe.ouom.neriplayer.core.source

import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.customsource.CUSTOM_SOURCE_KUGOU
import moe.ouom.neriplayer.core.customsource.CUSTOM_SOURCE_KUWO
import moe.ouom.neriplayer.core.customsource.CUSTOM_SOURCE_MIGU
import moe.ouom.neriplayer.core.customsource.CUSTOM_SOURCE_QQ

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
 * File: moe.ouom.neriplayer.core.source/MusicSourceCatalog
 * Created: 2026/9/28
 */

/**
 * 音源目录：把“内置平台”和“依赖脚本的渠道平台”统一成 [MusicSource]。
 *
 * 这里只描述**静态能力**，不发起任何网络或脚本请求，因此可以纯函数式单测。
 * 运行时状态（脚本是否声明、可用音质、登录态）由调用方以参数注入。
 */
internal object MusicSourceCatalog {

    // ---------------- 内置平台能力 ----------------

    private val NETEASE_CAPS = MusicSourceCapabilities(
        searchable = true,
        supportsSong = true,
        supportsArtist = true,
        supportsAlbum = true,
        supportsPlaylist = true,
        supportsRanking = true
    )

    private val BILIBILI_CAPS = MusicSourceCapabilities(
        searchable = true,
        supportsSong = true,
        supportsArtist = true,
        supportsPlaylist = true
    )

    private val YOUTUBE_CAPS = MusicSourceCapabilities(
        searchable = true,
        supportsSong = true,
        supportsArtist = true,
        supportsAlbum = true,
        supportsPlaylist = true
    )

    // ---------------- 渠道平台能力（依赖音源脚本） ----------------

    private val KUWO_CAPS = MusicSourceCapabilities(
        searchable = true,
        supportsSong = true
    )

    private val KUGOU_CAPS = MusicSourceCapabilities(
        searchable = true,
        supportsSong = true,
        supportsArtist = true,
        supportsPlaylist = true,
        supportsPlaylistSongs = true
    )

    private val MIGU_CAPS = MusicSourceCapabilities(
        searchable = true,
        supportsSong = true,
        supportsArtist = true,
        supportsPlaylist = true
    )

    private val TENCENT_CAPS = MusicSourceCapabilities(
        searchable = true,
        supportsSong = true,
        supportsArtist = true,
        supportsPlaylistSongs = true,
        supportsRanking = true
    )

    /** 渠道音源的静态定义：UI 平台 id ↔ 音源脚本 id ↔ 名称 / 能力。 */
    private data class ChannelDef(
        val sourceId: String,
        val customSourceId: String,
        val nameResId: Int,
        val capabilities: MusicSourceCapabilities,
        val sortOrder: Int
    )

    private val CHANNEL_DEFS: List<ChannelDef> = listOf(
        ChannelDef(
            sourceId = MusicSourceIds.KUWO,
            customSourceId = CUSTOM_SOURCE_KUWO,
            nameResId = R.string.explore_tab_kuwo,
            capabilities = KUWO_CAPS,
            sortOrder = 100
        ),
        ChannelDef(
            sourceId = MusicSourceIds.KUGOU,
            customSourceId = CUSTOM_SOURCE_KUGOU,
            nameResId = R.string.explore_tab_kugou,
            capabilities = KUGOU_CAPS,
            sortOrder = 110
        ),
        ChannelDef(
            sourceId = MusicSourceIds.MIGU,
            customSourceId = CUSTOM_SOURCE_MIGU,
            nameResId = R.string.explore_tab_migu,
            capabilities = MIGU_CAPS,
            sortOrder = 120
        ),
        ChannelDef(
            sourceId = MusicSourceIds.TENCENT,
            customSourceId = CUSTOM_SOURCE_QQ,
            nameResId = R.string.explore_tab_tencent,
            capabilities = TENCENT_CAPS,
            sortOrder = 130
        )
    )

    /**
     * 内置平台音源（无需音源脚本即可搜索 / 播放）。
     *
     * @param youtubeEnabled 关闭 YouTube 后不再产出该音源。
     */
    fun builtinSources(youtubeEnabled: Boolean): List<MusicSource> = buildList {
        add(
            MusicSource(
                id = MusicSourceIds.NETEASE,
                kind = MusicSourceKind.BUILTIN,
                enabled = true,
                status = MusicSourceStatus.READY,
                capabilities = NETEASE_CAPS,
                nameResId = R.string.platform_netease_short,
                sortOrder = 0
            )
        )
        add(
            MusicSource(
                id = MusicSourceIds.BILIBILI,
                kind = MusicSourceKind.BUILTIN,
                enabled = true,
                status = MusicSourceStatus.READY,
                capabilities = BILIBILI_CAPS,
                nameResId = R.string.platform_bilibili,
                sortOrder = 50
            )
        )
        if (youtubeEnabled) {
            add(
                MusicSource(
                    id = MusicSourceIds.YOUTUBE_MUSIC,
                    kind = MusicSourceKind.BUILTIN,
                    enabled = true,
                    status = MusicSourceStatus.READY,
                    capabilities = YOUTUBE_CAPS,
                    nameResId = R.string.explore_tab_youtube,
                    sortOrder = 200
                )
            )
        }
    }

    /**
     * 依赖音源脚本取直链的渠道音源。
     *
     * @param declaredCustomSourceIds 已加载且启用的脚本所声明的音源脚本 id（kw / kg / mg / tx…）。
     * @param qualitiesByCustomSourceId 各音源脚本 id 的可用音质，来自 [moe.ouom.neriplayer.core.customsource.CustomSourceManager]。
     * @return 全部渠道音源；未声明的会被标记为 [MusicSourceStatus.UNAVAILABLE]。
     */
    fun channelSources(
        declaredCustomSourceIds: Set<String>,
        qualitiesByCustomSourceId: Map<String, List<String>> = emptyMap()
    ): List<MusicSource> = CHANNEL_DEFS.map { def ->
        val declared = def.customSourceId in declaredCustomSourceIds
        MusicSource(
            id = def.sourceId,
            kind = MusicSourceKind.CUSTOM,
            enabled = declared,
            status = if (declared) MusicSourceStatus.READY else MusicSourceStatus.UNAVAILABLE,
            capabilities = def.capabilities,
            nameResId = def.nameResId,
            supportedQualities = qualitiesByCustomSourceId[def.customSourceId].orEmpty(),
            sortOrder = def.sortOrder
        )
    }

    /**
     * 全部音源（内置 + 渠道），按 [MusicSource.sortOrder] 排序。
     */
    fun allSources(
        youtubeEnabled: Boolean,
        declaredCustomSourceIds: Set<String>,
        qualitiesByCustomSourceId: Map<String, List<String>> = emptyMap()
    ): List<MusicSource> = buildList {
        addAll(builtinSources(youtubeEnabled))
        addAll(channelSources(declaredCustomSourceIds, qualitiesByCustomSourceId))
    }.sortedBy { it.sortOrder }

    /** 渠道音源是否为应用已知（有搜索 / 浏览实现）的平台。 */
    fun isKnownChannelSource(customSourceId: String): Boolean =
        CHANNEL_DEFS.any { it.customSourceId == customSourceId }

    /** 静态定义的名称资源（内置 + 渠道），未知音源返回 null。 */
    fun nameResIdFor(sourceId: String): Int? =
        (builtinSources(youtubeEnabled = true) + channelSources(emptySet()))
            .firstOrNull { it.id == sourceId }
            ?.nameResId

    /** 静态定义的排序值（内置 + 渠道），未知音源返回较大值。 */
    fun sortOrderFor(sourceId: String): Int =
        (builtinSources(youtubeEnabled = true) + channelSources(emptySet()))
            .firstOrNull { it.id == sourceId }
            ?.sortOrder
            ?: 1000

    /**
     * 按音源 id 取静态能力（内置或渠道）；未知音源返回 null。
     *
     * 供 `SourceSearchProvider` 与 UI 复用同一份能力表，避免重复定义。
     */
    fun capabilitiesFor(sourceId: String): MusicSourceCapabilities? = when (sourceId) {
        MusicSourceIds.NETEASE -> NETEASE_CAPS
        MusicSourceIds.BILIBILI -> BILIBILI_CAPS
        MusicSourceIds.YOUTUBE_MUSIC -> YOUTUBE_CAPS
        MusicSourceIds.KUWO -> KUWO_CAPS
        MusicSourceIds.KUGOU -> KUGOU_CAPS
        MusicSourceIds.MIGU -> MIGU_CAPS
        MusicSourceIds.TENCENT -> TENCENT_CAPS
        else -> null
    }
}
