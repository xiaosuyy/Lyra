package com.lyra.player.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateSelectionTest {
    @Test
    fun `version label prefers semver inside file name`() {
        assertEquals("0.7.2", AppUpdateSelection.versionLabel("Lyra-0.7.2-arm64-v8a.apk"))
        assertEquals("0.7.2", AppUpdateSelection.versionLabel("Lyra-0.7.2.apk"))
    }

    @Test
    fun `version label falls back to file name without apk suffix`() {
        assertEquals(
            "Lyra-arm64-v8a",
            AppUpdateSelection.versionLabel("Lyra-arm64-v8a.apk")
        )
    }

    @Test
    fun `universal apks pick the newest by modified time`() {
        val files = listOf(
            RemoteApkFile("Lyra-a.apk", "u1", modifiedAt = 100L, sizeBytes = 1L),
            RemoteApkFile("Lyra-b.apk", "u2", modifiedAt = 300L, sizeBytes = 1L),
            RemoteApkFile("Lyra-c.apk", "u3", modifiedAt = 200L, sizeBytes = 1L)
        )

        assertEquals("u2", AppUpdateSelection.selectLatestApk(files, listOf("arm64-v8a"))?.url)
    }

    @Test
    fun `abi matched apk wins over newer foreign apk`() {
        val files = listOf(
            RemoteApkFile("Lyra-arm64-v8a.apk", "arm", modifiedAt = 100L, sizeBytes = 1L),
            RemoteApkFile("Lyra-x86_64.apk", "x86", modifiedAt = 500L, sizeBytes = 1L)
        )

        assertEquals("arm", AppUpdateSelection.selectLatestApk(files, listOf("arm64-v8a"))?.url)
    }

    @Test
    fun `variant abi naming also matches`() {
        val files = listOf(
            RemoteApkFile("Lyra-arm64-v8a.apk", "arm", modifiedAt = 100L, sizeBytes = 1L)
        )

        assertEquals("arm", AppUpdateSelection.selectLatestApk(files, listOf("arm64"))?.url)
    }

    @Test
    fun `falls back to untagged apk when no abi matches`() {
        val files = listOf(
            RemoteApkFile("Lyra-x86_64.apk", "x86", modifiedAt = 500L, sizeBytes = 1L),
            RemoteApkFile("Lyra.apk", "universal", modifiedAt = 100L, sizeBytes = 1L)
        )

        assertEquals(
            "universal",
            AppUpdateSelection.selectLatestApk(files, listOf("arm64-v8a"))?.url
        )
    }

    @Test
    fun `ignores non apk files`() {
        val files = listOf(
            RemoteApkFile("Lyra-sync.json", "sync", modifiedAt = 900L, sizeBytes = 1L),
            RemoteApkFile("update.txt", "text", modifiedAt = 800L, sizeBytes = 1L)
        )

        assertNull(AppUpdateSelection.selectLatestApk(files, listOf("arm64-v8a")))
    }
}
