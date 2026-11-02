package com.lyra.player.core.startup.download

import android.content.Context
import kotlinx.coroutines.delay
import com.lyra.player.core.download.GlobalDownloadManager

internal class StartupDownloadRecoveryCoordinator(
    private val context: Context,
    private val awaitResumed: suspend () -> Unit,
    private val requestRecoveryDecision: (Context, String) -> Unit = { targetContext, reason ->
        GlobalDownloadManager.requestPendingDownloadRecoveryDecisionIfNeeded(
            context = targetContext,
            reason = reason
        )
    }
) {
    suspend fun requestWhenMainReady() {
        awaitResumed()
        StartupDownloadRecoveryPlan.defaultAttempts().forEach { attempt ->
            delay(attempt.delayMs)
            requestRecoveryDecision(context, attempt.reason)
        }
    }
}
