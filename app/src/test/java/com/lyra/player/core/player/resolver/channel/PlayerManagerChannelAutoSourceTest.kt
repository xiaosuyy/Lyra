package com.lyra.player.core.player.resolver.channel

import com.lyra.player.core.player.model.SongUrlResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerManagerChannelAutoSourceTest {
    @Test
    fun scriptSuccessDoesNotWaitForMatchingSource() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var cancelled = false
        val expected = SongUrlResult.Success("https://example.com/script.mp3")
        val result = withTimeout(2_000) {
            raceChannelAudioSources(
                script = { started.await(); expected },
                matched = {
                    started.complete(Unit)
                    try { awaitCancellation() } finally { cancelled = true }
                }
            )
        }
        assertSame(expected, result)
        assertTrue(cancelled)
    }

    @Test
    fun matchingSuccessDoesNotWaitForScript() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var cancelled = false
        val expected = SongUrlResult.Success("https://example.com/matched.mp3")
        val result = withTimeout(2_000) {
            raceChannelAudioSources(
                script = {
                    started.complete(Unit)
                    try { awaitCancellation() } finally { cancelled = true }
                },
                matched = { started.await(); expected }
            )
        }
        assertSame(expected, result)
        assertTrue(cancelled)
    }

    @Test
    fun fastFailureDoesNotDiscardOtherSuccess() = runBlocking {
        val expected = SongUrlResult.Success("https://example.com/matched.mp3")
        assertSame(expected, raceChannelAudioSources({ SongUrlResult.Failure }, { expected }))
        assertSame(expected, raceChannelAudioSources({ expected }, { null }))
    }

    @Test
    fun noSuccessfulSourceReturnsFailure() = runBlocking {
        assertSame(SongUrlResult.Failure, raceChannelAudioSources({ SongUrlResult.Failure }, { null }))
    }

    @Test
    fun parentCancellationStopsBothRequests() = runBlocking {
        val scriptStarted = CompletableDeferred<Unit>()
        val matchedStarted = CompletableDeferred<Unit>()
        var scriptCancelled = false
        var matchedCancelled = false
        val job = async {
            raceChannelAudioSources(
                script = {
                    scriptStarted.complete(Unit)
                    try { awaitCancellation() } finally { scriptCancelled = true }
                },
                matched = {
                    matchedStarted.complete(Unit)
                    try { awaitCancellation() } finally { matchedCancelled = true }
                }
            )
        }
        withTimeout(2_000) {
            scriptStarted.await()
            matchedStarted.await()
            job.cancel()
            job.join()
        }
        assertTrue(scriptCancelled)
        assertTrue(matchedCancelled)
    }
}
