package com.lyra.player.ui.viewmodel.tab

import com.lyra.player.data.local.playlist.sync.NeteaseRemotePlaylist
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryPlaylistImportFilterTest {
    @Test
    fun `created and collected playlists are both importable`() {
        val playlists = listOf(
            NeteaseRemotePlaylist(301L, "我喜欢的音乐", 5, isFavorites = true),
            NeteaseRemotePlaylist(302L, "自建歌单", 8),
            NeteaseRemotePlaylist(303L, "收藏歌单", 30, isCollected = true)
        )

        assertEquals(
            listOf(302L, 303L),
            playlists.importableLibraryPlaylists().map { it.id }
        )
    }

    @Test
    fun `favorites playlist is skipped by name even without special type`() {
        val playlists = listOf(
            NeteaseRemotePlaylist(401L, "我喜欢的音乐（128）", 12),
            NeteaseRemotePlaylist(402L, "我喜欢", 3),
            NeteaseRemotePlaylist(403L, "我的歌单", 3)
        )

        assertEquals(
            listOf(403L),
            playlists.importableLibraryPlaylists().map { it.id }
        )
    }
}
