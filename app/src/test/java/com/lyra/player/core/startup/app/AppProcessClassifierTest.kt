package com.lyra.player.core.startup.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppProcessClassifierTest {
    @Test
    fun `matches configured main process`() {
        assertTrue(
            AppProcessClassifier.isMainProcess(
                currentProcessName = "com.lyra.player",
                configuredMainProcessName = "com.lyra.player",
                packageName = "com.lyra.player"
            )
        )
    }

    @Test
    fun `falls back to package name when configured process is blank`() {
        assertTrue(
            AppProcessClassifier.isMainProcess(
                currentProcessName = "com.lyra.player",
                configuredMainProcessName = "",
                packageName = "com.lyra.player"
            )
        )
    }

    @Test
    fun `detects secondary process`() {
        assertFalse(
            AppProcessClassifier.isMainProcess(
                currentProcessName = "com.lyra.player:web_login",
                configuredMainProcessName = "com.lyra.player",
                packageName = "com.lyra.player"
            )
        )
    }
}
