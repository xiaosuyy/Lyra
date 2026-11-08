package com.lyra.player.data.model

import com.google.gson.Gson
import com.lyra.player.core.api.search.MusicPlatform
import com.lyra.player.data.sync.model.SyncCausalToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 旧 release(混淆构建)把 Gson 字段名打成了 P/R/m/n 这类短名, 用户设备上的歌单/收藏 payload
 * 都是那种键名。这里固定住"旧键可读 + 新键写出真实字段名"的约定。
 */
class LegacySongPayloadCompatibilityTest {

    private val gson = Gson()

    private val legacyPayload = """
        {
          "m": 42,
          "n": "夜曲",
          "o": "周杰伦",
          "p": "十一月的萧邦",
          "q": 3050,
          "r": 228000,
          "s": "https://example.com/cover.jpg",
          "t": "content://media/42",
          "u": "[00:01.00]lyric",
          "v": "[00:01.00]translated",
          "w": "m",
          "x": "matched-42",
          "y": 500,
          "z": "https://example.com/custom.jpg",
          "A": "自定义标题",
          "B": "自定义歌手",
          "C": "原始标题",
          "D": "原始歌手",
          "E": "https://example.com/original.jpg",
          "F": "原始歌词",
          "G": "原始翻译",
          "H": "song.flac",
          "I": "/storage/emulated/0/Music/song.flac",
          "J": "channel-1",
          "K": "audio-1",
          "L": "sub-audio-1",
          "M": "playlist-context",
          "N": "stable-key",
          "O": "https://example.com/stream.m4a",
          "P": [{"m": 7, "n": "方文山"}],
          "Q": 1700000000000,
          "R": [{"m": "device-a", "n": 3}]
        }
    """.trimIndent()

    @Test
    fun `decodes payload written with obfuscated keys`() {
        val song = gson.fromJson(legacyPayload, SongItem::class.java)

        assertEquals(42L, song.id)
        assertEquals("夜曲", song.name)
        assertEquals("周杰伦", song.artist)
        assertEquals("十一月的萧邦", song.album)
        assertEquals(3050L, song.albumId)
        assertEquals(228000L, song.durationMs)
        assertEquals("https://example.com/cover.jpg", song.coverUrl)
        assertEquals("content://media/42", song.mediaUri)
        assertEquals("[00:01.00]lyric", song.matchedLyric)
        assertEquals("[00:01.00]translated", song.matchedTranslatedLyric)
        assertEquals(MusicPlatform.CLOUD_MUSIC, song.matchedLyricSource)
        assertEquals("matched-42", song.matchedSongId)
        assertEquals(500L, song.userLyricOffsetMs)
        assertEquals("https://example.com/custom.jpg", song.customCoverUrl)
        assertEquals("自定义标题", song.customName)
        assertEquals("自定义歌手", song.customArtist)
        assertEquals("原始标题", song.originalName)
        assertEquals("原始歌手", song.originalArtist)
        assertEquals("https://example.com/original.jpg", song.originalCoverUrl)
        assertEquals("原始歌词", song.originalLyric)
        assertEquals("原始翻译", song.originalTranslatedLyric)
        assertEquals("song.flac", song.localFileName)
        assertEquals("/storage/emulated/0/Music/song.flac", song.localFilePath)
        assertEquals("channel-1", song.channelId)
        assertEquals("audio-1", song.audioId)
        assertEquals("sub-audio-1", song.subAudioId)
        assertEquals("playlist-context", song.playlistContextId)
        assertEquals("stable-key", song.sourceStableKey)
        assertEquals("https://example.com/stream.m4a", song.streamUrl)
        assertEquals(listOf(NeteaseArtistSummary(7L, "方文山")), song.neteaseArtists)
        assertEquals(1700000000000L, song.addedAt)
        assertEquals(listOf(SyncCausalToken("device-a", 3L)), song.syncMembershipTokens)
    }

    @Test
    fun `writes payload with stable real field names`() {
        val song = SongItem(
            id = 1L,
            name = "n",
            artist = "a",
            album = "al",
            albumId = 2L,
            durationMs = 3L,
            coverUrl = null,
            neteaseArtists = listOf(NeteaseArtistSummary(9L, "artist")),
            syncMembershipTokens = listOf(SyncCausalToken("device-b", 5L))
        )
        val json = gson.toJson(song)

        assertTrue(json.contains("\"neteaseArtists\""))
        assertTrue(json.contains("\"syncMembershipTokens\""))
        assertTrue(json.contains("\"deviceId\""))
        assertTrue(json.contains("\"counter\""))
        assertEquals(song, gson.fromJson(json, SongItem::class.java))
    }
}
