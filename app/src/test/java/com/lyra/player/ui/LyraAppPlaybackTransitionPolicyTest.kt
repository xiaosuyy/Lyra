package com.lyra.player.ui

import com.lyra.player.ui.component.playback.resolveMiniPlayerDisplayedCoverUrl
import com.lyra.player.ui.screen.resolveDisplayedNowPlayingCoverUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyraAppPlaybackTransitionPolicyTest {

    @Test
    fun `visual cover keeps previous image while new cover resolves`() {
        assertEquals(
            "old-cover",
            resolvePlaybackVisualCoverUrl(
                currentCoverUrl = null,
                previousVisualCoverUrl = "old-cover",
                hasCurrentSong = true,
                clearDelayElapsed = false
            )
        )
    }

    @Test
    fun `local playback keeps previous visual cover while its cover resolves`() {
        assertEquals(
            "remote-cover",
            resolvePlaybackVisualCoverUrl(
                currentCoverUrl = null,
                previousVisualCoverUrl = "remote-cover",
                hasCurrentSong = true,
                clearDelayElapsed = false
            )
        )
    }

    @Test
    fun `visual cover clears previous image when a new song cover is unresolved`() {
        assertNull(
            resolvePlaybackVisualCoverUrl(
                currentCoverUrl = null,
                previousVisualCoverUrl = "previous-song-cover",
                hasCurrentSong = true,
                clearDelayElapsed = false,
                preservePreviousVisualCover = false
            )
        )
    }

    @Test
    fun `visual cover clears after grace period or when playback stops`() {
        assertEquals(
            null,
            resolvePlaybackVisualCoverUrl(
                currentCoverUrl = null,
                previousVisualCoverUrl = "old-cover",
                hasCurrentSong = true,
                clearDelayElapsed = true
            )
        )
        assertEquals(
            null,
            resolvePlaybackVisualCoverUrl(
                currentCoverUrl = null,
                previousVisualCoverUrl = "old-cover",
                hasCurrentSong = false,
                clearDelayElapsed = false
            )
        )
    }

    @Test
    fun `visual cover prefers current non blank image`() {
        assertEquals(
            "new-cover",
            resolvePlaybackVisualCoverUrl(
                currentCoverUrl = "  new-cover  ",
                previousVisualCoverUrl = "old-cover",
                hasCurrentSong = true,
                clearDelayElapsed = false
            )
        )
    }

    @Test
    fun `cover image keeps the displayed cover until the new request succeeds`() {
        assertEquals(
            "old-cover",
            resolveDisplayedNowPlayingCoverUrl(
                requestedCoverUrl = "new-cover",
                displayedCoverUrl = "old-cover",
                requestSucceeded = false
            )
        )
        assertEquals(
            "new-cover",
            resolveDisplayedNowPlayingCoverUrl(
                requestedCoverUrl = "new-cover",
                displayedCoverUrl = "old-cover",
                requestSucceeded = true
            )
        )
    }

    @Test
    fun `cover image clears when the requested cover is absent`() {
        assertNull(
            resolveDisplayedNowPlayingCoverUrl(
                requestedCoverUrl = "  ",
                displayedCoverUrl = "old-cover",
                requestSucceeded = false
            )
        )
    }

    @Test
    fun `mini player keeps its cover while a newly resolved cover loads`() {
        assertEquals(
            "old-cover",
            resolveMiniPlayerDisplayedCoverUrl(
                requestedCoverUrl = "new-cover",
                displayedCoverUrl = "old-cover",
                requestSucceeded = false
            )
        )
        assertEquals(
            "new-cover",
            resolveMiniPlayerDisplayedCoverUrl(
                requestedCoverUrl = "new-cover",
                displayedCoverUrl = "old-cover",
                requestSucceeded = true
            )
        )
        assertEquals(
            "old-cover",
            resolveMiniPlayerDisplayedCoverUrl(
                requestedCoverUrl = null,
                displayedCoverUrl = "old-cover",
                requestSucceeded = false
            )
        )
        assertNull(
            resolveMiniPlayerDisplayedCoverUrl(
                requestedCoverUrl = null,
                displayedCoverUrl = "old-cover",
                requestSucceeded = false,
                clearDelayElapsed = true
            )
        )
    }

}
