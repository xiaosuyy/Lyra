package moe.ouom.neriplayer.ui.screen.tab.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.testutil.assumeComposeHostAvailable
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibraryAlbumRef
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibraryItem
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibraryPlaylistRef
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibraryPresentation
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibrarySection
import moe.ouom.neriplayer.ui.viewmodel.tab.library.LibrarySectionKind
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryUnifiedContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun assumeDeviceUnlocked() {
        assumeComposeHostAvailable()
    }

    @Test
    fun playlistSectionRendersAndClickPassesSourceId() {
        var clicked: LibraryPlaylistRef? = null
        val section = LibrarySection(
            id = "tencent:playlists",
            sourceId = "tencent",
            titleResId = R.string.library_tab_qqmusic,
            kind = LibrarySectionKind.PLAYLIST,
            presentation = LibraryPresentation.PLAYLIST_LIST,
            items = listOf(
                LibraryItem.Playlist(
                    LibraryPlaylistRef(sourceId = "tencent", id = "7", name = "测试歌单", songCount = 3)
                )
            )
        )

        composeRule.setContent {
            MaterialTheme {
                LibraryUnifiedContent(
                    sections = listOf(section),
                    onPlaylistClick = { clicked = it },
                    onAlbumClick = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("测试歌单").assertIsDisplayed().performClick()
        assertEquals("tencent", clicked?.sourceId)
        assertEquals("7", clicked?.id)
    }

    @Test
    fun albumSectionRendersAndClickPassesSourceId() {
        var clicked: LibraryAlbumRef? = null
        val section = LibrarySection(
            id = "netease:albums",
            sourceId = "netease",
            titleResId = R.string.library_tab_netease_album,
            kind = LibrarySectionKind.ALBUM,
            presentation = LibraryPresentation.ALBUM_GRID,
            items = listOf(
                LibraryItem.Album(
                    LibraryAlbumRef(sourceId = "netease", id = "9", name = "测试专辑")
                )
            )
        )

        composeRule.setContent {
            MaterialTheme {
                LibraryUnifiedContent(
                    sections = listOf(section),
                    onPlaylistClick = {},
                    onAlbumClick = { clicked = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("测试专辑").assertIsDisplayed().performClick()
        assertEquals("netease", clicked?.sourceId)
        assertEquals("9", clicked?.id)
    }
}
