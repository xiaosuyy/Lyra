package com.lyra.player.ui.screen

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
 * File: com.lyra.player.ui.screen/LyricsUiState
 * Created: 2026/9/28
 */

/** 歌词页面展示状态（纯 UI 推导，不涉及歌词业务）。 */
internal enum class LyricsUiState {
    LOADING,
    EMPTY,
    ERROR,
    CONTENT
}

/**
 * 推导歌词展示状态：
 * - 有歌词 -> CONTENT（始终优先）
 * - 加载失败 -> ERROR
 * - 加载中 -> LOADING
 * - 其余 -> EMPTY
 */
internal fun resolveLyricsUiState(
    lyricsCount: Int,
    loading: Boolean,
    error: Boolean
): LyricsUiState = when {
    lyricsCount > 0 -> LyricsUiState.CONTENT
    error -> LyricsUiState.ERROR
    loading -> LyricsUiState.LOADING
    else -> LyricsUiState.EMPTY
}
