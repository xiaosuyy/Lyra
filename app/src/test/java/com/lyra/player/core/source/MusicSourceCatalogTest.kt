package com.lyra.player.core.source

import com.lyra.player.core.customsource.CUSTOM_SOURCE_KUGOU
import com.lyra.player.core.customsource.CUSTOM_SOURCE_KUWO
import com.lyra.player.core.customsource.CUSTOM_SOURCE_MIGU
import com.lyra.player.core.customsource.CUSTOM_SOURCE_QQ
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSourceCatalogTest {

    @Test
    fun `builtin sources respect youtube switch`() {
        val withoutYoutube = MusicSourceCatalog.builtinSources(youtubeEnabled = false)
        assertEquals(
            listOf(MusicSourceIds.NETEASE, MusicSourceIds.BILIBILI),
            withoutYoutube.map { it.id }
        )

        val withYoutube = MusicSourceCatalog.builtinSources(youtubeEnabled = true)
        assertTrue(withYoutube.any { it.id == MusicSourceIds.YOUTUBE_MUSIC })
    }

    @Test
    fun `channel sources are unavailable without declaring scripts`() {
        val channels = MusicSourceCatalog.channelSources(declaredCustomSourceIds = emptySet())
        assertEquals(4, channels.size)
        assertTrue(channels.all { !it.enabled && it.status == MusicSourceStatus.UNAVAILABLE })
    }

    @Test
    fun `declared channel becomes ready`() {
        val channels = MusicSourceCatalog.channelSources(declaredCustomSourceIds = setOf(CUSTOM_SOURCE_KUWO))
        val kuwo = channels.first { it.id == MusicSourceIds.KUWO }
        val qq = channels.first { it.id == MusicSourceIds.TENCENT }
        assertTrue(kuwo.isReady)
        assertFalse(qq.isReady)
    }

    @Test
    fun `channel capabilities match implementation`() {
        val channels = MusicSourceCatalog.channelSources(declaredCustomSourceIds = setOf(CUSTOM_SOURCE_QQ))
        val qq = channels.first { it.id == MusicSourceIds.TENCENT }
        assertTrue(qq.capabilities.supportsSong)
        assertTrue(qq.capabilities.supportsRanking)
        assertFalse(qq.capabilities.supportsPlaylist)

        val kuwo = channels.first { it.id == MusicSourceIds.KUWO }
        assertTrue(kuwo.capabilities.supportsSong)
        assertFalse(kuwo.capabilities.supportsArtist)
    }

    @Test
    fun `playlist songs capability matches implementation`() {
        val declared = setOf(CUSTOM_SOURCE_KUGOU, CUSTOM_SOURCE_MIGU)
        val channels = MusicSourceCatalog.channelSources(declaredCustomSourceIds = declared)

        val kugou = channels.first { it.id == MusicSourceIds.KUGOU }
        assertTrue(kugou.capabilities.supportsPlaylistSongs)

        val migu = channels.first { it.id == MusicSourceIds.MIGU }
        assertFalse(migu.capabilities.supportsPlaylistSongs)
    }

    @Test
    fun `qualities are passed through`() {
        val channels = MusicSourceCatalog.channelSources(
            declaredCustomSourceIds = setOf(CUSTOM_SOURCE_KUWO),
            qualitiesByCustomSourceId = mapOf(CUSTOM_SOURCE_KUWO to listOf("320k", "flac"))
        )
        val kuwo = channels.first { it.id == MusicSourceIds.KUWO }
        assertEquals(listOf("320k", "flac"), kuwo.supportedQualities)
    }

    @Test
    fun `all sources are sorted and merge builtin plus channel`() {
        val sources = MusicSourceCatalog.allSources(
            youtubeEnabled = true,
            declaredCustomSourceIds = setOf(CUSTOM_SOURCE_KUWO)
        )
        val orders = sources.map { it.sortOrder }
        assertEquals(orders.sorted(), orders)
        assertTrue(sources.first().id == MusicSourceIds.NETEASE)
        assertTrue(sources.any { it.id == MusicSourceIds.KUWO && it.isReady })
        assertTrue(sources.any { it.id == MusicSourceIds.YOUTUBE_MUSIC })
    }
}
