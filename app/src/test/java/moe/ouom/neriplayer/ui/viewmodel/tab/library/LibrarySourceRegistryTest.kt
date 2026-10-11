package moe.ouom.neriplayer.ui.viewmodel.tab.library

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySourceRegistryTest {

    private class FakeProvider(
        override val sourceId: String,
        override val capabilities: LibraryCapabilities
    ) : LibrarySourceProvider {
        override fun sections(): List<LibrarySection> = emptyList()
    }

    @After
    fun tearDown() {
        LibrarySourceRegistry.clear()
    }

    @Test
    fun `register and resolve provider by sourceId`() {
        val provider = FakeProvider("netease", LibraryCapabilities(setOf(LibraryKind.SONG)))
        LibrarySourceRegistry.register(provider)

        assertEquals(provider, LibrarySourceRegistry.providerFor("netease"))
        assertNull(LibrarySourceRegistry.providerFor("unknown"))
        assertEquals(listOf(provider), LibrarySourceRegistry.all())
    }

    @Test
    fun `capabilities express kind support instead of platform checks`() {
        val caps = LibraryCapabilities(setOf(LibraryKind.PLAYLIST, LibraryKind.ALBUM))
        assertTrue(caps.supports(LibraryKind.PLAYLIST))
        assertTrue(caps.supports(LibraryKind.ALBUM))
        assertFalse(caps.supports(LibraryKind.SONG))
        assertFalse(caps.supports(LibraryKind.ARTIST))
    }

    @Test
    fun `clear empties registry`() {
        LibrarySourceRegistry.register(FakeProvider("local", LibraryCapabilities()))
        LibrarySourceRegistry.clear()
        assertNull(LibrarySourceRegistry.providerFor("local"))
        assertTrue(LibrarySourceRegistry.all().isEmpty())
    }

    @Test
    fun `library item sourceId is preserved`() {
        val playlist = LibraryItem.Playlist(
            LibraryPlaylistRef(sourceId = "tencent", id = "1", name = "demo")
        )
        assertEquals("tencent", playlist.sourceId)

        val album = LibraryItem.Album(
            LibraryAlbumRef(sourceId = "netease", id = "2", name = "album")
        )
        assertEquals("netease", album.sourceId)

        val artist = LibraryItem.Artist(
            LibraryArtistRef(sourceId = "kugou", id = "3", name = "artist")
        )
        assertEquals("kugou", artist.sourceId)
    }
}
