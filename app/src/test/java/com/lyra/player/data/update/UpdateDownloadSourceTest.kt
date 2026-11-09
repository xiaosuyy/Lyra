package com.lyra.player.data.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateDownloadSourceTest {
    private val releaseAssetUrl =
        "https://github.com/xiaosuyy/Lyra/releases/download/v1.0.0/Lyra-v1.0.0.apk"

    @Test
    fun githubKeepsOriginalDownloadUrl() {
        assertEquals(
            releaseAssetUrl,
            UpdateDownloadSource.GitHub.routedDownloadUrl(releaseAssetUrl)
        )
    }

    @Test
    fun ghProxyPrefixesGitHubReleaseAssetUrl() {
        assertEquals(
            "https://gh-proxy.com/$releaseAssetUrl",
            UpdateDownloadSource.GhProxy.routedDownloadUrl(releaseAssetUrl)
        )
    }

    @Test
    fun ghProxyLeavesNonGitHubAndInvalidUrlsUntouched() {
        val apiUrl = "https://api.github.com/repos/xiaosuyy/Lyra/releases/latest"

        assertEquals(apiUrl, UpdateDownloadSource.GhProxy.routedDownloadUrl(apiUrl))
        assertEquals("", UpdateDownloadSource.GhProxy.routedDownloadUrl(""))
    }

    @Test
    fun policyFallsBackToGitHubForUnknownValues() {
        assertEquals(UpdateDownloadSource.GhProxy, UpdateDownloadSourcePolicy.fromStorage("ghproxy"))
        assertEquals(UpdateDownloadSource.GhProxy, UpdateDownloadSourcePolicy.fromStorage("gh-proxy"))
        assertEquals(UpdateDownloadSource.GitHub, UpdateDownloadSourcePolicy.fromStorage(null))
        assertEquals(UpdateDownloadSource.GitHub, UpdateDownloadSourcePolicy.fromStorage("unknown"))
        assertEquals("ghproxy", UpdateDownloadSourcePolicy.normalize("GHProxy"))
    }
}
