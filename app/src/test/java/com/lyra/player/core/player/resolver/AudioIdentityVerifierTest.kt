package com.lyra.player.core.player.resolver

import com.lyra.player.data.model.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioIdentityVerifierTest {

    @Test
    fun parseId3Text_readsTitleAndArtist() {
        val tags = parseId3Text(id3Tag(title = "管他的公路尽头", artist = "王源"))

        assertEquals("管他的公路尽头", tags?.first)
        assertEquals("王源", tags?.second)
    }

    @Test
    fun parseId3Text_readsUtf16Text() {
        val tags = parseId3Text(
            id3Tag(title = "晴天", artist = "周杰伦", encoding = 1)
        )

        assertEquals("晴天", tags?.first)
        assertEquals("周杰伦", tags?.second)
    }

    @Test
    fun parseId3Text_returnsNullForNonId3Audio() {
        assertNull(parseId3Text(ByteArray(1024) { 0x55 }))
    }

    @Test
    fun identityMatchesSameTitleAndArtist() {
        assertTrue(audioIdentityMatches(song(), "管他的公路尽头", "王源"))
    }

    @Test
    fun identityRejectsDifferentRecording() {
        assertFalse(audioIdentityMatches(song(), "唯一", "G.E.M. 邓紫棋"))
    }

    @Test
    fun identityRejectsSameTitleDifferentArtist() {
        assertFalse(audioIdentityMatches(song(), "管他的公路尽头", "其他歌手"))
    }

    @Test
    fun identityAcceptsMultiArtistOverlap() {
        assertTrue(
            audioIdentityMatches(song(), "管他的公路尽头", "王源 / 其他嘉宾")
        )
    }

    @Test
    fun identityPassesWhenTagsMissing() {
        assertTrue(audioIdentityMatches(song(), null, null))
    }

    @Test
    fun identityUsesOriginalMetadataWhenPresent() {
        val song = song().copy(originalName = "管他的公路尽头", originalArtist = "王源")
        assertTrue(audioIdentityMatches(song, "管他的公路尽头", "王源"))
    }

    private fun song(): SongItem = SongItem(
        id = -5242896564209991359L,
        name = "管他的公路尽头",
        artist = "王源",
        album = "管他的公路尽头",
        albumId = 0L,
        durationMs = 231_000L,
        coverUrl = null
    )

    private fun id3Tag(title: String, artist: String, encoding: Int = 3): ByteArray {
        val frames = id3Frame("TIT2", title, encoding) + id3Frame("TPE1", artist, encoding)
        val size = frames.size
        val syncSafe = byteArrayOf(
            ((size shr 21) and 0x7F).toByte(),
            ((size shr 14) and 0x7F).toByte(),
            ((size shr 7) and 0x7F).toByte(),
            (size and 0x7F).toByte()
        )
        return byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0) +
            syncSafe + frames
    }

    private fun id3Frame(id: String, text: String, encoding: Int): ByteArray {
        val charset = if (encoding == 1) Charsets.UTF_16 else Charsets.UTF_8
        val payload = byteArrayOf(encoding.toByte()) + text.toByteArray(charset)
        val size = payload.size
        return id.toByteArray(Charsets.ISO_8859_1) + byteArrayOf(
            ((size shr 24) and 0xFF).toByte(),
            ((size shr 16) and 0xFF).toByte(),
            ((size shr 8) and 0xFF).toByte(),
            (size and 0xFF).toByte(),
            0,
            0
        ) + payload
    }
}
