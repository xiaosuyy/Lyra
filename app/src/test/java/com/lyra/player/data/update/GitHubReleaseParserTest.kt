package com.lyra.player.data.update

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseParserTest {
    private val releaseJson = """
        {
          "tag_name": "Lyra-2643d2b.11061518",
          "body": "## 更新内容\n- 修复若干问题",
          "published_at": "2026-11-06T07:00:00Z",
          "assets": [
            {
              "name": "Lyra-2643d2b.11061518-arm64-v8a.apk",
              "browser_download_url": "https://example.com/arm64.apk",
              "size": 12345,
              "updated_at": "2026-11-06T07:00:00Z"
            },
            {
              "name": "Lyra-2643d2b.11061518-x86.apk",
              "browser_download_url": "https://example.com/x86.apk",
              "size": 12345,
              "updated_at": "2026-11-06T08:00:00Z"
            },
            {
              "name": "SHA256SUMS.txt",
              "browser_download_url": "https://example.com/sums.txt",
              "size": 100,
              "updated_at": "2026-11-06T08:00:00Z"
            }
          ]
        }
    """.trimIndent()

    private fun parse(
        json: String = releaseJson,
        installedAtMillis: Long = 0L
    ): AppUpdateInfo? = GitHubReleaseParser.parse(
        release = JsonParser.parseString(json).asJsonObject,
        supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        installedAtMillis = installedAtMillis
    )

    @Test
    fun `picks matching abi asset and exposes release notes`() {
        val update = parse() ?: error("expected an update")

        assertEquals("2643d2b.11061518", update.versionLabel)
        assertEquals("Lyra-2643d2b.11061518-arm64-v8a.apk", update.fileName)
        assertEquals("https://example.com/arm64.apk", update.remoteUrl)
        assertEquals(12345L, update.sizeBytes)
        assertEquals("## 更新内容\n- 修复若干问题", update.changelog)
    }

    @Test
    fun `returns null when installed build is newer than the release asset`() {
        val releasedAt = java.time.Instant.parse("2026-11-06T07:00:00Z").toEpochMilli()
        assertNull(parse(installedAtMillis = releasedAt))
        assertNull(parse(installedAtMillis = releasedAt + 1))
    }

    @Test
    fun `ignores newer assets for other abis`() {
        val x86Only = """
            {
              "tag_name": "Lyra-1.0.0",
              "body": "",
              "assets": [
                {
                  "name": "Lyra-1.0.0-x86.apk",
                  "browser_download_url": "https://example.com/x86.apk",
                  "size": 1,
                  "updated_at": "2030-01-01T00:00:00Z"
                }
              ]
            }
        """.trimIndent()
        assertNull(parse(json = x86Only))
    }

    @Test
    fun `falls back to universal apk when no abi tag matches`() {
        val universal = """
            {
              "tag_name": "Lyra-2.0.0",
              "body": "notes",
              "assets": [
                {
                  "name": "Lyra-2.0.0.apk",
                  "browser_download_url": "https://example.com/universal.apk",
                  "size": 42,
                  "updated_at": "2030-01-01T00:00:00Z"
                }
              ]
            }
        """.trimIndent()
        val update = parse(json = universal) ?: error("expected an update")
        assertEquals("2.0.0", update.versionLabel)
        assertEquals("Lyra-2.0.0.apk", update.fileName)
    }

    @Test
    fun `version label strips brand prefix and falls back to asset name`() {
        assertEquals(
            "2643d2b.11061518",
            GitHubReleaseParser.versionLabel("Lyra-2643d2b.11061518", "Lyra-x.apk")
        )
        assertEquals(
            "0.7.2",
            GitHubReleaseParser.versionLabel("v0.7.2", "Lyra-0.7.2.apk")
        )
        assertEquals(
            "Lyra-2643d2b.11061518-arm64-v8a",
            GitHubReleaseParser.versionLabel("", "Lyra-2643d2b.11061518-arm64-v8a.apk")
        )
    }

    @Test
    fun `repository validation`() {
        assertTrue(GitHubReleaseChecker.isValidRepo("cwuom/NeriPlayer"))
        assertTrue(GitHubReleaseChecker.isValidRepo("/cwuom/NeriPlayer/"))
        assertFalse(GitHubReleaseChecker.isValidRepo("cwuom"))
        assertFalse(GitHubReleaseChecker.isValidRepo("https://github.com/cwuom/NeriPlayer"))
    }
}
