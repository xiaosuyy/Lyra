package com.lyra.player.core.player.service

import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.core.player.persistence.scheduleStatePersist
import com.lyra.player.core.player.watchdog.cancelPlaybackStartupWatchdog

internal fun PlayerManager.suspendPlaybackForServiceRestart(reason: String) {
    if (!isPlayerInitialized() || currentSongFlow.value == null) return
    val shouldResume = isTransportActiveWithoutInitialization()
    val positionMs = runCatching { player.currentPosition.coerceAtLeast(0L) }
        .getOrDefault(playbackPositionFlow.value.coerceAtLeast(0L))

    playbackRequestToken += 1L
    playJob?.cancel()
    playJob = null
    cancelPlaybackStartupWatchdog(reason = "service_restart")
    cancelPendingPauseRequest(resetVolumeToFull = true)
    cancelVolumeFade(resetToFull = true)
    runCatching {
        player.playWhenReady = false
        player.stop()
    }.onFailure { error ->
        LyraLogger.w(
            "LYRA-PlayerManager",
            "failed to suspend playback for service restart reason=$reason",
            error
        )
    }

    _isPlayingFlow.value = false
    _playWhenReadyFlow.value = false
    _playbackPositionMs.value = positionMs
    stopProgressUpdates()
    restoredShouldResumePlayback = shouldResume
    restoredResumePositionMs = positionMs
    updateResumePlaybackRequested(shouldResume)
    scheduleStatePersist(
        positionMs = positionMs,
        shouldResumePlayback = shouldResume,
        debounceMs = 0L
    )
    LyraLogger.i(
        "LYRA-PlayerManager",
        "suspended playback for service restart reason=$reason positionMs=$positionMs resume=$shouldResume"
    )
}
