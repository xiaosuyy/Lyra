package com.lyra.player.ui.screen.tab

import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.ui.viewmodel.tab.home.resolveEffectiveHomeSourceId
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSourceResolutionTest {

    @Test
    fun `available configured source is kept`() {
        assertEquals(
            MusicSourceIds.TENCENT,
            resolveEffectiveHomeSourceId(
                configuredSourceId = MusicSourceIds.TENCENT,
                availableSourceIds = setOf(MusicSourceIds.NETEASE, MusicSourceIds.TENCENT)
            )
        )
    }

    @Test
    fun `disabled configured source falls back to netease`() {
        assertEquals(
            MusicSourceIds.NETEASE,
            resolveEffectiveHomeSourceId(
                configuredSourceId = MusicSourceIds.TENCENT,
                availableSourceIds = setOf(MusicSourceIds.NETEASE)
            )
        )
    }

    @Test
    fun `missing configured source falls back to netease`() {
        assertEquals(
            MusicSourceIds.NETEASE,
            resolveEffectiveHomeSourceId(
                configuredSourceId = null,
                availableSourceIds = setOf(MusicSourceIds.NETEASE)
            )
        )
    }
}
