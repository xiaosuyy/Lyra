package com.lyra.player.ui.viewmodel.tab.library

import kotlinx.coroutines.runBlocking
import com.lyra.player.R
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.ui.viewmodel.tab.AlbumSummary
import com.lyra.player.ui.viewmodel.tab.BiliPlaylist
import com.lyra.player.ui.viewmodel.tab.PlaylistSummary
import com.lyra.player.ui.viewmodel.tab.YouTubeMusicPlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryProvidersTest {

    @Test
    fun `netease provider maps playlists and albums into unified sections`() {
        val provider = neteaseLibraryProvider(
            playlists = {
                listOf(PlaylistSummary(id = 1L, name = "我的歌单", picUrl = "https://c/1", playCount = 5L, trackCount = 10))
            },
            albums = {
                listOf(AlbumSummary(id = 2L, name = "专辑A", picUrl = "https://c/2", size = 7))
            },
            refresh = {}
        )

        assertEquals(MusicSourceIds.NETEASE, provider.sourceId)
        assertTrue(provider.capabilities.supports(LibraryKind.PLAYLIST))
        assertTrue(provider.capabilities.supports(LibraryKind.ALBUM))

        val sections = provider.sections()
        val playlistSection = sections.first { it.kind == LibrarySectionKind.PLAYLIST }
        assertEquals(R.string.library_tab_netease, playlistSection.titleResId)
        assertEquals(MusicSourceIds.NETEASE, playlistSection.sourceId)
        assertEquals(LibraryPresentation.PLAYLIST_LIST, playlistSection.presentation)
        val playlistItem = playlistSection.items.single() as LibraryItem.Playlist
        assertEquals(MusicSourceIds.NETEASE, playlistItem.sourceId)
        assertEquals("1", playlistItem.ref.id)
        assertEquals("我的歌单", playlistItem.ref.name)
        assertEquals(10, playlistItem.ref.songCount)

        val albumSection = sections.first { it.kind == LibrarySectionKind.ALBUM }
        val albumItem = albumSection.items.single() as LibraryItem.Album
        assertEquals(MusicSourceIds.NETEASE, albumItem.sourceId)
        assertEquals("2", albumItem.ref.id)
        assertEquals(7, albumItem.ref.songCount)
    }

    @Test
    fun `bilibili provider preserves sourceId and playlist fields`() {
        val provider = biliLibraryProvider(
            playlists = {
                listOf(
                    BiliPlaylist(
                        mediaId = 3L,
                        fid = 0L,
                        mid = 0L,
                        title = "收藏夹",
                        count = 4,
                        coverUrl = "https://c/3",
                        subtitle = "UP主"
                    )
                )
            },
            refresh = {}
        )

        val section = provider.sections().single()
        assertEquals(MusicSourceIds.BILIBILI, section.sourceId)
        val item = section.items.single() as LibraryItem.Playlist
        assertEquals(MusicSourceIds.BILIBILI, item.sourceId)
        assertEquals("3", item.ref.id)
        assertEquals("收藏夹", item.ref.name)
        assertEquals(4, item.ref.songCount)
        assertEquals("UP主", item.ref.subtitle)
    }

    @Test
    fun `youtube music provider keeps sourceId`() {
        val provider = youTubeMusicLibraryProvider(
            playlists = {
                listOf(
                    YouTubeMusicPlaylist(
                        browseId = "MPREb_x",
                        playlistId = "PL123",
                        title = "YT歌单",
                        subtitle = "",
                        coverUrl = "https://c/4",
                        trackCount = 6
                    )
                )
            },
            refresh = {}
        )

        val section = provider.sections().single()
        assertEquals(MusicSourceIds.YOUTUBE_MUSIC, section.sourceId)
        val item = section.items.single() as LibraryItem.Playlist
        assertEquals(MusicSourceIds.YOUTUBE_MUSIC, item.sourceId)
        assertEquals("PL123", item.ref.id)
    }

    @Test
    fun `refresh delegate is invoked`() = runBlocking {
        var refreshed = false
        val provider = biliLibraryProvider(playlists = { emptyList() }, refresh = { refreshed = true })
        provider.refresh()
        assertTrue(refreshed)
    }
}
