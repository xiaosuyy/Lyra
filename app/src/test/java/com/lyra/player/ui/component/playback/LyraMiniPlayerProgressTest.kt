package com.lyra.player.ui.component.playback

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
 * File: com.lyra.player.ui.component.playback/LyraMiniPlayerProgressTest
 * Created: 2026/9/28
 */

class LyraMiniPlayerProgressTest {

    @Test
    fun progressReflectsPlaybackRatio() {
        assertEquals(0f, resolveMiniPlayerProgressFraction(0L, 100_000L), 0.0001f)
        assertEquals(0.25f, resolveMiniPlayerProgressFraction(25_000L, 100_000L), 0.0001f)
        assertEquals(0.5f, resolveMiniPlayerProgressFraction(50_000L, 100_000L), 0.0001f)
        assertEquals(1f, resolveMiniPlayerProgressFraction(100_000L, 100_000L), 0.0001f)
    }

    @Test
    fun progressIsClampedWhenPositionExceedsDuration() {
        assertEquals(1f, resolveMiniPlayerProgressFraction(120_000L, 100_000L), 0.0001f)
    }

    @Test
    fun progressIsZeroWhenDurationIsUnknown() {
        assertEquals(0f, resolveMiniPlayerProgressFraction(10_000L, 0L), 0.0001f)
        assertEquals(0f, resolveMiniPlayerProgressFraction(10_000L, -1L), 0.0001f)
    }

    @Test
    fun progressIsZeroWhenPositionIsNegative() {
        assertEquals(0f, resolveMiniPlayerProgressFraction(-500L, 100_000L), 0.0001f)
    }
}
