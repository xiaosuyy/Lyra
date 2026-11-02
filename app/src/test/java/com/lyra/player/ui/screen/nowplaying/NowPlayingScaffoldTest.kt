package com.lyra.player.ui.screen.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
 * File: com.lyra.player.ui.screen.nowplaying/NowPlayingScaffoldTest
 * Created: 2026/9/28
 */

class NowPlayingScaffoldTest {

    @Test
    fun portraitCoverUsesSeventyFourPercentOfWidthOnTallScreen() {
        // 360 x 800：高度足够，宽度是限制项 -> 360 * 0.74
        val size = resolveNowPlayingCoverSizeDp(
            availableWidthDp = 360f,
            availableHeightDp = 800f,
            isLandscape = false
        )
        assertEquals(360f * 0.74f, size, 0.01f)
    }

    @Test
    fun portraitCoverIsLimitedByHeightOnShortScreen() {
        // 360 x 400：高度是限制项 -> 400 * 0.50
        val size = resolveNowPlayingCoverSizeDp(
            availableWidthDp = 360f,
            availableHeightDp = 400f,
            isLandscape = false
        )
        assertEquals(400f * 0.50f, size, 0.01f)
    }

    @Test
    fun landscapeCoverIsSmallerThanPortraitForSameBounds() {
        val portrait = resolveNowPlayingCoverSizeDp(800f, 1600f, isLandscape = false)
        val landscape = resolveNowPlayingCoverSizeDp(800f, 1600f, isLandscape = true)
        assertTrue("landscape=$landscape should be < portrait=$portrait", landscape < portrait)
    }

    @Test
    fun coverNeverExceedsAvailableBounds() {
        val size = resolveNowPlayingCoverSizeDp(
            availableWidthDp = 300f,
            availableHeightDp = 300f,
            isLandscape = false
        )
        assertTrue(size <= 300f)
        assertTrue(size > 0f)
    }

    @Test
    fun landscapeCoverUsesFortyTwoPercentWidthAndFiftyEightPercentHeight() {
        // 800 x 1600：高度足够 -> 宽度限制 800 * 0.42
        assertEquals(
            800f * 0.42f,
            resolveNowPlayingCoverSizeDp(800f, 1600f, isLandscape = true),
            0.01f
        )
        // 800 x 400：高度受限 -> 400 * 0.58
        assertEquals(
            400f * 0.58f,
            resolveNowPlayingCoverSizeDp(800f, 400f, isLandscape = true),
            0.01f
        )
    }

    @Test
    fun landscapeAndPortraitPoliciesDoNotShareFractions() {
        // 1000x1000：landscape 宽度受限=420；portrait 高度受限=500 -> 两者不会互相套用
        val landscape = resolveNowPlayingCoverSizeDp(1000f, 1000f, isLandscape = true)
        val portrait = resolveNowPlayingCoverSizeDp(1000f, 1000f, isLandscape = false)
        assertEquals(1000f * 0.42f, landscape, 0.01f)
        assertEquals(1000f * 0.50f, portrait, 0.01f)
    }

    @Test
    fun negativeBoundsResolveToZero() {
        assertEquals(
            0f,
            resolveNowPlayingCoverSizeDp(-100f, -100f, isLandscape = false),
            0.0001f
        )
    }
}
