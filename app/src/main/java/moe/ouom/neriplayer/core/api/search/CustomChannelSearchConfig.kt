package moe.ouom.neriplayer.core.api.search

import moe.ouom.neriplayer.BuildConfig

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
 * File: moe.ouom.neriplayer.core.api.search/CustomChannelSearchConfig
 * Created: 2026/9/28
 */

/**
 * 自定义渠道搜索（酷我 / 酷狗 / 咪咕 / QQ）的可调配置。
 *
 * 集中放置易失效或与客户端标识相关的配置，避免散落在各个 API 实现里。
 * 默认值保证开箱可用，可在构建时通过 Gradle 属性覆盖：
 *
 * ```
 * ./gradlew assembleRelease \
 *   -PcustomChannelTencentAggregateKey=xxxx \
 *   -PcustomChannelMobileUserAgent="Mozilla/5.0 ..."
 * ```
 */
internal object CustomChannelSearchConfig {

    /**
     * QQ 关键词兜底所用的第三方聚合接口（`api.yaohud.cn`）。
     *
     * 注意：这是**第三方公共服务，并非 QQ 音乐官方 API**，其 key 可能随服务方
     * 调整而失效，也随时可能停止服务。仅在官方 / 联想接口都无结果时才会走到这里，
     * 因此失效只会降级为“QQ 搜索不到结果”，不会影响其他平台。
     */
    val TENCENT_AGGREGATE_BASE_URL: String = "https://api.yaohud.cn/api/music/qq"

    /** 上述聚合接口的访问 key（构建时可覆盖，默认值仅为开箱可用）。 */
    val TENCENT_AGGREGATE_KEY: String = BuildConfig.CUSTOM_CHANNEL_TENCENT_AGGREGATE_KEY

    /**
     * 手机端 UA：使用通用浏览器标识，不携带应用名，避免形成固定的客户端指纹。
     */
    val MOBILE_USER_AGENT: String = BuildConfig.CUSTOM_CHANNEL_MOBILE_USER_AGENT

    /** 桌面端 UA：部分 QQ 官方接口需要桌面标识才能返回数据。 */
    const val DESKTOP_USER_AGENT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
}
