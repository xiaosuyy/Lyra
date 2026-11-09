package com.lyra.player.core.api.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TencentArtistJsonTest {

    @Test
    fun `artist songs parser maps header and song list`() {
        val raw = """
            {
              "code": 0,
              "req": {
                "code": 0,
                "data": {
                  "singer_info": { "id": 1, "mid": "001", "name": "周杰伦", "fans": 123456 },
                  "singer_brief": "  华语流行歌手  ",
                  "total_song": 120,
                  "songlist": [
                    {
                      "mid": "songA",
                      "name": "晴天",
                      "interval": 269,
                      "singer": [ { "mid": "001", "name": "周杰伦" } ],
                      "album": { "id": 10, "mid": "albA", "name": "叶惠美" },
                      "id": 9001
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val page = parseTencentArtistSongs(raw, offset = 0)!!

        assertEquals("周杰伦", page.name)
        assertEquals(123456, page.fans)
        assertEquals("华语流行歌手", page.brief)
        assertEquals(120, page.total)
        assertTrue(page.hasMore)
        assertEquals(1, page.items.size)
        val song = page.items.single()
        assertEquals("songA", song.platformId)
        assertEquals("晴天", song.name)
        assertEquals("周杰伦", song.artist)
        assertEquals("叶惠美", song.album)
        assertEquals(269_000L, song.durationMs)
        assertEquals(10L, song.albumId)
        assertEquals("9001", song.albumAudioId)
        assertEquals(tencentAlbumCoverUrl("albA"), song.coverUrl)
    }

    @Test
    fun `artist songs parser reports no more when offset reaches total`() {
        val raw = """
            {
              "req": {
                "data": {
                  "singer_info": { "name": "A", "fans": 0 },
                  "total_song": 1,
                  "songlist": [
                    { "mid": "m1", "name": "n1", "singer": [], "album": { "mid": "a1" } }
                  ]
                }
              }
            }
        """.trimIndent()

        val page = parseTencentArtistSongs(raw, offset = 0)!!

        assertEquals(1, page.total)
        assertFalse(page.hasMore)
    }

    @Test
    fun `artist albums parser maps album entries and pagination`() {
        val raw = """
            {
              "code": 0,
              "data": {
                "total": 42,
                "list": [
                  {
                    "albumID": 1,
                    "albumMID": "albA",
                    "albumName": "叶惠美",
                    "pubTime": "2003-07-31",
                    "latest_song": { "song_count": 11 }
                  }
                ]
              }
            }
        """.trimIndent()

        val page = parseTencentArtistAlbums(raw, offset = 0)!!

        assertEquals(42, page.total)
        assertTrue(page.hasMore)
        val album = page.items.single()
        assertEquals("albA", album.mid)
        assertEquals("叶惠美", album.name)
        assertEquals(11, album.songCount)
        assertEquals("2003-07-31", album.publishDate)
        assertEquals(tencentAlbumCoverUrl("albA"), album.coverUrl)
    }

    @Test
    fun `album songs parser falls back to album mid and name`() {
        val raw = """
            {
              "data": {
                "name": "叶惠美",
                "mid": "",
                "list": [
                  {
                    "songmid": "songA",
                    "songname": "晴天",
                    "interval": 269,
                    "singer": [ { "mid": "001", "name": "周杰伦" } ],
                    "albummid": "",
                    "albumid": 10,
                    "songid": 9001
                  }
                ]
              }
            }
        """.trimIndent()

        val songs = parseTencentAlbumSongs(raw, fallbackAlbumMid = "albA")

        val song = songs.single()
        assertEquals("songA", song.platformId)
        assertEquals("晴天", song.name)
        assertEquals("周杰伦", song.artist)
        assertEquals("叶惠美", song.album)
        assertEquals(269_000L, song.durationMs)
        assertEquals(10L, song.albumId)
        assertEquals("9001", song.albumAudioId)
        assertEquals(tencentAlbumCoverUrl("albA"), song.coverUrl)
    }
}
