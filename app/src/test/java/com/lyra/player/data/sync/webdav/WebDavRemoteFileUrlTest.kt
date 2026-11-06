package com.lyra.player.data.sync.webdav

import org.junit.Assert.assertEquals
import org.junit.Test

class WebDavRemoteFileUrlTest {
    private val serverUrl = "https://example.teracloud.jp/dav"

    @Test
    fun `builds new sync file name by default`() {
        assertEquals(
            "https://example.teracloud.jp/dav/Lyra/Lyra-sync.json",
            WebDavApiClient.buildRemoteFileUrl(serverUrl, "/Lyra/")
        )
    }

    @Test
    fun `builds legacy sync file name for migration read`() {
        assertEquals(
            "https://example.teracloud.jp/dav/Lyra/neriplayer-sync.json",
            WebDavApiClient.buildRemoteFileUrl(
                serverUrl,
                "Lyra",
                WebDavApiClient.LEGACY_REMOTE_FILE_NAME
            )
        )
    }
}
