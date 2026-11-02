package com.lyra.player.core.player.quality

import com.lyra.player.core.player.PlayerManager
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerManagerTrafficQualityExtensionsTest {

    @Test
    fun `effectiveYouTubeQuality falls back before player manager initialization`() {
        val previousQuality = PlayerManager.youtubePreferredQuality
        PlayerManager.youtubePreferredQuality = "very_high"
        try {
            assertEquals("very_high", PlayerManager.effectiveYouTubeQuality())
        } finally {
            PlayerManager.youtubePreferredQuality = previousQuality
        }
    }
}
