package com.lyra.player.core.player.policy.wake

import android.content.Context
import android.os.PowerManager
import com.lyra.player.core.logging.LyraLogger

internal object PlaybackTransitionWakeLock {
    private const val TAG = "LYRA-PlaybackWakeLock"
    private const val LOCK_TAG = "LyraPlayer:PlaybackTransition"
    private val lock = Any()
    private var wakeLock: PowerManager.WakeLock? = null
    private var activeRequestToken: Long? = null

    fun acquire(context: Context, requestToken: Long, reason: String) {
        synchronized(lock) {
            val playbackWakeLock = wakeLock ?: runCatching { createWakeLock(context) }
                .onFailure { error -> LyraLogger.w(TAG, "create failed reason=$reason", error) }
                .getOrNull()
                ?.also { wakeLock = it }
                ?: return
            activeRequestToken = requestToken
            runCatching {
                playbackWakeLock.acquire(PLAYBACK_TRANSITION_WAKE_LOCK_LEASE_MS)
            }.onFailure { error ->
                LyraLogger.w(TAG, "acquire failed reason=$reason token=$requestToken", error)
            }.onSuccess {
                LyraLogger.d(
                    TAG,
                    "acquired reason=$reason token=$requestToken " +
                        "timeoutMs=$PLAYBACK_TRANSITION_WAKE_LOCK_LEASE_MS"
                )
            }
        }
    }

    fun release(requestToken: Long, reason: String) {
        synchronized(lock) {
            if (!shouldReleasePlaybackTransitionWakeLock(requestToken, activeRequestToken)) {
                return
            }
            activeRequestToken = null
            val playbackWakeLock = wakeLock ?: return
            if (!runCatching { playbackWakeLock.isHeld }.getOrDefault(false)) return
            runCatching { playbackWakeLock.release() }
                .onSuccess { LyraLogger.d(TAG, "released reason=$reason token=$requestToken") }
                .onFailure { error -> LyraLogger.w(TAG, "release failed reason=$reason", error) }
        }
    }

    fun releaseAll(reason: String) {
        synchronized(lock) {
            activeRequestToken = null
            val playbackWakeLock = wakeLock ?: return
            if (!runCatching { playbackWakeLock.isHeld }.getOrDefault(false)) return
            runCatching { playbackWakeLock.release() }
                .onSuccess { LyraLogger.d(TAG, "released all reason=$reason") }
                .onFailure { error -> LyraLogger.w(TAG, "release all failed reason=$reason", error) }
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
