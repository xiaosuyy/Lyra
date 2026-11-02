package com.lyra.player.core.source

import com.lyra.player.core.customsource.CUSTOM_SOURCE_KUWO
import com.lyra.player.core.customsource.CUSTOM_SOURCE_QQ
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceManagerTest {

    private fun state(
        declared: Set<String> = setOf(CUSTOM_SOURCE_KUWO, CUSTOM_SOURCE_QQ),
        disabled: Set<String> = emptySet(),
        order: List<String> = emptyList(),
        youtubeEnabled: Boolean = true
    ) = SourceManagerState(
        youtubeEnabled = youtubeEnabled,
        declaredCustomSourceIds = declared,
        disabledSourceIds = disabled,
        orderOverride = order
    )

    @Test
    fun `default order is by sortOrder`() {
        val ids = SourceManager.sources(state()).map { it.id }
        assertEquals(
            listOf(
                MusicSourceIds.NETEASE,
                MusicSourceIds.BILIBILI,
                MusicSourceIds.KUWO,
                MusicSourceIds.KUGOU,
                MusicSourceIds.MIGU,
                MusicSourceIds.TENCENT,
                MusicSourceIds.YOUTUBE_MUSIC
            ),
            ids
        )
    }

    @Test
    fun `disabled source is marked and excluded from search`() {
        val sources = SourceManager.sources(state(disabled = setOf(MusicSourceIds.NETEASE)))
        val netease = sources.first { it.id == MusicSourceIds.NETEASE }
        assertFalse(netease.enabled)
        assertEquals(MusicSourceStatus.DISABLED, netease.status)
        assertFalse(netease.canSearch)
        assertTrue(SourceManager.searchSources(state(disabled = setOf(MusicSourceIds.NETEASE))).none { it.id == MusicSourceIds.NETEASE })
    }

    @Test
    fun `user order override is applied`() {
        val ids = SourceManager.sources(
            state(order = listOf(MusicSourceIds.YOUTUBE_MUSIC, MusicSourceIds.NETEASE))
        ).map { it.id }
        assertEquals(MusicSourceIds.YOUTUBE_MUSIC, ids.first())
        assertEquals(MusicSourceIds.NETEASE, ids[1])
    }

    @Test
    fun `unavailable channel is excluded and available one included`() {
        val searchIds = SourceManager.searchSources(state()).map { it.id }
        assertTrue(searchIds.contains(MusicSourceIds.KUWO))
        assertTrue(searchIds.contains(MusicSourceIds.TENCENT))
        assertFalse(searchIds.contains(MusicSourceIds.KUGOU)) // 未声明脚本
        assertFalse(searchIds.contains(MusicSourceIds.MIGU))
    }

    @Test
    fun `capability filter by search type`() {
        val playlist = SourceManager.sourcesSupporting(state(), SourceSearchType.PLAYLIST).map { it.id }
        assertTrue(playlist.contains(MusicSourceIds.NETEASE))
        assertFalse(playlist.contains(MusicSourceIds.KUWO))   // 不支持歌单
        assertFalse(playlist.contains(MusicSourceIds.TENCENT)) // 不支持歌单

        val ranking = SourceManager.sourcesSupporting(state(), SourceSearchType.RANKING).map { it.id }
        assertTrue(ranking.contains(MusicSourceIds.NETEASE))
        assertTrue(ranking.contains(MusicSourceIds.TENCENT))
    }

    @Test
    fun `youtube disabled removes source`() {
        val ids = SourceManager.sources(state(youtubeEnabled = false)).map { it.id }
        assertFalse(ids.contains(MusicSourceIds.YOUTUBE_MUSIC))
    }
}
