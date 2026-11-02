package com.lyra.player.ui.screen.tab

import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.core.source.SourceSearchType
import com.lyra.player.ui.viewmodel.tab.SearchSource
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * LyraPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 LyraPlayer developers
 * https://github.com/cwuom/Lyra
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.lyra.player.ui.screen.tab/ExploreSourceFilterTest
 * Created: 2026/9/28
 */

class ExploreSourceFilterTest {

    private val songOnly: (String, SourceSearchType) -> Boolean =
        { _, type -> type == SourceSearchType.SONG }

    private val baseSources = listOf(
        SearchSource.ALL,
        SearchSource.NETEASE,
        SearchSource.BILIBILI,
        SearchSource.LINK_RECOGNITION
    )

    @Test
    fun allAndLinkAreAlwaysKept() {
        val result = filterExploreSourcesForType(
            sources = listOf(SearchSource.ALL, SearchSource.LINK_RECOGNITION),
            type = SourceSearchType.ARTIST,
            supports = songOnly
        )
        assertEquals(listOf(SearchSource.ALL, SearchSource.LINK_RECOGNITION), result)
    }

    @Test
    fun switchingSongToArtistHidesSongOnlySources() {
        val songResult = filterExploreSourcesForType(
            sources = baseSources,
            type = SourceSearchType.SONG,
            supports = songOnly
        )
        val artistResult = filterExploreSourcesForType(
            sources = baseSources,
            type = SourceSearchType.ARTIST,
            supports = songOnly
        )

        // 搜「歌曲」：支持 song 的音源全部保留
        assertEquals(
            listOf(
                SearchSource.ALL,
                SearchSource.NETEASE,
                SearchSource.BILIBILI,
                SearchSource.LINK_RECOGNITION
            ),
            songResult
        )
        // 切到「歌手」：不支持 artist 的音源隐藏，全部/链接常驻
        assertEquals(
            listOf(SearchSource.ALL, SearchSource.LINK_RECOGNITION),
            artistResult
        )
    }

    @Test
    fun sourceOrderIsPreserved() {
        val ordered = listOf(
            SearchSource.ALL,
            SearchSource.KUWO,
            SearchSource.NETEASE,
            SearchSource.KUGOU,
            SearchSource.LINK_RECOGNITION
        )
        val result = filterExploreSourcesForType(
            sources = ordered,
            type = SourceSearchType.SONG,
            supports = { _, _ -> true }
        )
        assertEquals(ordered, result)
    }

    @Test
    fun unsupportedSourceIsRemovedButOthersStay() {
        val result = filterExploreSourcesForType(
            sources = baseSources,
            type = SourceSearchType.ARTIST,
            supports = { sourceId, type ->
                type == SourceSearchType.ARTIST && sourceId != MusicSourceIds.BILIBILI
            }
        )
        assertEquals(
            listOf(
                SearchSource.ALL,
                SearchSource.NETEASE,
                SearchSource.LINK_RECOGNITION
            ),
            result
        )
    }

    @Test
    fun emptyInputStaysEmpty() {
        assertEquals(
            emptyList<SearchSource>(),
            filterExploreSourcesForType(
                sources = emptyList(),
                type = SourceSearchType.SONG,
                supports = songOnly
            )
        )
    }
}
