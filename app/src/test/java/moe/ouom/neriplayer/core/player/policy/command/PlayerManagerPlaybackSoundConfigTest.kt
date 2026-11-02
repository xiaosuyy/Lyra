package moe.ouom.neriplayer.core.player.policy.command

import moe.ouom.neriplayer.core.player.model.PlaybackEqualizerPresetId
import moe.ouom.neriplayer.core.player.model.PlaybackSoundConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerManagerPlaybackSoundConfigTest {

    @Test
    fun `usb exclusive disables processing for the active engine only`() {
        val effectiveConfig = resolvePlaybackSoundConfigForEngine(
            baseConfig = PlaybackSoundConfig(
                speed = 1.25f,
                pitch = 0.9f,
                loudnessGainMb = 600,
                volumeBalance = 0.4f,
                volumeNormalizationEnabled = true,
                equalizerEnabled = true,
                presetId = PlaybackEqualizerPresetId.CUSTOM,
                customBandLevelsMb = listOf(100, -100)
            ),
            usbExclusivePlaybackEnabled = true
        )

        assertEquals(1.0f, effectiveConfig.speed, 0.0001f)
        assertEquals(1.0f, effectiveConfig.pitch, 0.0001f)
        assertEquals(0, effectiveConfig.loudnessGainMb)
        assertEquals(0f, effectiveConfig.volumeBalance, 0.0001f)
        assertEquals(false, effectiveConfig.volumeNormalizationEnabled)
        assertEquals(false, effectiveConfig.equalizerEnabled)
        assertEquals(PlaybackEqualizerPresetId.CUSTOM, effectiveConfig.presetId)
        assertEquals(listOf(100, -100), effectiveConfig.customBandLevelsMb)
    }
}
