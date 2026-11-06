package com.lyra.player.data.sync.webdav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class WebDavMultistatusParserTest {
    private val directoryUrl = "https://example.teracloud.jp/dav/Lyra/"

    @Test
    fun `parses files and directories`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/Lyra/Lyra-0.7.2-arm64-v8a.apk</d:href>
                <d:propstat>
                  <d:prop>
                    <d:getlastmodified>Mon, 03 Nov 2025 10:20:30 GMT</d:getlastmodified>
                    <d:getcontentlength>73400320</d:getcontentlength>
                  </d:prop>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/Lyra/update.txt</d:href>
                <d:propstat>
                  <d:prop>
                    <d:getlastmodified>Tue, 04 Nov 2025 08:00:00 GMT</d:getlastmodified>
                    <d:getcontentlength>120</d:getcontentlength>
                  </d:prop>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/Lyra/sub/</d:href>
                <d:propstat>
                  <d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val entries = WebDavMultistatusParser.parse(xml, directoryUrl)

        assertEquals(3, entries.size)
        val apk = entries[0]
        assertEquals("Lyra-0.7.2-arm64-v8a.apk", apk.name)
        assertEquals(73_400_320L, apk.sizeBytes)
        assertFalse(apk.isDirectory)
        assertEquals(
            ZonedDateTime.parse("Mon, 03 Nov 2025 10:20:30 GMT", DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant()
                .toEpochMilli(),
            apk.lastModifiedAt
        )
        assertEquals("update.txt", entries[1].name)
        assertTrue(entries[2].isDirectory)
        assertEquals("sub", entries[2].name)
    }

    @Test
    fun `resolves percent encoded href to absolute url`() {
        val xml = """
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/Lyra/My%20App%2B1.apk</d:href>
                <d:propstat><d:prop><d:getcontentlength>10</d:getcontentlength></d:prop></d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val entry = WebDavMultistatusParser.parse(xml, directoryUrl).single()

        assertEquals("My App+1.apk", entry.name)
        assertEquals("https://example.teracloud.jp/dav/Lyra/My%20App%2B1.apk", entry.url)
    }
}
