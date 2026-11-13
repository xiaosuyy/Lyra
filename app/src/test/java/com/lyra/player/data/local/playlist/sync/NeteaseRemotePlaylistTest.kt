package com.lyra.player.data.local.playlist.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class NeteaseRemotePlaylistTest {
    @Test
    fun `parse remote playlists keeps only owner playlists and de duplicates ids`() {
        val raw = """
            {
              "code": 200,
              "playlist": [
                {
                  "id": 101,
                  "name": "Daily",
                  "trackCount": 12,
                  "creator": { "userId": 7 }
                },
                {
                  "id": 102,
                  "name": "Subscribed",
                  "trackCount": 30,
                  "creator": { "userId": 8 }
                },
                {
                  "id": 101,
                  "name": "Duplicate",
                  "trackCount": 99,
                  "creator": { "userId": 7 }
                }
              ]
            }
        """.trimIndent()

        val playlists = parseNeteaseRemotePlaylists(raw, ownerUserId = 7L)

        assertEquals(
            listOf(NeteaseRemotePlaylist(id = 101L, name = "Daily", trackCount = 12)),
            playlists
        )
    }

    @Test
    fun `parse remote playlists accepts playlists fallback key`() {
        val raw = """
            {
              "code": 200,
              "playlists": [
                {
                  "id": 201,
                  "name": "Fallback",
                  "trackCount": 3,
                  "creator": { "userId": 9 }
                }
              ]
            }
        """.trimIndent()

        val playlists = parseNeteaseRemotePlaylists(raw, ownerUserId = 9L)

        assertEquals(
            listOf(NeteaseRemotePlaylist(id = 201L, name = "Fallback", trackCount = 3)),
            playlists
        )
    }

    @Test
    fun `parse remote playlists marks collected and favorites when including others`() {
        val raw = """
            {
              "code": 200,
              "playlist": [
                {
                  "id": 301,
                  "name": "我喜欢的音乐",
                  "trackCount": 5,
                  "specialType": 5,
                  "subscribed": false,
                  "creator": { "userId": 7 }
                },
                {
                  "id": 302,
                  "name": "Self Made",
                  "trackCount": 8,
                  "subscribed": false,
                  "creator": { "userId": 7 }
                },
                {
                  "id": 303,
                  "name": "Collected",
                  "trackCount": 30,
                  "subscribed": true,
                  "creator": { "userId": 8 }
                }
              ]
            }
        """.trimIndent()

        val playlists = parseNeteaseRemotePlaylists(raw, ownerUserId = 7L, onlyOwned = false)

        assertEquals(
            listOf(
                NeteaseRemotePlaylist(301L, "我喜欢的音乐", 5, isFavorites = true, isCollected = false),
                NeteaseRemotePlaylist(302L, "Self Made", 8),
                NeteaseRemotePlaylist(303L, "Collected", 30, isCollected = true)
            ),
            playlists
        )
    }

    @Test
    fun `parse remote playlists reports api errors`() {
        val error = assertThrows(IOException::class.java) {
            parseNeteaseRemotePlaylists(
                raw = """{"code":301,"msg":"login required"}""",
                ownerUserId = 9L
            )
        }

        assertTrue(error.message.orEmpty().contains("login required"))
    }
}
