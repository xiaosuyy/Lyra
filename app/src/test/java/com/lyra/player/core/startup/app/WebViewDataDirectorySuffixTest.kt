package com.lyra.player.core.startup.app

import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewDataDirectorySuffixTest {
    @Test
    fun `uses process segment after colon`() {
        assertEquals(
            "bili.login",
            WebViewDataDirectorySuffix.forProcess("com.lyra.player:bili.login")
        )
    }

    @Test
    fun `uses full process name when no colon exists`() {
        assertEquals(
            "com.lyra.player",
            WebViewDataDirectorySuffix.forProcess("com.lyra.player")
        )
    }

    @Test
    fun `falls back when suffix is blank`() {
        assertEquals("webview", WebViewDataDirectorySuffix.forProcess("com.lyra.player:"))
    }

    @Test
    fun `normalizes unsafe suffix characters`() {
        assertEquals(
            "login_process_1",
            WebViewDataDirectorySuffix.forProcess("com.lyra.player:login process#1")
        )
    }
}
