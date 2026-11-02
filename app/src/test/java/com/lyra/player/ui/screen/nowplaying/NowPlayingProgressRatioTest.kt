package com.lyra.player.ui.screen.nowplaying

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
 * File: com.lyra.player.ui.screen.nowplaying/NowPlayingProgressRatioTest
 * Created: 2026/9/28
 */

class NowPlayingProgressRatioTest {

    @Test
    fun normalPlaybackProducesLinearRatio() {
        assertEquals(0f, resolveNowPlayingProgressRatio(0L, 100_000L), 0.0001f)
        assertEquals(0.25f, resolveNowPlayingProgressRatio(25_000L, 100_000L), 0.0001f)
        assertEquals(0.5f, resolveNowPlayingProgressRatio(50_000L, 100_000L), 0.0001f)
        assertEquals(1f, resolveNowPlayingProgressRatio(100_000L, 100_000L), 0.0001f)
    }

    @Test
    fun durationUnknownOrZeroYieldsZero() {
        assertEquals(0f, resolveNowPlayingProgressRatio(12_345L, 0L), 0.0001f)
        assertEquals(0f, resolveNowPlayingProgressRatio(12_345L, -1L), 0.0001f)
    }

    @Test
    fun positionBoundariesAreClamped() {
        assertEquals(0f, resolveNowPlayingProgressRatio(-999L, 100_000L), 0.0001f)
        assertEquals(1f, resolveNowPlayingProgressRatio(250_000L, 100_000L), 0.0001f)
    }

    @Test
    fun switchingSongsResetsRatio() {
        // 切歌后 position 归零 -> 比例归零
        assertEquals(0f, resolveNowPlayingProgressRatio(0L, 200_000L), 0.0001f)
    }
}
