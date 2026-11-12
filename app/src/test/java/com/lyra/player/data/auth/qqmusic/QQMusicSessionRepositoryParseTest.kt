package com.lyra.player.data.auth.qqmusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QQMusicSessionRepositoryParseTest {

    @Test
    fun `parses QQ login cookie`() {
        val session = QQMusicSessionRepository.parse(
            "uin=o123456789; qm_keyst=abc123; qqmusic_key=legacy; login_type=1"
        )
        assertEquals("123456789", session.uin)
        assertEquals("abc123", session.musicKey)
        assertTrue(session.isLoggedIn)
    }

    @Test
    fun `parses WeChat login cookie`() {
        val session = QQMusicSessionRepository.parse(
            "wxuin=987654321; qm_keyst=key456; login_type=2"
        )
        assertEquals("987654321", session.uin)
        assertEquals("key456", session.musicKey)
        assertTrue(session.isLoggedIn)
    }

    @Test
    fun `falls back to legacy music key`() {
        val session = QQMusicSessionRepository.parse(
            "qqmusic_uin=555666777; qqmusic_key=oldkey"
        )
        assertEquals("555666777", session.uin)
        assertEquals("oldkey", session.musicKey)
    }

    @Test
    fun `rejects cookie without music key`() {
        val session = QQMusicSessionRepository.parse("uin=o123; skey=foo")
        assertFalse(session.isLoggedIn)
    }

    @Test
    fun `rejects empty cookie`() {
        assertFalse(QQMusicSessionRepository.parse("").isLoggedIn)
    }
}
