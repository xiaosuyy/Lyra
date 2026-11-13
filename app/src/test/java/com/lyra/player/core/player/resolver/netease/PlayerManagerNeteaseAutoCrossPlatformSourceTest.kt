package com.lyra.player.core.player.resolver.netease

import com.lyra.player.data.model.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerManagerNeteaseAutoCrossPlatformSourceTest {

    @Test
    fun score_acceptsSameTitleArtistAndDuration() {
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗",
            candidateArtist = "许飞",
            candidateDurationMs = 228_000L
        )

        assertTrue(score > 0)
    }

    @Test
    fun score_rejectsDifferentArtist() {
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗",
            candidateArtist = "李健",
            candidateDurationMs = 228_000L
        )

        assertEquals(-1, score)
    }

    @Test
    fun score_rejectsDurationMismatch() {
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗",
            candidateArtist = "许飞",
            candidateDurationMs = 260_000L
        )

        assertEquals(-1, score)
    }

    @Test
    fun score_rejectsUnknownCandidateDuration() {
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗",
            candidateArtist = "许飞",
            candidateDurationMs = 0L
        )

        assertEquals(-1, score)
    }

    @Test
    fun score_rejectsInstrumentalVersion() {
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗 (伴奏)",
            candidateArtist = "许飞",
            candidateDurationMs = 228_000L
        )

        assertEquals(-1, score)
    }

    @Test
    fun score_rejectsLongerTitleContainingTarget() {
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗 (Live)",
            candidateArtist = "许飞",
            candidateDurationMs = 228_000L
        )

        assertEquals(-1, score)
    }

    @Test
    fun score_rejectsPartialArtistName() {
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗",
            candidateArtist = "许",
            candidateDurationMs = 228_000L
        )

        assertEquals(-1, score)
    }

    @Test
    fun score_rejectsShortTitleMatchingLongerCandidate() {
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong().copy(name = "100"),
            candidateName = "100种生活",
            candidateArtist = "许飞",
            candidateDurationMs = 228_000L
        )

        assertEquals(-1, score)
    }

    @Test
    fun score_acceptsCandidateArtistWithExtraNames() {
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗",
            candidateArtist = "许飞 / 李健",
            candidateDurationMs = 229_000L
        )

        assertTrue(score > 0)
    }

    @Test
    fun score_acceptsArtistAliasInParentheses() {
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗",
            candidateArtist = "许飞 (Xu Fei)",
            candidateDurationMs = 228_000L
        )

        assertTrue(score > 0)
    }

    @Test
    fun score_prefersCloserDuration() {
        val exact = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗",
            candidateArtist = "许飞",
            candidateDurationMs = 228_000L
        )
        val loose = scoreNeteaseAutoCrossPlatformCandidate(
            song = neteaseSong(),
            candidateName = "父亲写的散文诗",
            candidateArtist = "许飞",
            candidateDurationMs = 235_000L
        )

        assertTrue(exact > loose)
    }

    private fun neteaseSong(): SongItem = SongItem(
        id = 1330348068L,
        name = "父亲写的散文诗",
        artist = "许飞",
        album = "父亲写的散文诗",
        albumId = 35006558L,
        durationMs = 228_000L,
        coverUrl = null
    )
}
