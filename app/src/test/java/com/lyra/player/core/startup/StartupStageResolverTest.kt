package com.lyra.player.core.startup

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupStageResolverTest {
    @Test
    fun `uses loading stage while disclaimer state is unknown`() {
        assertEquals(StartupStage.Loading, StartupStageResolver.resolve(disclaimerAccepted = null))
    }

    @Test
    fun `requires disclaimer before main`() {
        assertEquals(StartupStage.Disclaimer, StartupStageResolver.resolve(disclaimerAccepted = false))
    }

    @Test
    fun `routes accepted users to main stage`() {
        assertEquals(StartupStage.Main, StartupStageResolver.resolve(disclaimerAccepted = true))
    }

    @Test
    fun `routes pending disclaimer acceptance without waiting for storage echo`() {
        assertEquals(
            StartupStage.Main,
            StartupStageResolver.resolve(
                disclaimerAccepted = false,
                pendingDisclaimerAccepted = true
            )
        )
        assertEquals(
            StartupStage.Disclaimer,
            StartupStageResolver.resolve(
                disclaimerAccepted = false,
                pendingDisclaimerAccepted = false
            )
        )
    }
}
