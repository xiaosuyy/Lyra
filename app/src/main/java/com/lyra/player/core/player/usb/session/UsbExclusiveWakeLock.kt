package com.lyra.player.core.player.usb.session

import android.content.Context
import android.os.PowerManager
import com.lyra.player.core.logging.LyraLogger

internal object UsbExclusiveWakeLock {
    private const val TAG = "LYRA-UsbWakeLock"
    private const val LOCK_TAG = "LyraPlayer:UsbExclusivePlayback"
    private const val LEASE_TIMEOUT_MS = 10L * 60L * 1000L
    private val lock = Any()
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire(context: Context, reason: String) {
        synchronized(lock) {
            val playbackWakeLock = wakeLock ?: runCatching { createWakeLock(context) }
                .onFailure { error -> LyraLogger.w(TAG, "create failed reason=$reason", error) }
                .getOrNull()
                ?.also { wakeLock = it }
                ?: return
            val alreadyHeld = runCatching { playbackWakeLock.isHeld }.getOrDefault(false)
            runCatching { playbackWakeLock.acquire(LEASE_TIMEOUT_MS) }
                .onSuccess {
                    if (!alreadyHeld) {
                        LyraLogger.d(TAG, "acquired reason=$reason timeoutMs=$LEASE_TIMEOUT_MS")
                    }
                }
                .onFailure { error -> LyraLogger.w(TAG, "acquire failed reason=$reason", error) }
        }
    }

    fun release(reason: String) {
        synchronized(lock) {
            val playbackWakeLock = wakeLock ?: return
            if (!runCatching { playbackWakeLock.isHeld }.getOrDefault(false)) return
            runCatching { playbackWakeLock.release() }
                .onSuccess { LyraLogger.d(TAG, "released reason=$reason") }
                .onFailure { error -> LyraLogger.w(TAG, "release failed reason=$reason", error) }
        }
    }

    fun isHeld(): Boolean {
        return synchronized(lock) {
            wakeLock?.let { runCatching { it.isHeld }.getOrDefault(false) } == true
        }
    }

    private fun createWakeLock(context: Context): PowerManager.WakeLock {
        val powerManager = context.applicationContext
            .getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_TAG).apply {
            setReferenceCounted(false)
        }
    }
}
