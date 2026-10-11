package moe.ouom.neriplayer.core.source

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
 * File: moe.ouom.neriplayer.core.source/MusicSource
 * Created: 2026/9/28
 */

/** 音源类型：应用内置平台，还是由 JS 脚本提供的自定义音源。 */
internal enum class MusicSourceKind {
    BUILTIN,
    CUSTOM
}

/** 音源当前状态。 */
internal enum class MusicSourceStatus {
    /** 可用。 */
    READY,

    /** 正在加载（例如脚本初始化中）。 */
    LOADING,

    /** 已被用户关闭。 */
    DISABLED,

    /** 当前不可用（例如没有脚本声明该音源，或加载失败）。 */
    UNAVAILABLE
}

/**
 * 音源能力：UI 只根据能力决定展示哪些入口/操作，不关心具体平台实现。
 */
internal data class MusicSourceCapabilities(
    /** 是否支持关键词搜索。 */
    val searchable: Boolean = false,
    /** 是否支持搜索歌曲。 */
    val supportsSong: Boolean = false,
    /** 是否支持搜索 / 浏览歌手。 */
    val supportsArtist: Boolean = false,
    /** 是否支持搜索 / 浏览专辑。 */
    val supportsAlbum: Boolean = false,
    /** 是否支持搜索 / 浏览歌单。 */
    val supportsPlaylist: Boolean = false,
    /** 是否能按歌单 id 拉取歌单内歌曲（打开渠道歌单详情）。 */
    val supportsPlaylistSongs: Boolean = false,
    /** 是否支持榜单 / 排行。 */
    val supportsRanking: Boolean = false
)

/**
 * 统一音源模型。
 *
 * 这是数据驱动 UI 的基础：搜索页、首页等只消费 [MusicSource] 列表，
 * 不需要再对具体平台写 `when(source)` 分支。
 */
internal data class MusicSource(
    /** 稳定标识，见 [MusicSourceIds]。 */
    val id: String,
    val kind: MusicSourceKind,
    val enabled: Boolean,
    val status: MusicSourceStatus,
    val capabilities: MusicSourceCapabilities,
    /** 内置 / 渠道音源的名称资源；自定义脚本音源可能为空。 */
    val nameResId: Int? = null,
    /** 脚本提供的名称（自定义音源）。 */
    val scriptName: String? = null,
    /** 该音源可用的音质（来自脚本声明，按优先级排列）。 */
    val supportedQualities: List<String> = emptyList(),
    /** 展示排序，越小越靠前。 */
    val sortOrder: Int = 0,
    /** 加载/解析错误信息。 */
    val error: String? = null
) {
    /** 是否可用（已启用且就绪）。 */
    val isReady: Boolean
        get() = enabled && status == MusicSourceStatus.READY

    /** 是否可作为搜索来源。 */
    val canSearch: Boolean
        get() = isReady && capabilities.searchable
}
