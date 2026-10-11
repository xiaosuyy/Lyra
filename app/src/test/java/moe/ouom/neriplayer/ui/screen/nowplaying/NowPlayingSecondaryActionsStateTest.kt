package moe.ouom.neriplayer.ui.screen.nowplaying

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.ui.screen.nowplaying/NowPlayingSecondaryActionsStateTest
 * Created: 2026/9/28
 */

class NowPlayingSecondaryActionsStateTest {

    @Test
    fun withoutLyricsTheLyricsEntryIsDisabled() {
        val state = resolveNowPlayingSecondaryActionState(
            lyricsCount = 0,
            showLyricsScreen = false,
            sleepTimerActive = false
        )
        assertFalse(state.lyricsEnabled)
        assertFalse(state.lyricsActive)
    }

    @Test
    fun withLyricsButNotOnLyricsPageItIsEnabledAndInactive() {
        val state = resolveNowPlayingSecondaryActionState(
            lyricsCount = 12,
            showLyricsScreen = false,
            sleepTimerActive = false
        )
        assertTrue(state.lyricsEnabled)
        assertFalse(state.lyricsActive)
    }

    @Test
    fun onLyricsPageTheLyricsEntryBecomesActive() {
        val state = resolveNowPlayingSecondaryActionState(
            lyricsCount = 12,
            showLyricsScreen = true,
            sleepTimerActive = false
        )
        assertTrue(state.lyricsEnabled)
        assertTrue(state.lyricsActive)
    }

    @Test
    fun showingLyricsPageWithoutLyricsStaysInactive() {
        val state = resolveNowPlayingSecondaryActionState(
            lyricsCount = 0,
            showLyricsScreen = true,
            sleepTimerActive = false
        )
        assertFalse(state.lyricsEnabled)
        assertFalse(state.lyricsActive)
    }

    @Test
    fun timerActiveReflectsSleepTimerState() {
        assertTrue(
            resolveNowPlayingSecondaryActionState(
                lyricsCount = 3,
                showLyricsScreen = false,
                sleepTimerActive = true
            ).timerActive
        )
        assertFalse(
            resolveNowPlayingSecondaryActionState(
                lyricsCount = 3,
                showLyricsScreen = false,
                sleepTimerActive = false
            ).timerActive
        )
    }
}
