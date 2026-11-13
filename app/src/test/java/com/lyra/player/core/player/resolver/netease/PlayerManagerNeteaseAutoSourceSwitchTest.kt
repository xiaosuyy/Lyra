package com.lyra.player.core.player.resolver.netease

import com.lyra.player.data.platform.bili.BiliAudioStreamInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerManagerNeteaseAutoSourceSwitchTest {

    @Test
    fun buildNeteaseAutoBiliCacheKey_usesBiliAutoNamespace() {
        val key = buildNeteaseAutoBiliCacheKey(
            bvid = "BV1UyuRz5ERv",
            cid = 31144021363L,
            selectedStream = biliStream(id = 30280)
        )

        assertTrue(key.startsWith("bili-auto-"))
        assertFalse(key.startsWith("netease-"))
    }

    @Test
    fun buildNeteaseAutoBiliCacheKey_isStableForSameStream() {
        val stream = biliStream(id = 30280)
        val first = buildNeteaseAutoBiliCacheKey("BV1UyuRz5ERv", 31144021363L, stream)
        val second = buildNeteaseAutoBiliCacheKey("BV1UyuRz5ERv", 31144021363L, stream)

        assertEquals(first, second)
    }

    @Test
    fun buildNeteaseAutoBiliCacheKey_separatesDifferentStreams() {
        val highKey = buildNeteaseAutoBiliCacheKey(
            bvid = "BV1UyuRz5ERv",
            cid = 31144021363L,
            selectedStream = biliStream(id = 30280)
        )
        val mediumKey = buildNeteaseAutoBiliCacheKey(
            bvid = "BV1UyuRz5ERv",
            cid = 31144021363L,
            selectedStream = biliStream(id = 30232, bitrateKbps = 92)
        )

        assertNotEquals(highKey, mediumKey)
    }

    @Test
    fun scoreNeteaseAutoBiliText_rejectsLongerTitleWithoutArtistHit() {
        val score = scoreNeteaseAutoBiliText(
            song = neteaseSong(),
            title = "晴天 (翻唱)",
            author = "某UP主",
            durationSec = 269
        )

        assertEquals(-1, score)
    }

    @Test
    fun scoreNeteaseAutoBiliText_acceptsExactTitleWithUnknownUploader() {
        val score = scoreNeteaseAutoBiliText(
            song = neteaseSong(),
            title = "晴天",
            author = "音乐搬运工",
            durationSec = 269
        )

        assertTrue(score > 0)
    }

    private fun neteaseSong() = com.lyra.player.data.model.SongItem(
        id = 186016L,
        name = "晴天",
        artist = "周杰伦",
        album = "叶惠美",
        albumId = 1L,
        durationMs = 269_000L,
        coverUrl = null
    )

    private fun biliStream(
        id: Int?,
        bitrateKbps: Int = 200,
        qualityTag: String? = null
    ): BiliAudioStreamInfo {
        val suffix = id?.toString() ?: qualityTag ?: bitrateKbps.toString()
        return BiliAudioStreamInfo(
            id = id,
            mimeType = "audio/mp4",
            bitrateKbps = bitrateKbps,
            qualityTag = qualityTag,
            url = "https://upos.example.bilivideo.com/$suffix.m4s"
        )
    }
}
