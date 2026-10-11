package moe.ouom.neriplayer.core.customsource

import moe.ouom.neriplayer.data.model.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 来源判定：channelId 优先，album 前缀仅用于兼容旧数据。
 */
class CustomSourceSongSupportTest {

    private fun song(
        channelId: String? = null,
        album: String = "专辑"
    ) = SongItem(
        id = 1L,
        name = "测试歌曲",
        artist = "测试歌手",
        album = album,
        albumId = 0L,
        durationMs = 0L,
        coverUrl = null,
        channelId = channelId
    )

    @Test
    fun `channelId maps to custom source id`() {
        assertEquals(CUSTOM_SOURCE_KUWO, customSourceIdForSong(song(channelId = "kuwo")))
        assertEquals(CUSTOM_SOURCE_KUGOU, customSourceIdForSong(song(channelId = "kugou")))
        assertEquals(CUSTOM_SOURCE_MIGU, customSourceIdForSong(song(channelId = "migu")))
        assertEquals(CUSTOM_SOURCE_QQ, customSourceIdForSong(song(channelId = "tencent")))
    }

    @Test
    fun `channelId is case and whitespace insensitive`() {
        assertEquals(CUSTOM_SOURCE_KUWO, customSourceIdForSong(song(channelId = " KuWo ")))
    }

    @Test
    fun `legacy album prefix is still supported`() {
        assertEquals(
            CUSTOM_SOURCE_KUWO,
            customSourceIdForSong(song(album = "Kuwo|旧专辑"))
        )
        assertEquals(
            CUSTOM_SOURCE_QQ,
            customSourceIdForSong(song(album = "Tencent|旧专辑"))
        )
    }

    @Test
    fun `channelId wins over legacy album prefix`() {
        assertEquals(
            CUSTOM_SOURCE_KUGOU,
            customSourceIdForSong(song(channelId = "kugou", album = "Kuwo|旧专辑"))
        )
    }

    @Test
    fun `non channel song has no custom source`() {
        assertNull(customSourceIdForSong(song(channelId = "netease", album = "普通专辑")))
        assertNull(customSourceIdForSong(song(channelId = null, album = "普通专辑")))
    }
}
