package com.lyra.player.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

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
 * File: com.lyra.player.ui.screen/LyricsUiStateTest
 * Created: 2026/9/28
 */

class LyricsUiStateTest {

    @Test
    fun lyricsPresentAlwaysShowsContent() {
        assertEquals(
            LyricsUiState.CONTENT,
            resolveLyricsUiState(lyricsCount = 12, loading = false, error = false)
        )
        // 即使同时标记 loading/error，只要有歌词就展示内容
        assertEquals(
            LyricsUiState.CONTENT,
            resolveLyricsUiState(lyricsCount = 1, loading = true, error = true)
        )
    }

    @Test
    fun noLyricsWhileLoadingShowsLoading() {
        assertEquals(
            LyricsUiState.LOADING,
            resolveLyricsUiState(lyricsCount = 0, loading = true, error = false)
        )
    }

    @Test
    fun noLyricsAndNoLoadingShowsEmpty() {
        assertEquals(
            LyricsUiState.EMPTY,
            resolveLyricsUiState(lyricsCount = 0, loading = false, error = false)
        )
    }

    @Test
    fun failureShowsErrorEvenIfStillLoading() {
        assertEquals(
            LyricsUiState.ERROR,
            resolveLyricsUiState(lyricsCount = 0, loading = true, error = true)
        )
        assertEquals(
            LyricsUiState.ERROR,
            resolveLyricsUiState(lyricsCount = 0, loading = false, error = true)
        )
    }
}
