package moe.ouom.neriplayer.ui.viewmodel.tab.home

import moe.ouom.neriplayer.data.settings.HomeSectionId
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeSectionState

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
 * File: moe.ouom.neriplayer.ui.viewmodel.tab.home/HomeFeedGroupBuilders
 * Created: 2026/9/28
 */

/** 分区默认图标。 */
internal fun homeSectionIcon(section: HomeSectionId): HomeFeedIcon = when (section) {
    HomeSectionId.TRENDING -> HomeFeedIcon.BOLT
    HomeSectionId.RADAR -> HomeFeedIcon.RADAR
    HomeSectionId.RECOMMENDED -> HomeFeedIcon.STAR
    HomeSectionId.CONTINUE -> HomeFeedIcon.MUSIC
}

/** 某个分区「加载中」的统一分组。 */
internal fun homeLoadingFeedGroup(section: HomeSectionId, sourceId: String): HomeFeedGroup =
    HomeFeedGroup(
        key = "loading:${sourceId}:${section.name}",
        titleResId = section.titleResId,
        icon = homeSectionIcon(section),
        presentation = HomeFeedPresentation.SONG_LIST,
        sourceId = sourceId,
        songs = HomeSectionState(loading = true)
    )

/** 某个分区「加载失败」的统一分组。 */
internal fun homeErrorFeedGroup(
    section: HomeSectionId,
    sourceId: String,
    error: String?
): HomeFeedGroup = HomeFeedGroup(
    key = "error:${sourceId}:${section.name}",
    titleResId = section.titleResId,
    icon = homeSectionIcon(section),
    presentation = HomeFeedPresentation.SONG_LIST,
    sourceId = sourceId,
    songs = HomeSectionState(error = error)
)
