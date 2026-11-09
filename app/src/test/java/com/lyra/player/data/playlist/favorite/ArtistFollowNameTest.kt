package com.lyra.player.data.playlist.favorite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ArtistFollowNameTest {
    @Test
    fun `normalizes case and surrounding whitespace`() {
        assertEquals("许嵩", normalizeArtistFollowName(" 许嵩 "))
        assertEquals("taylorswift", normalizeArtistFollowName("Taylor Swift"))
    }

    @Test
    fun `normalizes full width spaces`() {
        assertEquals(
            normalizeArtistFollowName("Taylor Swift"),
            normalizeArtistFollowName("Taylor\u3000Swift")
        )
    }

    @Test
    fun `blank names normalize to empty key`() {
        assertEquals("", normalizeArtistFollowName(""))
        assertEquals("", normalizeArtistFollowName("  \u3000 "))
    }

    @Test
    fun `different artists keep different keys`() {
        assertNotEquals(
            normalizeArtistFollowName("许嵩"),
            normalizeArtistFollowName("许巍")
        )
    }
}
