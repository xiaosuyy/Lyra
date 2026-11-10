package com.lyra.player.core.customsource

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomSourceRuntimeConcurrencyTest {
    @Test
    fun fastRequestFinishesWhileSlowRequestIsBlocked() = runBlocking {
        val started = CountDownLatch(1)
        val releaseSlow = CountDownLatch(1)
        val runtime = runtime(started, releaseSlow)
        try {
            assertTrue(runtime.load(script).loaded)
            val slow = async(Dispatchers.IO) { runtime.resolveMusicUrl("test", "slow", JSONObject()) }
            assertTrue(withContext(Dispatchers.IO) { started.await(3, TimeUnit.SECONDS) })
            val fast = withTimeout(3_000) {
                runtime.resolveMusicUrl("test", "fast", JSONObject())
            }
            assertEquals("https://example.com/fast.mp3", fast)
            assertFalse(slow.isCompleted)
            releaseSlow.countDown()
            assertEquals("https://example.com/slow.mp3", withTimeout(3_000) { slow.await() })
        } finally {
            releaseSlow.countDown()
            runtime.release()
        }
    }

    @Test
    fun releaseCancelsOwnedNetworkCallAndCompletesWaitingResolver() = runBlocking {
        val started = CountDownLatch(1)
        val releaseSlow = CountDownLatch(1)
        val call = AtomicReference<Call>()
        val runtime = runtime(started, releaseSlow, call)
        try {
            assertTrue(runtime.load(script).loaded)
            val pending = async(Dispatchers.IO) { runtime.resolveMusicUrl("test", "slow", JSONObject()) }
            assertTrue(withContext(Dispatchers.IO) { started.await(3, TimeUnit.SECONDS) })
            runtime.release()
            assertTrue(call.get().isCanceled())
            assertNull(withTimeout(3_000) { pending.await() })
        } finally {
            releaseSlow.countDown()
            runtime.release()
        }
    }

    @Test
    fun scriptAbortCancelsOnlyItsNetworkCall() = runBlocking {
        val started = CountDownLatch(1)
        val releaseSlow = CountDownLatch(1)
        val call = AtomicReference<Call>()
        val runtime = runtime(started, releaseSlow, call)
        try {
            val abortScript = """
                var abortPending;
                lx.on(lx.EVENT_NAMES.request, function (request) {
                    if (request.info.type === 'slow') {
                        return new Promise(function (resolve) {
                            abortPending = lx.request('https://example.com/slow', {}, function () {
                                resolve('https://example.com/unexpected.mp3');
                            });
                        });
                    }
                    abortPending();
                    return 'https://example.com/aborted.mp3';
                });
                lx.send(lx.EVENT_NAMES.inited, {
                    sources: { test: { type: 'music', qualitys: ['slow', 'fast'] } }
                });
            """.trimIndent()
            assertTrue(runtime.load(abortScript).loaded)
            val pending = async(Dispatchers.IO) { runtime.resolveMusicUrl("test", "slow", JSONObject()) }
            assertTrue(withContext(Dispatchers.IO) { started.await(3, TimeUnit.SECONDS) })
            assertEquals(
                "https://example.com/aborted.mp3",
                withTimeout(3_000) { runtime.resolveMusicUrl("test", "fast", JSONObject()) }
            )
            assertTrue(call.get().isCanceled())
            pending.cancel()
        } finally {
            releaseSlow.countDown()
            runtime.release()
        }
    }

    private fun runtime(
        started: CountDownLatch,
        releaseSlow: CountDownLatch,
        capturedCall: AtomicReference<Call> = AtomicReference()
    ): CustomSourceRuntime {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val path = chain.request().url.encodedPath
            if (path == "/slow") {
                capturedCall.set(chain.call())
                started.countDown()
                check(releaseSlow.await(10, TimeUnit.SECONDS))
            }
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("https://example.com$path.mp3".toResponseBody())
                .build()
        }.build()
        return CustomSourceRuntime(
            InstrumentationRegistry.getInstrumentation().targetContext,
            client,
            "{}"
        )
    }

    private val script = """
        lx.on(lx.EVENT_NAMES.request, function (request) {
            return new Promise(function (resolve, reject) {
                lx.request('https://example.com/' + request.info.type, {}, function (error, response, body) {
                    if (error) reject(error);
                    else resolve(body);
                });
            });
        });
        lx.send(lx.EVENT_NAMES.inited, {
            sources: { test: { type: 'music', qualitys: ['slow', 'fast'] } }
        });
    """.trimIndent()
}
