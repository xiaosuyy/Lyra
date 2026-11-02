package com.lyra.player.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class LyraAppLayeringPolicyTest {
    @Test
    fun miniPlayerRemainsAboveMainAndNavigationScenes() {
        assertTrue(MAIN_TAB_LAYER_Z_INDEX < NAV_HOST_LAYER_Z_INDEX)
        assertTrue(NAV_HOST_LAYER_Z_INDEX < MINI_PLAYER_OVERLAY_Z_INDEX)
    }
}
