package moe.ouom.neriplayer.ui.screen.tab

import moe.ouom.neriplayer.core.source.MusicSourceIds
import moe.ouom.neriplayer.ui.viewmodel.tab.home.resolveEffectiveHomeSourceId
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
