package com.lyra.player.data.auth.web

import com.lyra.player.core.logging.LyraLogger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal object ForegroundWebLoginGuard {
    const val SKIP_REASON = "foreground_web_login_active"

    private const val TAG = "LYRA-WebLoginGuard"
    private val activeCount = AtomicInteger(0)

    val isActive: Boolean
        get() = activeCount.get() > 0

    fun enter(owner: String): AutoCloseable {
        val normalizedOwner = owner.ifBlank { "unknown" }
        val count = activeCount.incrementAndGet()
        LyraLogger.d(TAG, "Enter owner=$normalizedOwner activeCount=$count")
        return Token(normalizedOwner)
    }

    private class Token(
        private val owner: String
    ) : AutoCloseable {
        private val closed = AtomicBoolean(false)

        override fun close() {
            if (!closed.compareAndSet(false, true)) {
                return
            }
            val count = activeCount.decrementAndGet()
            LyraLogger.d(TAG, "Exit owner=$owner activeCount=$count")
        }
    }
}
