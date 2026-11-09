package com.lyra.player.ui.viewmodel.artist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TencentArtistFavoriteIdTest {
    @Test
    fun `favorite id is stable for the same singer mid`() {
        assertEquals(
            tencentArtistFavoriteId("0025NhlN2yWrP4"),
            tencentArtistFavoriteId("0025NhlN2yWrP4")
        )
    }

    @Test
    fun `favorite id is negative to stay clear of platform ids`() {
        assertTrue(tencentArtistFavoriteId("0025NhlN2yWrP4") < 0L)
        assertTrue(tencentArtistFavoriteId("") < 0L)
    }

    @Test
    fun `different singer mids produce different ids`() {
        assertNotEquals(
            tencentArtistFavoriteId("0025NhlN2yWrP4"),
            tencentArtistFavoriteId("001BLpXF2DyJe2")
        )
    }
}
