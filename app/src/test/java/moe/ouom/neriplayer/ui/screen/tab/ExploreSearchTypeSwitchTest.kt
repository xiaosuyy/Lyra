package moe.ouom.neriplayer.ui.screen.tab

import moe.ouom.neriplayer.core.source.SourceSearchType
import moe.ouom.neriplayer.ui.viewmodel.tab.resolveAggregateAllSourcesFailed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
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
 * File: moe.ouom.neriplayer.ui.screen.tab/ExploreSearchTypeSwitchTest
 * Created: 2026/9/28
 */

class ExploreSearchTypeSwitchTest {

    // ---- 「歌曲」Section Header 显示规则 ----

    @Test
    fun aggregateWithMixedTypesShowsSongHeader() {
        assertTrue(
            shouldShowExploreSongSectionHeader(
                searchType = SourceSearchType.ARTIST,
                hasSongResults = true,
                hasOtherTypeResults = true
            )
        )
    }

    @Test
    fun songsOnlyDoesNotShowHeader() {
        assertFalse(
            shouldShowExploreSongSectionHeader(
                searchType = SourceSearchType.ARTIST,
                hasSongResults = true,
                hasOtherTypeResults = false
            )
        )
    }

    @Test
    fun explicitSongTypeNeverShowsHeader() {
        assertFalse(
            shouldShowExploreSongSectionHeader(
                searchType = SourceSearchType.SONG,
                hasSongResults = true,
                hasOtherTypeResults = true
            )
        )
    }

    @Test
    fun noSongsDoesNotShowHeader() {
        assertFalse(
            shouldShowExploreSongSectionHeader(
                searchType = SourceSearchType.ARTIST,
                hasSongResults = false,
                hasOtherTypeResults = true
            )
        )
    }

    // ---- Error 判定与聚合失败 ----

    @Test
    fun singleSourceFailureShowsError() {
        assertTrue(
            isExploreSearchError(hasQuery = true, searching = false, searchError = "boom", allSourcesFailed = false)
        )
    }

    @Test
    fun aggregateAllSourcesFailedShowsError() {
        assertTrue(
            isExploreSearchError(hasQuery = true, searching = false, searchError = null, allSourcesFailed = true)
        )
    }

    @Test
    fun loadingOrNoQueryNeverShowsError() {
        assertFalse(
            isExploreSearchError(hasQuery = true, searching = true, searchError = "boom", allSourcesFailed = true)
        )
        assertFalse(
            isExploreSearchError(hasQuery = false, searching = false, searchError = "boom", allSourcesFailed = true)
        )
    }

    @Test
    fun aggregateAllFailedFlagOnlyWhenEveryTargetFails() {
        assertTrue(
            resolveAggregateAllSourcesFailed(targetCount = 3, failureCount = 3, successCount = 0)
        )
    }

    @Test
    fun aggregatePartialFailureIsNotAllFailed() {
        assertFalse(
            resolveAggregateAllSourcesFailed(targetCount = 3, failureCount = 1, successCount = 2)
        )
    }

    @Test
    fun aggregateAllSucceededButEmptyIsNotAllFailed() {
        assertFalse(
            resolveAggregateAllSourcesFailed(targetCount = 3, failureCount = 0, successCount = 3)
        )
        assertFalse(
            resolveAggregateAllSourcesFailed(targetCount = 0, failureCount = 0, successCount = 0)
        )
    }

    // ---- 搜索结果 Empty 判定 ----

    @Test
    fun noQueryIsNotSearchEmpty() {
        assertFalse(
            isExploreSearchEmpty(hasQuery = false, searching = false, hasError = false, hasResults = false)
        )
    }

    @Test
    fun loadingIsNotSearchEmpty() {
        assertFalse(
            isExploreSearchEmpty(hasQuery = true, searching = true, hasError = false, hasResults = false)
        )
    }

    @Test
    fun errorIsNotSearchEmpty() {
        assertFalse(
            isExploreSearchEmpty(hasQuery = true, searching = false, hasError = true, hasResults = false)
        )
    }

    @Test
    fun resultsAreNotSearchEmpty() {
        assertFalse(
            isExploreSearchEmpty(hasQuery = true, searching = false, hasError = false, hasResults = true)
        )
    }

    @Test
    fun queryWithNoResultsShowsSearchEmpty() {
        assertTrue(
            isExploreSearchEmpty(hasQuery = true, searching = false, hasError = false, hasResults = false)
        )
    }

    // ---- 「歌手」Section Header 规则 ----

    @Test
    fun aggregateWithMixedTypesShowsArtistHeader() {
        assertTrue(
            shouldShowExploreArtistSectionHeader(
                searchType = SourceSearchType.SONG,
                hasArtistResults = true,
                hasOtherTypeResults = true
            )
        )
    }

    @Test
    fun artistOnlyDoesNotShowArtistHeader() {
        assertFalse(
            shouldShowExploreArtistSectionHeader(
                searchType = SourceSearchType.SONG,
                hasArtistResults = true,
                hasOtherTypeResults = false
            )
        )
    }

    @Test
    fun explicitArtistTypeNeverShowsArtistHeader() {
        assertFalse(
            shouldShowExploreArtistSectionHeader(
                searchType = SourceSearchType.ARTIST,
                hasArtistResults = true,
                hasOtherTypeResults = true
            )
        )
    }

    @Test
    fun noArtistsDoesNotShowArtistHeader() {
        assertFalse(
            shouldShowExploreArtistSectionHeader(
                searchType = SourceSearchType.SONG,
                hasArtistResults = false,
                hasOtherTypeResults = true
            )
        )
    }

    // ---- 切类型自动重搜 ----

    @Test
    fun previouslySearchedKeywordTriggersReSearchOnTypeChange() {
        assertEquals(
            "zhoujielun",
            resolveExploreReSearchKeyword(
                executedKeyword = "zhoujielun",
                previousType = SourceSearchType.SONG,
                newType = SourceSearchType.ARTIST
            )
        )
        assertEquals(
            "zhoujielun",
            resolveExploreReSearchKeyword(
                executedKeyword = "zhoujielun",
                previousType = SourceSearchType.SONG,
                newType = SourceSearchType.ALBUM
            )
        )
    }

    @Test
    fun blankKeywordNeverTriggersReSearch() {
        assertNull(
            resolveExploreReSearchKeyword(
                executedKeyword = "",
                previousType = SourceSearchType.SONG,
                newType = SourceSearchType.ARTIST
            )
        )
        assertNull(
            resolveExploreReSearchKeyword(
                executedKeyword = "   ",
                previousType = SourceSearchType.SONG,
                newType = SourceSearchType.ARTIST
            )
        )
    }

    @Test
    fun sameTypeDoesNotRepeatSearch() {
        assertNull(
            resolveExploreReSearchKeyword(
                executedKeyword = "zhoujielun",
                previousType = SourceSearchType.ARTIST,
                newType = SourceSearchType.ARTIST
            )
        )
    }

    @Test
    fun keywordIsTrimmedBeforeReSearch() {
        assertEquals(
            "jay",
            resolveExploreReSearchKeyword(
                executedKeyword = "  jay  ",
                previousType = SourceSearchType.SONG,
                newType = SourceSearchType.ARTIST
            )
        )
    }

    // ---- Tab 索引收敛（崩溃回归） ----

    @Test
    fun selectedTabIndexIsClampedWhenSourceListShrinks() {
        // 复现崩溃场景：列表从 8 项被过滤到 6 项，而当前页仍是 6
        assertEquals(5, resolveExploreSelectedTabIndex(currentPage = 6, tabCount = 6))
        assertEquals(7, resolveExploreSelectedTabIndex(currentPage = 7, tabCount = 8))
    }

    @Test
    fun selectedTabIndexHandlesEmptyAndNegative() {
        assertEquals(0, resolveExploreSelectedTabIndex(currentPage = 3, tabCount = 0))
        assertEquals(0, resolveExploreSelectedTabIndex(currentPage = -1, tabCount = 4))
        assertEquals(0, resolveExploreSelectedTabIndex(currentPage = 0, tabCount = 1))
    }
}
