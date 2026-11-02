package moe.ouom.neriplayer.core.player.usb.device

import moe.ouom.neriplayer.core.player.model.PlaybackSoundConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbExclusiveCompatibilityPolicyTest {

    @Test
    fun `default sound configuration keeps native path eligible`() {
        assertFalse(PlaybackSoundConfig().requiresSystemAudioProcessor())
    }

    @Test
    fun `speed and pitch correction require system processor`() {
        assertTrue(PlaybackSoundConfig(speed = 1.05f).requiresSystemAudioProcessor())
        assertTrue(PlaybackSoundConfig(pitch = 0.95f).requiresSystemAudioProcessor())
    }

    @Test
    fun `platform audio effects require system audio session`() {
        assertTrue(PlaybackSoundConfig(equalizerEnabled = true).requiresSystemAudioProcessor())
        assertTrue(PlaybackSoundConfig(loudnessGainMb = 100).requiresSystemAudioProcessor())
    }

    @Test
    fun `stereo balance requires system audio processor`() {
        assertTrue(PlaybackSoundConfig(volumeBalance = -0.25f).requiresSystemAudioProcessor())
        assertTrue(PlaybackSoundConfig(volumeBalance = 0.25f).requiresSystemAudioProcessor())
    }

    @Test
    fun `volume normalization requires system audio processor`() {
        assertTrue(PlaybackSoundConfig(volumeNormalizationEnabled = true).requiresSystemAudioProcessor())
    }

    @Test
    fun `imperceptible playback parameter noise keeps native path eligible`() {
        assertFalse(PlaybackSoundConfig(speed = 1.00005f).requiresSystemAudioProcessor())
        assertFalse(PlaybackSoundConfig(pitch = 0.99995f).requiresSystemAudioProcessor())
    }

    @Test
    fun `playback parameter changes beyond epsilon require system processor`() {
        assertTrue(PlaybackSoundConfig(speed = 1.0002f).requiresSystemAudioProcessor())
        assertTrue(PlaybackSoundConfig(pitch = 0.9998f).requiresSystemAudioProcessor())
    }

    @Test
    fun `non positive loudness gain does not require platform effect`() {
        assertFalse(PlaybackSoundConfig(loudnessGainMb = 0).requiresSystemAudioProcessor())
        assertFalse(PlaybackSoundConfig(loudnessGainMb = -100).requiresSystemAudioProcessor())
    }
}
