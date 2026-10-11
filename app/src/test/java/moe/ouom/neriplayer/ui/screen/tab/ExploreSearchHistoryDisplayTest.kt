package moe.ouom.neriplayer.ui.screen.tab

import moe.ouom.neriplayer.ui.viewmodel.tab.SearchSource
import moe.ouom.neriplayer.ui.viewmodel.tab.search.BilibiliSearchProvider
import moe.ouom.neriplayer.ui.viewmodel.tab.search.CustomChannelSearchProvider
import moe.ouom.neriplayer.ui.viewmodel.tab.search.NeteaseSearchProvider
import moe.ouom.neriplayer.ui.viewmodel.tab.search.SourceSearchRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExploreSearchHistoryDisplayTest {

    @Before
    fun registerProviders() {
        // 类型栏显隐由已注册 Provider 的能力决定
        SourceSearchRegistry.register(NeteaseSearchProvider)
        SourceSearchRegistry.register(BilibiliSearchProvider)
        CustomChannelSearchProvider.all.forEach(SourceSearchRegistry::register)
    }

    @Test
    fun `search history display keeps recent entries in stable order`() {
        val history = listOf("你好", "哈哈", "晴天")

        assertEquals(history, filteredExploreSearchHistory(history))
    }

    @Test
    fun `search history display caps visible entries at fifteen`() {
        val history = (1..20).map { "history$it" }

        assertEquals((1..15).map { "history$it" }, filteredExploreSearchHistory(history))
    }

    @Test
    fun `search history hides after content leaves the top`() {
        val history = listOf("你好")

        assertEquals(true, shouldShowExploreSearchHistory(history, contentScrolled = false))
        assertEquals(false, shouldShowExploreSearchHistory(history, contentScrolled = true))
        assertEquals(false, shouldShowExploreSearchHistory(emptyList(), contentScrolled = false))
    }

    @Test
    fun `netease search type bar hides after content leaves the top`() {
        assertTrue(
            shouldShowExploreNeteaseSearchTypeBar(
                selectedSearchSource = SearchSource.NETEASE,
                contentScrolled = false
            )
        )
        assertFalse(
            shouldShowExploreNeteaseSearchTypeBar(
                selectedSearchSource = SearchSource.NETEASE,
                contentScrolled = true
            )
        )
    }

    @Test
    fun `netease search type bar stays hidden for other sources`() {
        assertFalse(
            shouldShowExploreNeteaseSearchTypeBar(
                selectedSearchSource = SearchSource.BILIBILI,
                contentScrolled = false
            )
        )
        assertFalse(
            shouldShowExploreNeteaseSearchTypeBar(
                selectedSearchSource = SearchSource.BILIBILI,
                contentScrolled = true
            )
        )
    }

    @Test
    fun `channel search type bar follows the selected source and scroll state`() {
        assertTrue(
            shouldShowExploreYouTubeSearchTypeBar(
                selectedSearchSource = SearchSource.KUGOU,
                contentScrolled = false
            ).not()
        )
        assertTrue(
            exploreSearchTypeBarSource(
                selectedSearchSource = SearchSource.KUGOU,
                contentScrolled = false
            ) == SearchSource.KUGOU
        )
        assertFalse(
            exploreSearchTypeBarSource(
                selectedSearchSource = SearchSource.KUGOU,
                contentScrolled = true
            ) == SearchSource.KUGOU
        )
        assertFalse(
            exploreSearchTypeBarSource(
                selectedSearchSource = SearchSource.KUWO,
                contentScrolled = false
            ) == SearchSource.KUWO
        )
    }

    @Test
    fun `platform changes retain one search type bar target for an interruptible swap`() {
        val targets = listOf(
            SearchSource.NETEASE,
            SearchSource.KUGOU,
            SearchSource.NETEASE,
            SearchSource.KUGOU
        ).map { source ->
            exploreSearchTypeBarSource(
                selectedSearchSource = source,
                contentScrolled = false
            )
        }

        assertEquals(
            listOf(
                SearchSource.NETEASE,
                SearchSource.KUGOU,
                SearchSource.NETEASE,
                SearchSource.KUGOU
            ),
            targets
        )
        assertTrue(isExploreSearchTypeBarSourceSwap(targets[0], targets[1]))
        assertTrue(isExploreSearchTypeBarSourceSwap(targets[1], targets[2]))
        assertTrue(isExploreSearchTypeBarSourceSwap(targets[2], targets[3]))
    }

    @Test
    fun `search type bar only expands or collapses when a visible source changes`() {
        assertFalse(isExploreSearchTypeBarSourceSwap(SearchSource.NETEASE, null))
        assertFalse(isExploreSearchTypeBarSourceSwap(null, SearchSource.YOUTUBE_MUSIC))
        assertEquals(
            null,
            exploreSearchTypeBarSource(
                selectedSearchSource = SearchSource.BILIBILI,
                contentScrolled = false
            )
        )
        assertEquals(
            null,
            exploreSearchTypeBarSource(
                selectedSearchSource = SearchSource.NETEASE,
                contentScrolled = true
            )
        )
    }
}
