package com.lyra.player.ui.screen.tab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LibraryScreenYouTubeGateTest {

    @Test
    fun `library tabs only show local and favorites`() {
        val tabs = libraryTabDisplayOrder()

        assertEquals(listOf(LibraryTab.LOCAL, LibraryTab.FAVORITE), tabs)
        // 其余渠道页签（含已移除的 YouTube）不再展示
        assertFalse(tabs.contains(LibraryTab.YTMUSIC))
        assertFalse(tabs.contains(LibraryTab.NETEASE))
        assertFalse(tabs.contains(LibraryTab.BILI))
    }
}
