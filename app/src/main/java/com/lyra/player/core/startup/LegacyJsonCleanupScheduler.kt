package com.lyra.player.core.startup

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.data.local.database.store.LegacyJsonCleanupCoordinator
import com.lyra.player.data.local.database.store.LegacyJsonCleanupResult
import com.lyra.player.data.local.database.store.LegacyJsonCleanupStatus

internal object LegacyJsonCleanupScheduler {
    private const val TAG = "LYRA-LegacyJsonCleanup"
    private val running = AtomicBoolean(false)
    private val pendingReason = AtomicReference<String?>(null)
    private val retryDelaysMs = longArrayOf(
        0L,
        1_500L,
        3_000L,
        5_000L,
        8_000L,
        13_000L
    )

    fun schedule(context: Context, reason: String) {
        val appContext = context.applicationContext
        if (!running.compareAndSet(false, true)) {
            pendingReason.set(reason)
            return
        }

        AppContainer.launchBackgroundIo {
            try {
                val coordinator = LegacyJsonCleanupCoordinator(appContext)
                var lastResult: LegacyJsonCleanupResult? = null
                for (attemptIndex in retryDelaysMs.indices) {
                    if (attemptIndex > 0) {
                        delay(retryDelaysMs[attemptIndex])
                    }

                    val plan = coordinator.buildPlan()
                    if (plan.targets.none { it.exists }) {
                        return@launchBackgroundIo
                    }

                    lastResult = coordinator.execute(plan, confirmed = true)
                    if (lastResult.status == LegacyJsonCleanupStatus.COMPLETED) {
                        LyraLogger.d(
                            TAG,
                            "Legacy JSON cleanup completed: reason=$reason, " +
                                "deleted=${lastResult.deletedFiles.size}"
                        )
                        return@launchBackgroundIo
                    }
                }

                lastResult?.let { result ->
                    LyraLogger.d(
                        TAG,
                        "Legacy JSON cleanup pending: reason=$reason, status=${result.status}, " +
                            "deleted=${result.deletedFiles.size}, " +
                            "blocked=${result.blockedFiles}, failed=${result.failedFiles}"
                    )
                }
            } finally {
                running.set(false)
                pendingReason.getAndSet(null)?.let { nextReason ->
                    schedule(appContext, nextReason)
                }
            }
        }
    }
}
