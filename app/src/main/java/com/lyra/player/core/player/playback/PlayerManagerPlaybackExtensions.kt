@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.lyra.player.core.player.playback

import android.os.SystemClock
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lyra.player.R
import com.lyra.player.core.api.bili.BiliClient
import com.lyra.player.core.api.bili.buildBiliPartSong
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.core.lyricon.LyriconManager
import com.lyra.player.core.lyricon.mediaLyriconPositionMs
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.core.player.LocalPlaylistPlaybackSource
import com.lyra.player.core.player.audio.focus.StartupAudioFocusController
import com.lyra.player.core.player.debug.playbackStateName
import com.lyra.player.core.player.lifecycle.clearUsbExclusiveInterruptedPlaybackIntent
import com.lyra.player.core.player.lifecycle.prepareUsbExclusiveRouteForManualPlayback
import com.lyra.player.core.player.lifecycle.updateAudioOffloadPreferences
import com.lyra.player.core.player.lyrics.isExternalBluetoothLyricCadenceActive
import com.lyra.player.core.player.lyrics.updateExternalBluetoothLyricLine
import com.lyra.player.core.player.model.PlayerEvent
import com.lyra.player.core.player.model.SongUrlResult
import com.lyra.player.core.player.model.insertIntoShuffleRestorePlaylist
import com.lyra.player.core.player.model.removeFromShuffleRestorePlaylist
import com.lyra.player.core.player.model.replaceInShuffleRestorePlaylist
import com.lyra.player.core.player.model.resolvePlayerQueueRestoreOrder
import com.lyra.player.core.player.model.resolvePlayerRepeatAllShuffleOrder
import com.lyra.player.core.player.model.resolvePlayerSequentialShuffleOrder
import com.lyra.player.core.player.persistence.persistStateNow
import com.lyra.player.core.player.persistence.scheduleStatePersist
import com.lyra.player.core.player.policy.command.PlaybackCommandSource
import com.lyra.player.core.player.policy.command.PlaybackStartPlan
import com.lyra.player.core.player.policy.command.USB_TRACK_TRANSITION_PROTECTION_FADE_DURATION_MS
import com.lyra.player.core.player.policy.command.resolveEffectivePlaybackStartPlan
import com.lyra.player.core.player.policy.command.resolveManagedPlaybackStartPlan
import com.lyra.player.core.player.policy.command.resolveManualResumePlaybackDecision
import com.lyra.player.core.player.policy.command.resolveNoFadePlaybackStartPlan
import com.lyra.player.core.player.policy.command.resolvePauseVolumePlan
import com.lyra.player.core.player.policy.command.resolvePlaybackContinuationStartPlan
import com.lyra.player.core.player.policy.command.shouldPausePlaybackWhenToggling
import com.lyra.player.core.player.policy.failure.PlaybackFailureAdvanceAction
import com.lyra.player.core.player.policy.failure.resolvePlaybackFailureAdvanceAction
import com.lyra.player.core.player.policy.pending.resolvePendingMediaLoadEntryAction
import com.lyra.player.core.player.policy.pending.resolvePendingPauseAction
import com.lyra.player.core.player.policy.pending.resolvePendingPlayAction
import com.lyra.player.core.player.policy.pending.resolvePendingSeekAction
import com.lyra.player.core.player.policy.pending.resolveSeekExecutionAction
import com.lyra.player.core.player.policy.pending.shouldApplyResolvedMedia
import com.lyra.player.core.player.policy.pending.shouldApplyResolvedMediaSideEffects
import com.lyra.player.core.player.policy.progress.LONG_FORM_PLAYBACK_MIN_DURATION_MS
import com.lyra.player.core.player.policy.progress.PLAYBACK_PROGRESS_STATS_UPDATE_INTERVAL_MS
import com.lyra.player.core.player.policy.progress.resolvePlaybackProgressUpdateIntervalMs
import com.lyra.player.core.player.policy.progress.shouldRunPlaybackProgressUpdates
import com.lyra.player.core.player.policy.skip.BiliSkipSegmentSource
import com.lyra.player.core.player.policy.skip.resolveBiliSkipSegmentPromptMessageRes
import com.lyra.player.core.player.policy.wake.PlaybackTransitionWakeLock
import com.lyra.player.core.player.prefetch.cancelGenericUrlPrefetchUnlessReusableForSong
import com.lyra.player.core.player.prefetch.cancelYouTubePrefetchForPlaybackDemand
import com.lyra.player.core.player.prefetch.clearPlaybackDemandCacheKey
import com.lyra.player.core.player.prefetch.kickoffYouTubePlaybackIntentWarmup
import com.lyra.player.core.player.prefetch.replacePlaybackDemandCacheKey
import com.lyra.player.core.player.resolver.youtube.YouTubeSeekRefreshPolicy
import com.lyra.player.core.player.service.AudioPlayerService
import com.lyra.player.core.player.url.cancelUrlRefreshIfNotReusableForPendingLoad
import com.lyra.player.core.player.url.allowsCustomCacheKey
import com.lyra.player.core.player.url.resolveSongUrl
import com.lyra.player.core.player.url.synchronizeCachedPlaybackDescriptor
import com.lyra.player.core.player.url.youtubePlaybackRecoveryStrategyForSeek
import com.lyra.player.core.player.usb.path.UsbExclusiveAudioPathState
import com.lyra.player.core.player.usb.path.UsbExclusiveAudioPathTracker
import com.lyra.player.core.player.watchdog.cancelPlaybackStartupWatchdog
import com.lyra.player.core.player.watchdog.clearActivePlaybackCandidates
import com.lyra.player.core.player.watchdog.configureActivePlaybackCandidates
import com.lyra.player.core.player.watchdog.currentPlaybackCandidate
import com.lyra.player.core.player.watchdog.isPlaybackActuallyAdvancing
import com.lyra.player.core.player.watchdog.resetPlaybackProgressAdvanceBaseline
import com.lyra.player.core.player.watchdog.resetPlaybackRuntimeWatchdog
import com.lyra.player.core.player.watchdog.recordPlaybackRuntimeProgress
import com.lyra.player.core.player.watchdog.schedulePlaybackStartupWatchdog
import com.lyra.player.core.player.watchdog.schedulePlaybackRuntimeWatchdog
import com.lyra.player.data.local.audioimport.LocalAudioImportManager
import com.lyra.player.data.local.playlist.runLocalPlaylistMutationSafely
import com.lyra.player.data.model.SongItem
import com.lyra.player.data.model.sameIdentityAs
import com.lyra.player.data.model.stableKey
import com.lyra.player.data.platform.youtube.extractYouTubeMusicVideoId
import com.lyra.player.data.platform.youtube.youtubeMusicThumbnailUrl
import com.lyra.player.ui.feedback.AppFeedback

internal fun PlayerManager.cancelVolumeFadeImpl(resetToFull: Boolean = false) {
    val hadActiveFade = volumeFadeJob?.isActive == true
    if (hadActiveFade || resetToFull) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "cancelVolumeFade: hadActiveFade=$hadActiveFade, resetToFull=$resetToFull, currentSong=${_currentSongFlow.value?.name}"
        )
    }
    volumeFadeJob?.cancel()
    volumeFadeJob = null
    if (resetToFull && !isAudioRouteMuteSuppressed() && isPlayerInitialized()) {
        runPlayerActionOnMainThread {
            runCatching { player.volume = 1f }
        }
    }
}

internal fun PlayerManager.cancelPendingPauseRequestImpl(resetVolumeToFull: Boolean = false) {
    val hadPendingPause = pendingPauseJob?.isActive == true
    if (hadPendingPause || resetVolumeToFull) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "cancelPendingPauseRequest: hadPendingPause=$hadPendingPause, resetVolumeToFull=$resetVolumeToFull, currentSong=${_currentSongFlow.value?.name}"
        )
    }
    pendingPauseJob?.cancel()
    pendingPauseJob = null
    if (
        resetVolumeToFull &&
        hadPendingPause &&
        !isAudioRouteMuteSuppressed() &&
        isPlayerInitialized()
    ) {
        runPlayerActionOnMainThread {
            if (isPlayerInitialized()) {
                player.volume = 1f
            }
        }
    }
}

private fun PlayerManager.isAudioRouteMuteSuppressed(): Boolean {
    return audioRouteMuteRestoreVolume?.let { it > 0f } == true
}

private fun PlayerManager.volumeWhileAudioRouteMuted(volume: Float): Float {
    return if (isAudioRouteMuteSuppressed()) 0f else volume
}

internal fun PlayerManager.clearAudioRouteMuteSuppression(reason: String) {
    clearAudioRouteMuteSuppression(
        reason = reason,
        preserveExplicitRestore = false
    )
}

internal fun PlayerManager.clearAudioRouteMuteSuppression(
    reason: String,
    preserveExplicitRestore: Boolean
) {
    if (
        preserveExplicitRestore &&
        shouldDeferAudioRouteMuteRestore(audioRouteMuteRequiresExplicitRestore)
    ) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "clearAudioRouteMuteSuppression(): keep explicit listener mute, reason=$reason, currentSong=${_currentSongFlow.value?.name}"
        )
        return
    }
    val suppressedVolume = audioRouteMuteRestoreVolume
    audioRouteMuteRestoreVolume = null
    audioRouteMuteRequiresExplicitRestore = false
    _audioRouteMuteSuppressedFlow.value = false
    if (suppressedVolume == null) return
    resetPlaybackRuntimeWatchdog(reason = "audio_route_mute_cleared")
    LyraLogger.d(
        "LYRA-PlayerManager",
        "clearAudioRouteMuteSuppression(): reason=$reason, suppressedVolume=$suppressedVolume, currentSong=${_currentSongFlow.value?.name}"
    )
}

internal fun shouldDeferAudioRouteMuteRestore(
    requiresExplicitRestore: Boolean
): Boolean = requiresExplicitRestore

internal fun resolveAudioRouteMuteRestoreVolume(
    currentVolume: Float,
    existingRestoreVolume: Float?
): Float? {
    return existingRestoreVolume?.takeIf { it > 0f }
        ?: currentVolume.coerceIn(0f, 1f).takeIf { it > 0f }
}

internal fun PlayerManager.suppressPlaybackForAudioRouteLoss(reason: String) {
    if (!isPlayerInitialized()) return
    val requiresExplicitRestore = false
    cancelVolumeFade(resetToFull = false)
    runPlayerActionOnMainThread {
        if (!isPlayerInitialized()) return@runPlayerActionOnMainThread
        val currentVolume = runCatching { player.volume.coerceIn(0f, 1f) }.getOrDefault(1f)
        val restoreVolume = resolveAudioRouteMuteRestoreVolume(
            currentVolume = currentVolume,
            existingRestoreVolume = audioRouteMuteRestoreVolume
        )
        if (restoreVolume == null) {
            audioRouteMuteRestoreVolume = null
            audioRouteMuteRequiresExplicitRestore = false
            _audioRouteMuteSuppressedFlow.value = false
            resetPlaybackRuntimeWatchdog(reason = "audio_route_mute_noop")
            return@runPlayerActionOnMainThread
        }
        audioRouteMuteRestoreVolume = restoreVolume
        audioRouteMuteRequiresExplicitRestore =
            audioRouteMuteRequiresExplicitRestore || requiresExplicitRestore
        _audioRouteMuteSuppressedFlow.value = true
        resetPlaybackRuntimeWatchdog(reason = "audio_route_mute_entered")
        player.volume = 0f
        LyraLogger.d(
            "LYRA-PlayerManager",
            "suppressPlaybackForAudioRouteLoss(): reason=$reason, capturedVolume=$restoreVolume, explicitRestore=${audioRouteMuteRequiresExplicitRestore}, currentSong=${_currentSongFlow.value?.name}"
        )
    }
}

internal fun PlayerManager.restoreAudioRouteMuteImpl() {
    val restoreVolume = audioRouteMuteRestoreVolume ?: run {
        audioRouteMuteRequiresExplicitRestore = false
        _audioRouteMuteSuppressedFlow.value = false
        return
    }
    audioRouteMuteRestoreVolume = null
    audioRouteMuteRequiresExplicitRestore = false
    _audioRouteMuteSuppressedFlow.value = false
    resetPlaybackRuntimeWatchdog(reason = "audio_route_mute_restored")
    if (!isPlayerInitialized()) return
    runPlayerActionOnMainThread {
        if (!isPlayerInitialized()) return@runPlayerActionOnMainThread
        player.volume = restoreVolume.coerceIn(0f, 1f)
        schedulePlaybackRuntimeWatchdog(reason = "audio_route_mute_restored")
        LyraLogger.d(
            "LYRA-PlayerManager",
            "restoreAudioRouteMuteImpl(): restoredVolume=$restoreVolume, currentSong=${_currentSongFlow.value?.name}"
        )
    }
}

internal fun PlayerManager.restorePlaybackAfterTransientAudioRouteLoss(reason: String) {
    if (shouldDeferAudioRouteMuteRestore(audioRouteMuteRequiresExplicitRestore)) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "restorePlaybackAfterTransientAudioRouteLoss(): keep listener muted until explicit restore, reason=$reason, currentSong=${_currentSongFlow.value?.name}"
        )
        return
    }
    val restoreVolume = audioRouteMuteRestoreVolume ?: run {
        _audioRouteMuteSuppressedFlow.value = false
        return
    }
    audioRouteMuteRestoreVolume = null
    _audioRouteMuteSuppressedFlow.value = false
    resetPlaybackRuntimeWatchdog(reason = "audio_route_mute_transient_restored")
    if (!isPlayerInitialized()) return
    val shouldRestore = runCatching {
        player.playWhenReady || player.isPlaying
    }.getOrDefault(false) || _isPlayingFlow.value || playJob?.isActive == true
    if (!shouldRestore) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "restorePlaybackAfterTransientAudioRouteLoss(): skipped restore for inactive playback, reason=$reason, currentSong=${_currentSongFlow.value?.name}"
        )
        return
    }
    runPlayerActionOnMainThread {
        if (!isPlayerInitialized()) return@runPlayerActionOnMainThread
        player.volume = restoreVolume.coerceIn(0f, 1f)
        schedulePlaybackRuntimeWatchdog(reason = "audio_route_mute_transient_restored")
        LyraLogger.d(
            "LYRA-PlayerManager",
            "restorePlaybackAfterTransientAudioRouteLoss(): reason=$reason, restoredVolume=$restoreVolume, currentSong=${_currentSongFlow.value?.name}"
        )
    }
}

internal fun PlayerManager.pauseForAudioRouteLoss(reason: String) {
    _playWhenReadyFlow.value = false
    _isPlayingFlow.value = false
    if (lyriconEnabled) {
        LyriconManager.setPlaybackState(false)
    }
    syncPlaybackControlPlayingState()
    pauseImpl(
        forcePersist = false,
        commandSource = PlaybackCommandSource.LOCAL,
        allowFadeOut = false,
        preserveMutedVolume = true,
        debugReason = "audio_route_loss:$reason",
        flushPlayerOutput = true,
    )
}

private fun PlayerManager.persistPausedPlaybackState(
    forcePersist: Boolean,
    positionMs: Long,
    shouldResumePlayback: Boolean,
    reason: String
) {
    if (!forcePersist) {
        scheduleStatePersist(
            positionMs = positionMs,
            shouldResumePlayback = shouldResumePlayback
        )
        return
    }
    ioScope.launch {
        try {
            runCatching { drainPlaybackStatsPersistJobBlocking(reason) }
                .onFailure { error ->
                    LyraLogger.w(
                        "LYRA-PlayerManager",
                        "pause persistence could not drain playback stats: reason=$reason",
                        error
                    )
                }
            persistStateNow(
                positionMs = positionMs,
                shouldResumePlayback = shouldResumePlayback,
                reason = reason
            )
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            LyraLogger.w(
                "LYRA-PlayerManager",
                "pause persistence failed: reason=$reason",
                error
            )
        }
    }
}

internal fun PlayerManager.preparePlayerForManagedStart(plan: PlaybackStartPlan) {
    if (!isPlayerInitialized()) return
    cancelVolumeFade()
    val effectivePlan = resolveEffectivePlaybackStartPlan(
        plan = plan,
        usbExclusivePlaybackEnabled = usbExclusivePlaybackEnabled
    )
    LyraLogger.d(
        "LYRA-PlayerManager",
        "preparePlayerForManagedStart: useFadeIn=${effectivePlan.useFadeIn}, fadeDurationMs=${effectivePlan.fadeDurationMs}, initialVolume=${effectivePlan.initialVolume}, currentSong=${_currentSongFlow.value?.name}"
    )
    player.playWhenReady = false
    player.volume = volumeWhileAudioRouteMuted(effectivePlan.initialVolume)
}

internal suspend fun PlayerManager.fadeOutCurrentPlaybackIfNeeded(
    enabled: Boolean,
    fadeOutDurationMs: Long = playbackCrossfadeOutDurationMs
) {
    if (!enabled || !isPlayerInitialized()) {
        return
    }

    val shouldFade = _isPlayingFlow.value
    if (!shouldFade) {
        return
    }

    val durationMs = fadeOutDurationMs.coerceAtLeast(0L)
    if (durationMs <= 0L) {
        return
    }

    cancelVolumeFade()
    val startVolume = withContext(Dispatchers.Main) { player.volume.coerceIn(0f, 1f) }
    if (startVolume <= 0f) {
        return
    }
    LyraLogger.d(
        "LYRA-PlayerManager",
        "fadeOutCurrentPlaybackIfNeeded: durationMs=$durationMs, startVolume=$startVolume, currentSong=${_currentSongFlow.value?.name}"
    )

    val steps = fadeStepsFor(durationMs)
    if (steps <= 0) return
    val stepDelay = (durationMs / steps).coerceAtLeast(1L)
    repeat(steps) { step ->
        val fraction = (step + 1).toFloat() / steps
        withContext(Dispatchers.Main) {
            if (!isPlayerInitialized()) {
                return@withContext
            }
            player.volume = (startVolume * (1f - fraction)).coerceAtLeast(0f)
        }
        delay(stepDelay)
    }

    withContext(Dispatchers.Main) {
        if (isPlayerInitialized()) {
            player.volume = 0f
        }
    }
    LyraLogger.d(
        "LYRA-PlayerManager",
        "fadeOutCurrentPlaybackIfNeeded completed: durationMs=$durationMs, currentSong=${_currentSongFlow.value?.name}"
    )
}

internal fun PlayerManager.startPlayerPlaybackWithFade(plan: PlaybackStartPlan) {
    cancelVolumeFade()
    StartupAudioFocusController.release("playback_start")
    val effectivePlan = resolveEffectivePlaybackStartPlan(
        plan = plan,
        usbExclusivePlaybackEnabled = usbExclusivePlaybackEnabled
    )
    LyraLogger.d(
        "LYRA-PlayerManager",
        "startPlayerPlaybackWithFade: useFadeIn=${effectivePlan.useFadeIn}, fadeDurationMs=${effectivePlan.fadeDurationMs}, initialVolume=${effectivePlan.initialVolume}, currentSong=${_currentSongFlow.value?.name}"
    )
    runPlayerActionOnMainThread {
        if (!isPlayerInitialized()) return@runPlayerActionOnMainThread
        if (usbExclusivePlaybackEnabled && !isUsbExclusiveNativePlaybackStable()) {
            markUsbExclusivePlaybackPreparing(true, "playback_start")
        }
        if (!prepareUsbExclusiveRouteForManualPlayback("playback_start")) {
            return@runPlayerActionOnMainThread
        }
        applyAudioFocusPolicyOnMainThread()
        player.volume = volumeWhileAudioRouteMuted(effectivePlan.initialVolume)
        player.playWhenReady = true
        player.play()
    }
    if (!effectivePlan.useFadeIn || isAudioRouteMuteSuppressed()) {
        return
    }

    val steps = fadeStepsFor(effectivePlan.fadeDurationMs)
    if (steps <= 0) return
    val stepDelay = (effectivePlan.fadeDurationMs / steps).coerceAtLeast(1L)
    volumeFadeJob = mainScope.launch {
        repeat(steps) { step ->
            delay(stepDelay)
            if (!isPlayerInitialized()) return@launch
            player.volume = volumeWhileAudioRouteMuted(
                ((step + 1).toFloat() / steps).coerceAtMost(1f)
            )
        }
        if (isPlayerInitialized()) {
            player.volume = volumeWhileAudioRouteMuted(1f)
        }
        volumeFadeJob = null
    }
}

internal fun PlayerManager.resolveCurrentPlaybackStartPlan(
    useTrackTransitionFade: Boolean = false,
    useUsbTransitionProtection: Boolean = false,
    forceStartupProtectionFade: Boolean = false
): PlaybackStartPlan {
    return resolveManagedPlaybackStartPlan(
        playbackFadeInEnabled = playbackFadeInEnabled,
        playbackFadeInDurationMs = playbackFadeInDurationMs,
        playbackCrossfadeInDurationMs = playbackCrossfadeInDurationMs,
        useTrackTransitionFade = useTrackTransitionFade,
        useUsbTransitionProtection = useUsbTransitionProtection,
        forceStartupProtectionFade = forceStartupProtectionFade
    )
}

internal fun PlayerManager.handleTrackEnded() {
    clearPendingSeekPosition()
    val finishedSong = _currentSongFlow.value
    val finishedDurationMs = maxOf(
        finishedSong?.durationMs?.coerceAtLeast(0L) ?: 0L,
        _playbackDurationMs.value
    )
    persistLongFormPlaybackProgress(
        song = finishedSong,
        positionMs = finishedDurationMs,
        durationMs = finishedDurationMs
    )
    _playbackPositionMs.value = 0L
    val isLastInPlaylist = currentIndex >= currentPlaylist.lastIndex
    LyraLogger.d(
        "LYRA-PlayerManager",
        "handleTrackEnded: currentIndex=$currentIndex, queueSize=${currentPlaylist.size}, repeatMode=$repeatModeSetting, shuffle=${player.shuffleModeEnabled}, isLastInPlaylist=$isLastInPlaylist"
    )


    if (sleepTimerManager.shouldStopOnTrackEnd(isLastInPlaylist)) {
        pause()
        sleepTimerManager.cancel()
        return
    }

    when (repeatModeSetting) {
        Player.REPEAT_MODE_ONE -> {
            markAutoTrackAdvance()
            playAtIndex(
                index = currentIndex,
                commandSource = activePlaybackCommandSource,
                allowRememberedLongFormPosition = false
            )
        }
        Player.REPEAT_MODE_ALL -> {
            markAutoTrackAdvance()
            nextImpl(
                force = true,
                commandSource = activePlaybackCommandSource,
                bypassLoudVolumeWarning = true
            )
        }
        else -> {
            if (currentIndex < currentPlaylist.lastIndex) {
                markAutoTrackAdvance()
                nextImpl(
                    force = false,
                    commandSource = activePlaybackCommandSource,
                    bypassLoudVolumeWarning = true
                )
            } else {
                stopPlaybackPreservingQueue()
            }
        }
    }
}

internal fun PlayerManager.advanceAfterPlaybackFailure(
    source: String,
    commandSource: PlaybackCommandSource = activePlaybackCommandSource
) {
    clearPendingSeekPosition()
    persistCurrentLongFormPlaybackProgress()
    _playbackPositionMs.value = 0L

    val action = resolvePlaybackFailureAdvanceAction(
        currentIndex = currentIndex,
        playlistSize = currentPlaylist.size,
        repeatMode = repeatModeSetting
    )
    LyraLogger.d(
        "LYRA-PlayerManager",
        "advanceAfterPlaybackFailure: source=$source, action=$action, currentIndex=$currentIndex, queueSize=${currentPlaylist.size}, repeatMode=$repeatModeSetting, shuffle=${player.shuffleModeEnabled}"
    )

    when (action) {
        PlaybackFailureAdvanceAction.NEXT -> {
            markAutoTrackAdvance()
            nextImpl(
                force = false,
                commandSource = commandSource,
                bypassLoudVolumeWarning = true
            )
        }
        PlaybackFailureAdvanceAction.WRAP -> {
            markAutoTrackAdvance()
            nextImpl(
                force = true,
                commandSource = commandSource,
                bypassLoudVolumeWarning = true
            )
        }
        PlaybackFailureAdvanceAction.STOP -> {
            stopPlaybackPreservingQueue(clearMediaUrl = true)
        }
    }
}

internal fun PlayerManager.playPlaylistImpl(
    songs: List<SongItem>,
    startIndex: Int,
    commandSource: PlaybackCommandSource = PlaybackCommandSource.LOCAL,
    bypassLoudVolumeWarning: Boolean = false,
    localPlaylistId: Long? = null
) {
    ensureInitialized()
    check(initialized) { "Call PlayerManager.initialize(application) first." }
    if (songs.isEmpty()) {
        LyraLogger.w("LYRA-Player", "playPlaylist called with EMPTY list")
        return
    }
    val targetSong = songs.getOrNull(startIndex.coerceIn(0, songs.lastIndex)) ?: songs.first()
    if (shouldBlockLocalRoomControl(commandSource) ||
        shouldBlockLocalSongSwitch(targetSong, commandSource)
    ) {
        return
    }
    if (requestUsbExclusiveLoudPlaybackConfirmation(
            commandSource = commandSource,
            bypassWarning = bypassLoudVolumeWarning,
            continuePlayback = {
                playPlaylistImpl(
                    songs = songs,
                    startIndex = startIndex,
                    commandSource = commandSource,
                    bypassLoudVolumeWarning = true,
                    localPlaylistId = localPlaylistId
                )
            }
        )
    ) {
        return
    }
    LyraLogger.d(
        "LYRA-PlayerManager",
        "playPlaylist: size=${songs.size}, requestedStart=$startIndex, resolvedStart=${startIndex.coerceIn(0, songs.lastIndex)}, source=$commandSource, target=${targetSong.name}, stack=[${debugStackHint()}]"
    )
    suppressAutoResumeForCurrentSession = false
    consecutivePlayFailures = 0
    localPlaylistPlaybackSource = localPlaylistId?.let { playlistId ->
        LocalPlaylistPlaybackSource(
            playlistId = playlistId,
            songKeys = songs.mapTo(LinkedHashSet(songs.size)) { song -> song.stableKey() }
        )
    }
    currentPlaylist = songs
    _currentQueueFlow.value = currentPlaylist
    currentIndex = startIndex.coerceIn(0, songs.lastIndex)

    if (player.shuffleModeEnabled && commandSource != PlaybackCommandSource.REMOTE_SYNC) {
        rememberShuffleRestoreQueueSnapshot()
        shuffleCurrentQueueForSequentialPlayback()
    } else {
        clearShuffleRestoreQueueSnapshot()
    }

    playAtIndex(currentIndex, commandSource = commandSource)
    emitPlaybackCommand(
        type = "PLAY_PLAYLIST",
        source = commandSource,
        queue = currentPlaylist.toList(),
        currentIndex = currentIndex,
        positionMs = _playbackPositionMs.value
    )
    scheduleStatePersist()
}

private fun PlayerManager.rememberShuffleRestoreQueueSnapshot() {
    if (currentPlaylist.isEmpty()) {
        clearShuffleRestoreQueueSnapshot()
        return
    }
    shuffleRestorePlaylistReference = currentPlaylist.toList()
    shuffleRestoreCurrentIndex = currentIndex.coerceIn(currentPlaylist.indices)
}

private fun PlayerManager.clearShuffleRestoreQueueSnapshot() {
    shuffleRestorePlaylistReference = null
    shuffleRestoreCurrentIndex = -1
}

internal fun PlayerManager.syncShuffleRestoreQueueSnapshotAfterAdd(
    song: SongItem,
    afterCurrent: Boolean
) {
    val restorePlaylist = shuffleRestorePlaylistReference ?: return
    shuffleRestorePlaylistReference = insertIntoShuffleRestorePlaylist(
        restorePlaylist = restorePlaylist,
        song = song,
        currentSong = _currentSongFlow.value,
        afterCurrent = afterCurrent
    )
    refreshShuffleRestoreCurrentIndex()
}

internal fun PlayerManager.syncShuffleRestoreQueueSnapshotAfterRemove(song: SongItem) {
    val restorePlaylist = shuffleRestorePlaylistReference ?: return
    val updated = removeFromShuffleRestorePlaylist(restorePlaylist, song)
    if (updated.isEmpty()) {
        clearShuffleRestoreQueueSnapshot()
        return
    }
    shuffleRestorePlaylistReference = updated
    refreshShuffleRestoreCurrentIndex()
}

internal fun PlayerManager.syncShuffleRestoreQueueSnapshotAfterReplace(
    replacedSong: SongItem?,
    song: SongItem
) {
    val restorePlaylist = shuffleRestorePlaylistReference ?: return
    shuffleRestorePlaylistReference = replaceInShuffleRestorePlaylist(
        restorePlaylist = restorePlaylist,
        replacedSong = replacedSong,
        song = song
    )
    refreshShuffleRestoreCurrentIndex()
}

internal fun PlayerManager.invalidateShuffleRestoreQueueSnapshot(reason: String) {
    if (shuffleRestorePlaylistReference == null) return
    LyraLogger.d(
        "LYRA-PlayerManager",
        "invalidateShuffleRestoreQueueSnapshot: reason=$reason, restoreSize=${shuffleRestorePlaylistReference?.size}"
    )
    clearShuffleRestoreQueueSnapshot()
}

private fun PlayerManager.refreshShuffleRestoreCurrentIndex() {
    val restorePlaylist = shuffleRestorePlaylistReference ?: return
    val currentSong = _currentSongFlow.value
    shuffleRestoreCurrentIndex = currentSong
        ?.let { song -> restorePlaylist.indexOfFirst { it.sameIdentityAs(song) } }
        ?.takeIf { it >= 0 }
        ?: shuffleRestoreCurrentIndex.coerceIn(0, restorePlaylist.lastIndex)
}

private fun PlayerManager.restoreShuffleRestoreQueueSnapshot(): Boolean {
    val currentSong = _currentSongFlow.value
    val restoreOrder = resolvePlayerQueueRestoreOrder(
        restorePlaylist = shuffleRestorePlaylistReference,
        currentSong = currentSong,
        fallbackIndex = shuffleRestoreCurrentIndex
    ) ?: run {
        clearShuffleRestoreQueueSnapshot()
        return false
    }

    val restoredPlaylist = restoreOrder.playlist.toMutableList()
    if (currentSong != null && restoreOrder.currentIndex in restoredPlaylist.indices) {
        restoredPlaylist[restoreOrder.currentIndex] = currentSong
    }
    currentPlaylist = restoredPlaylist
    currentIndex = restoreOrder.currentIndex
    _currentQueueFlow.value = currentPlaylist
    setCurrentSongForPlayback(currentPlaylist.getOrNull(currentIndex))
    bumpCurrentQueueDisplayRevision()
    clearShuffleRestoreQueueSnapshot()
    return true
}

internal fun PlayerManager.shuffleCurrentQueueForSequentialPlayback(): Boolean {
    val order = resolvePlayerSequentialShuffleOrder(
        queueSize = currentPlaylist.size,
        currentIndex = currentIndex
    )
    if (order.queueIndices.isEmpty()) {
        currentIndex = -1
        return false
    }

    val shuffledPlaylist = order.queueIndices.map { index -> currentPlaylist[index] }
    val changed = shuffledPlaylist != currentPlaylist || currentIndex != order.currentIndex
    currentPlaylist = shuffledPlaylist
    currentIndex = order.currentIndex
    if (changed) {
        _currentQueueFlow.value = currentPlaylist
        setCurrentSongForPlayback(currentPlaylist.getOrNull(currentIndex))
        bumpCurrentQueueDisplayRevision()
    }
    LyraLogger.d(
        "LYRA-PlayerManager",
        "shuffleCurrentQueueForSequentialPlayback: queueSize=${currentPlaylist.size}, currentIndex=$currentIndex, changed=$changed"
    )
    return changed
}

private fun PlayerManager.reshuffleCurrentQueueForRepeatAllCycle(): Boolean {
    if (
        !player.shuffleModeEnabled ||
        repeatModeSetting != Player.REPEAT_MODE_ALL ||
        currentPlaylist.size <= 1 ||
        currentIndex != currentPlaylist.lastIndex
    ) {
        return false
    }
    val order = resolvePlayerRepeatAllShuffleOrder(
        queueSize = currentPlaylist.size,
        completedIndex = currentIndex
    )
    currentPlaylist = order.queueIndices.map { index -> currentPlaylist[index] }
    currentIndex = order.currentIndex
    _currentQueueFlow.value = currentPlaylist
    bumpCurrentQueueDisplayRevision()
    LyraLogger.d(
        "LYRA-PlayerManager",
        "reshuffleCurrentQueueForRepeatAllCycle: queueSize=${currentPlaylist.size}, currentIndex=$currentIndex"
    )
    return true
}

internal fun PlayerManager.playAtIndex(
    index: Int,
    resumePositionMs: Long = 0L,
    useTrackTransitionFade: Boolean = false,
    commandSource: PlaybackCommandSource = PlaybackCommandSource.LOCAL,
    forceStartupProtectionFade: Boolean = false,
    startPlanOverride: PlaybackStartPlan? = null,
    allowRememberedLongFormPosition: Boolean =
        commandSource == PlaybackCommandSource.LOCAL
) {
    if (currentPlaylist.isEmpty() || index !in currentPlaylist.indices) {
        LyraLogger.w("LYRA-Player", "playAtIndex called with invalid index: $index")
        return
    }

    if (consecutivePlayFailures >= MAX_CONSECUTIVE_FAILURES) {
        LyraLogger.e(
            "LYRA-PlayerManager",
            "Too many consecutive playback failures: $consecutivePlayFailures"
        )
        mainScope.launch {
            AppFeedback.showToast(
                context = application,
                message = getLocalizedString(R.string.toast_playback_stopped)
            )
        }
        stopPlaybackPreservingQueue(clearMediaUrl = true)
        return
    }

    val song = currentPlaylist[index]
    var keepTransitionWakeLockUntilPlaybackProgress = false
    val resolvedResumePositionMs = resolveRememberedLongFormPlaybackStartPosition(
        song = song,
        requestedPositionMs = resumePositionMs,
        allowRememberedPosition = allowRememberedLongFormPosition
    )
    val useUsbTransitionProtection = usbExclusivePlaybackEnabled &&
        (player.isPlaying || player.playWhenReady)
    LyraLogger.d(
        "LYRA-PlayerManager",
            "playAtIndex: index=$index, song=${song.name}, resumePositionMs=$resolvedResumePositionMs, " +
            "transitionFade=$useTrackTransitionFade, usbTransitionProtection=" +
            "$useUsbTransitionProtection, source=$commandSource, " +
            "forceStartupProtectionFade=$forceStartupProtectionFade, " +
            "nextToken=${playbackRequestToken + 1}, stack=[${debugStackHint()}]"
    )
    replacePlaybackDemandCacheKey(
        cacheKey = song
            .takeUnless { isLocalSong(it) || isDirectStreamUrl(it.streamUrl) }
            ?.let(::computeCacheKey),
        reason = "play_at_index_request"
    )
    kickoffYouTubePlaybackIntentWarmup(song, source = "play_at_index")
    cancelPendingPauseRequest()
    val previousSong = _currentSongFlow.value
    val retainCurrentAudioInfo = commandSource == PlaybackCommandSource.REMOTE_SYNC &&
        previousSong?.sameIdentityAs(song) == true
    setCurrentSongForPlayback(song, syncLyricon = false)
    _currentMediaUrl.value = null
    if (!retainCurrentAudioInfo) {
        _currentPlaybackAudioInfo.value = null
    }
    currentMediaUrlResolvedAtMs = 0L
    updateResumePlaybackRequested(true)
    clearUsbExclusiveInterruptedPlaybackIntent("play_at_index")
    restoredShouldResumePlayback = false
    restoredResumePositionMs = 0L
    scheduleStatePersist(
        positionMs = resolvedResumePositionMs,
        shouldResumePlayback = true
    )
    bumpCurrentQueueDisplayRevision()

    playJob?.cancel()
    cancelYouTubePrefetchForPlaybackDemand(song, reason = "play_at_index")
    cancelGenericUrlPrefetchUnlessReusableForSong(song, reason = "play_at_index")
    playbackRequestToken += 1
    val requestToken = playbackRequestToken
    BiliSponsorBlockPlaybackController.onPlaybackRequestStarted(song, requestToken)
    BiliVideoSkipPlaybackController.onPlaybackRequestStarted(song, requestToken)
    if (isBiliTrack(song)) {
        BiliVideoSkipPlaybackController.prepareActiveBiliTrackTarget(
            song = song,
            requestToken = requestToken,
            scope = ioScope
        )
    }
    PlaybackTransitionWakeLock.acquire(
        context = application,
        requestToken = requestToken,
        reason = "play_at_index"
    )
    maybeHydrateSongForPlayback(index, song, requestToken)
    cancelUrlRefreshIfNotReusableForPendingLoad(
        song = song,
        resumePositionMs = resolvedResumePositionMs,
        requestGeneration = requestToken,
        commandSource = commandSource
    )
    clearPendingSeekPosition()
    enterPendingMediaLoad(resolvedResumePositionMs)
    playJob = ioScope.launch {
        try {
        val result = resolveSongUrl(
            song = song,
            playbackRequestTokenOverride = requestToken,
            shouldApplyCacheMutation = {
                shouldApplyResolvedMedia(requestToken, playbackRequestToken) && isActive
            }
        )
        if (!shouldApplyResolvedMedia(requestToken, playbackRequestToken) || !isActive) {
            LyraLogger.d(
                "LYRA-PlayerManager",
                "播放请求已过期，跳过本次 URL 解析结果: song=${song.name}, requestToken=$requestToken, currentToken=$playbackRequestToken, active=$isActive"
            )
            return@launch
        }

        when (result) {
            is SongUrlResult.Success -> {
                if (!shouldApplyResolvedMedia(requestToken, playbackRequestToken) || !isActive) {
                    LyraLogger.d(
                        "LYRA-PlayerManager",
                        "播放请求已过期，跳过媒体项装载: song=${song.name}, requestToken=$requestToken, currentToken=$playbackRequestToken, active=$isActive"
                    )
                    return@launch
                }

                fadeOutCurrentPlaybackIfNeeded(
                    enabled = useTrackTransitionFade || useUsbTransitionProtection,
                    fadeOutDurationMs = if (useUsbTransitionProtection) {
                        USB_TRACK_TRANSITION_PROTECTION_FADE_DURATION_MS
                    } else {
                        playbackCrossfadeOutDurationMs
                    }
                )
                if (!shouldApplyResolvedMedia(requestToken, playbackRequestToken) || !isActive) {
                    return@launch
                }

                var appliedResolvedMedia = false
                withContext(Dispatchers.Main) {
                    if (!shouldApplyResolvedMediaSideEffects(
                            requestGeneration = requestToken,
                            currentRequestGeneration = playbackRequestToken,
                            requestActive = true
                        )
                    ) {
                        return@withContext
                    }
                    consecutivePlayFailures = 0
                    result.noticeMessage?.let { message ->
                        postPlayerEvent(PlayerEvent.ShowError(message))
                    }
                    maybeUpdateSongDuration(song, result.durationMs ?: 0L)
                    val cacheKey = result.cacheKeyOverride ?: computeCacheKey(song)
                    replacePlaybackDemandCacheKey(
                        cacheKey = cacheKey.takeUnless {
                            isLocalSong(song) || isDirectStreamUrl(song.streamUrl)
                        },
                        reason = "play_at_index_resolved"
                    )
                    configureActivePlaybackCandidates(
                        result,
                        resolvedResumePositionMs,
                        commandSource
                    )
                    val selectedCandidate = currentPlaybackCandidate()
                    val selectedUrl = selectedCandidate?.url ?: result.url
                    val selectedAudioInfo = selectedCandidate?.audioInfo ?: result.audioInfo
                    val selectedMimeType = selectedCandidate?.mimeType ?: result.mimeType
                    val selectedExpectedContentLength =
                        selectedCandidate?.expectedContentLength ?: result.expectedContentLength
                    val selectedRepresentationIdentity =
                        selectedCandidate?.representationIdentity ?: result.representationIdentity
                    LyraLogger.d(
                        "LYRA-PlayerManager",
                        "Using custom cache key: $cacheKey for song: ${song.name}"
                    )
                    val cacheSynchronization = synchronizeCachedPlaybackDescriptor(
                        cacheKey = cacheKey,
                        audioInfo = selectedAudioInfo,
                        expectedContentLength = selectedExpectedContentLength,
                        representationIdentity = selectedRepresentationIdentity,
                        shouldApplyMutation = {
                            shouldApplyResolvedMediaSideEffects(
                                requestGeneration = requestToken,
                                currentRequestGeneration = playbackRequestToken,
                                requestActive = true
                            )
                        }
                    )
                    if (!shouldApplyResolvedMediaSideEffects(
                            requestGeneration = requestToken,
                            currentRequestGeneration = playbackRequestToken,
                            requestActive = isActive
                        )
                    ) {
                        return@withContext
                    }
                    val mediaItem = buildMediaItem(
                        _currentSongFlow.value ?: song,
                        selectedUrl,
                        cacheKey,
                        selectedMimeType,
                        allowCustomCacheKey = cacheSynchronization.allowsCustomCacheKey()
                    )
                    syncLyriconSong(_currentSongFlow.value ?: song)
                    _currentMediaUrl.value = selectedUrl
                    _currentPlaybackAudioInfo.value = selectedAudioInfo
                    updateAudioOffloadPreferences("resolved_stream_source")
                    currentMediaUrlResolvedAtMs = SystemClock.elapsedRealtime()
                    scheduleStatePersist(
                        positionMs = resolvedResumePositionMs,
                        shouldResumePlayback = true
                    )
                    val startPlan = startPlanOverride ?: resolveCurrentPlaybackStartPlan(
                        useTrackTransitionFade = useTrackTransitionFade,
                        useUsbTransitionProtection = useUsbTransitionProtection,
                        forceStartupProtectionFade = forceStartupProtectionFade &&
                            resolvedResumePositionMs > 0L
                    )
                    preparePlayerForManagedStart(startPlan)
                    resetTrackEndDeduplicationState()
                    applyWakeModeForPlaybackUrl(selectedUrl)
                    player.setMediaItem(mediaItem)
                    loadedMediaRequestToken = requestToken
                    pendingMediaLoadActive = false
                    syncExoRepeatMode()
                    val startPositionMs = pendingSeekPositionOrNull()
                        ?: resolvedResumePositionMs
                    if (startPositionMs > 0L) {
                        player.seekTo(startPositionMs)
                        _playbackPositionMs.value = startPositionMs
                    }
                    resetPlaybackProgressAdvanceBaseline(startPositionMs)
                    clearPendingSeekPosition()
                    player.prepare()
                    if (resumePlaybackRequested) {
                        startPlayerPlaybackWithFade(startPlan)
                        startProgressUpdates()
                        schedulePlaybackStartupWatchdog(reason = "media_resolved")
                        keepTransitionWakeLockUntilPlaybackProgress = true
                    } else {
                        player.playWhenReady = false
                        player.pause()
                    }
                    appliedResolvedMedia = true
                }
                if (!appliedResolvedMedia) {
                    return@launch
                }
                maybeWarmNextYouTubeMusicAfterCurrentResolved()
            }
            is SongUrlResult.RequiresLogin -> {
                clearPlaybackDemandCacheKey(reason = "play_at_index_requires_login")
                LyraLogger.w(
                    "LYRA-PlayerManager",
                    "Requires login to play: id=${song.id}, source=${song.album}"
                )
                postPlayerEvent(
                    PlayerEvent.ShowLoginPrompt(
                        getLocalizedString(R.string.player_playback_login_required)
                    )
                )
                withContext(Dispatchers.Main) {
                    nextImpl(
                        commandSource = commandSource,
                        bypassLoudVolumeWarning = true
                    )
                }
            }
            is SongUrlResult.Failure -> {
                clearPlaybackDemandCacheKey(reason = "play_at_index_failure")
                LyraLogger.e(
                    "LYRA-PlayerManager",
                    "获取播放地址失败，跳过当前歌曲: id=${song.id}, source=${song.album}"
                )
                consecutivePlayFailures++
                withContext(Dispatchers.Main) {
                    advanceAfterPlaybackFailure(
                        source = "resolve_song_url_failure",
                        commandSource = commandSource
                    )
                }
            }
        }
        } finally {
            if (
                !keepTransitionWakeLockUntilPlaybackProgress ||
                requestToken != playbackRequestToken ||
                !resumePlaybackRequested
            ) {
                PlaybackTransitionWakeLock.release(requestToken, "play_request_finished")
            } else {
                LyraLogger.d(
                    "LYRA-PlaybackWakeLock",
                    "keep transition wake lock until position advances: token=$requestToken"
                )
            }
        }
    }
}

private fun PlayerManager.maybeHydrateSongForPlayback(
    index: Int,
    song: SongItem,
    requestToken: Long
) {
    if (isYouTubeMusicTrack(song) && song.coverUrl.isNullOrBlank() && song.customCoverUrl.isNullOrBlank()) {
        val videoId = extractYouTubeMusicVideoId(song.mediaUri).orEmpty()
        if (videoId.isNotBlank()) {
            val thumbnailUrl = youtubeMusicThumbnailUrl(videoId)
            hydrateSongMetadata(
                originalSong = song,
                updatedSong = song.copy(
                    coverUrl = thumbnailUrl,
                    originalCoverUrl = song.originalCoverUrl ?: thumbnailUrl
                )
            )
        }
        return
    }
    if (!isLocalSong(song)) {
        return
    }

    ioScope.launch {
        val hydratedSong = LocalAudioImportManager.hydrateLocalSongMetadata(application, song)
        if (hydratedSong == song) {
            return@launch
        }

        var applied = false
        withContext(Dispatchers.Main) {
            if (requestToken != playbackRequestToken) {
                return@withContext
            }
            if (index !in currentPlaylist.indices || !currentPlaylist[index].sameIdentityAs(song)) {
                return@withContext
            }

            val updatedPlaylist = currentPlaylist.toMutableList()
            updatedPlaylist[index] = hydratedSong
            currentPlaylist = updatedPlaylist
            _currentQueueFlow.value = updatedPlaylist
            if (_currentSongFlow.value?.sameIdentityAs(song) == true) {
                setCurrentSongForPlayback(hydratedSong, syncLyricon = false)
            }
            applied = true
        }

        if (!applied) {
            return@launch
        }

        runLocalPlaylistMutationSafely("hydratePlaybackSongMetadata") {
            withContext(Dispatchers.IO) {
                localRepo.updateSongMetadata(song, hydratedSong)
            }
        }
        scheduleStatePersist()
    }
}

internal fun PlayerManager.enterPendingMediaLoad(requestedPositionMs: Long) {
    val action = resolvePendingMediaLoadEntryAction(requestedPositionMs)
    cancelPlaybackStartupWatchdog(reason = "pending_media_load")
    resetPlaybackRuntimeWatchdog(reason = "pending_media_load")
    clearActivePlaybackCandidates()
    pendingMediaLoadActive = true
    pendingMediaLoadPositionMs = action.positionMs
    if (action.stopProgressUpdates) stopProgressUpdates()
    cancelVolumeFade(resetToFull = true)
    if (action.stopPlayer) runCatching { player.stop() }
    if (action.clearMediaItems) runCatching { player.clearMediaItems() }
    _isPlayingFlow.value = action.isPlaying
    _playWhenReadyFlow.value = action.playWhenReady
    _playerPlaybackStateFlow.value = action.playbackState
    _playbackPositionMs.value = action.positionMs
}

private fun PlayerManager.maybeWarmNextYouTubeMusicAfterCurrentResolved() {
    val currentSong = _currentSongFlow.value ?: return
    if (!isYouTubeMusicTrack(currentSong) || currentMediaUrlResolvedAtMs <= 0) {
        return
    }
    val nextStartIndex = currentIndex + 1
    if (nextStartIndex !in currentPlaylist.indices) {
        return
    }
    prefetchYouTubeQueueWindow(
        playlist = currentPlaylist,
        startIndex = nextStartIndex,
        source = "after_current_resolved"
    )
}

internal fun PlayerManager.playBiliVideoPartsImpl(
    videoInfo: BiliClient.VideoBasicInfo,
    startIndex: Int,
    coverUrl: String
) {
    ensureInitialized()
    check(initialized) { "Call PlayerManager.initialize(application) first." }
    val songs = videoInfo.pages.map { page -> buildBiliPartSong(page, videoInfo, coverUrl) }
    LyraLogger.d(
        "LYRA-PlayerManager",
        "playBiliVideoParts: bvid=${videoInfo.bvid}, pages=${songs.size}, requestedStart=$startIndex, title=${videoInfo.title}"
    )
    playPlaylist(songs, startIndex)
}

internal fun PlayerManager.playImpl(
    commandSource: PlaybackCommandSource = PlaybackCommandSource.LOCAL,
    bypassLoudVolumeWarning: Boolean = false,
    allowFadeIn: Boolean = true
) {
    ensureInitialized()
    if (!initialized) return
    if (commandSource == PlaybackCommandSource.LOCAL && shouldBlockLocalRoomControl(commandSource)) return
    if (requestUsbExclusiveLoudPlaybackConfirmation(
            commandSource = commandSource,
            bypassWarning = bypassLoudVolumeWarning,
            continuePlayback = {
                playImpl(
                    commandSource = commandSource,
                    bypassLoudVolumeWarning = true,
                    allowFadeIn = allowFadeIn
                )
            }
        )
    ) {
        return
    }
    if (isPendingMediaLoadActive() && playJob?.isActive == true) {
        val action = resolvePendingPlayAction(pendingLoadActive = true)
        cancelPendingPauseRequest(resetVolumeToFull = true)
        suppressAutoResumeForCurrentSession = false
        updateResumePlaybackRequested(action.resumePlaybackRequested)
        scheduleStatePersist(
            positionMs = _playbackPositionMs.value,
            shouldResumePlayback = true
        )
        emitPlaybackCommand(
            type = "PLAY",
            source = commandSource,
            positionMs = _playbackPositionMs.value,
            currentIndex = currentIndex
        )
        return
    }
    val resumeVolumeFromPendingPause = if (
        pendingPauseJob?.isActive == true &&
        isPlayerInitialized()
    ) {
        runCatching { player.volume.coerceIn(0f, 1f) }.getOrNull()
    } else {
        null
    }
    cancelPendingPauseRequest(resetVolumeToFull = resumeVolumeFromPendingPause == null)
    suppressAutoResumeForCurrentSession = false
    updateResumePlaybackRequested(true)
    if (!usbExclusivePlaybackEnabled) {
        clearUsbExclusiveInterruptedPlaybackIntent("manual_play")
    }
    val song = _currentSongFlow.value
    val preparedInPlayer = isPreparedInPlayer()
    LyraLogger.d(
        "LYRA-PlayerManager",
        "play requested: source=$commandSource, prepared=$preparedInPlayer, queueSize=${currentPlaylist.size}, currentIndex=$currentIndex, song=${song?.name}, stack=[${debugStackHint()}]"
    )
    if (preparedInPlayer && song != null && !isLocalSong(song)) {
        val url = _currentMediaUrl.value
        if (!url.isNullOrBlank()) {
            val ageMs = if (currentMediaUrlResolvedAtMs > 0L) {
                SystemClock.elapsedRealtime() - currentMediaUrlResolvedAtMs
            } else {
                Long.MAX_VALUE
            }
            if (
                ageMs >= MEDIA_URL_STALE_MS ||
                YouTubeSeekRefreshPolicy.shouldRefreshUrlBeforeResume(song, url)
            ) {
                refreshCurrentSongUrl(
                    resumePositionMs = player.currentPosition,
                    allowFallback = false,
                    reason = "stale_resume",
                    bypassCooldown = true,
                    resumedPlaybackCommandSource = commandSource
                )
                return
            }
        }
    }
    when {
        preparedInPlayer -> {
            syncExoRepeatMode()
            startPlayerPlaybackWithFade(
                if (allowFadeIn) {
                    resolvePlaybackContinuationStartPlan(
                        plan = resolveCurrentPlaybackStartPlan(),
                        currentVolume = resumeVolumeFromPendingPause
                    )
                } else {
                    resolveNoFadePlaybackStartPlan()
                }
            )
            val resumePositionMs = player.currentPosition.coerceAtLeast(0L)
            _playbackPositionMs.value = resumePositionMs
            resetPlaybackProgressAdvanceBaseline(resumePositionMs)
            startProgressUpdates()
            schedulePlaybackStartupWatchdog(reason = "manual_resume_prepared")
            scheduleStatePersist(
                positionMs = resumePositionMs,
                shouldResumePlayback = true
            )
            emitPlaybackCommand(
                type = "PLAY",
                source = commandSource,
                positionMs = resumePositionMs,
                currentIndex = currentIndex
            )
        }
        currentPlaylist.isNotEmpty() && currentIndex != -1 -> {
            val manualResumeDecision = resolveManualResumePlaybackDecision(
                keepLastPlaybackProgressEnabled = keepLastPlaybackProgressEnabled,
                restoredResumePositionMs = restoredResumePositionMs,
                persistedPlaybackPositionMs = _playbackPositionMs.value,
                isPlayerPrepared = preparedInPlayer,
                currentMediaUrlResolvedAtMs = currentMediaUrlResolvedAtMs
            )
            playAtIndex(
                currentIndex,
                resumePositionMs = manualResumeDecision.resumePositionMs,
                commandSource = commandSource,
                forceStartupProtectionFade = manualResumeDecision.forceStartupProtectionFade,
                startPlanOverride = if (allowFadeIn) null else resolveNoFadePlaybackStartPlan()
            )
            emitPlaybackCommand(
                type = "PLAY",
                source = commandSource,
                positionMs = manualResumeDecision.resumePositionMs,
                currentIndex = currentIndex
            )
        }
        currentPlaylist.isNotEmpty() -> {
            playAtIndex(
                index = 0,
                commandSource = commandSource,
                startPlanOverride = if (allowFadeIn) null else resolveNoFadePlaybackStartPlan()
            )
            emitPlaybackCommand(
                type = "PLAY",
                source = commandSource,
                positionMs = 0L,
                currentIndex = 0
            )
        }
        else -> {}
    }
}

internal fun PlayerManager.handleTrackEndedIfNeededImpl(source: String) {
    val currentKey = trackEndDeduplicationKey(
        mediaId = player.currentMediaItem?.mediaId,
        fallbackSongKey = _currentSongFlow.value?.stableKey()
    )
    val isRepeatOne = repeatModeSetting == Player.REPEAT_MODE_ONE
    if (
        !isRepeatOne &&
        !shouldHandleTrackEnd(lastHandledKey = lastHandledTrackEndKey, currentKey = currentKey)
    ) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "忽略重复的曲目结束事件: source=$source, key=$currentKey"
        )
        return
    }
    val now = SystemClock.elapsedRealtime()
    if (now - lastTrackEndHandledAtMs < 500L) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "忽略过近的曲目结束事件: source=$source, key=$currentKey, delta=${now - lastTrackEndHandledAtMs}ms"
        )
        return
    }
    lastHandledTrackEndKey = currentKey
    lastTrackEndHandledAtMs = now
    LyraLogger.d(
        "LYRA-PlayerManager",
        "开始处理曲目结束事件: source=$source, key=$currentKey, index=$currentIndex, queueSize=${currentPlaylist.size}"
    )
    persistPlaybackStatsSnapshotAsync(
        synchronized(playbackStatsTracker) {
            playbackStatsTracker.onTrackEnded()
        }
    )
    handleTrackEnded()
}

internal fun PlayerManager.pauseImpl(
    forcePersist: Boolean = false,
    commandSource: PlaybackCommandSource = PlaybackCommandSource.LOCAL,
    allowFadeOut: Boolean = true,
    preserveMutedVolume: Boolean = false,
    debugReason: String = "pause_internal",
    flushPlayerOutput: Boolean = false,
) {
    ensureInitialized()
    if (!initialized) return
    val internalUsbTransition = debugReason.startsWith("usb_toggle_")
    if (!internalUsbTransition && shouldBlockLocalRoomControl(commandSource)) return
    restoredShouldResumePlayback = false
    restoredResumePositionMs = 0L
    if (isPendingMediaLoadActive()) {
        val action = resolvePendingPauseAction(
            pendingLoadActive = true,
            exposedPositionMs = _playbackPositionMs.value
        )
        cancelPlaybackStartupWatchdog(reason = debugReason)
        cancelPendingPauseRequest(resetVolumeToFull = true)
        updateResumePlaybackRequested(action.resumePlaybackRequested)
        if (!action.resumePlaybackRequested) {
            PlaybackTransitionWakeLock.release(
                playbackRequestToken,
                "pending_pause:$debugReason"
            )
        }
        if (!internalUsbTransition) {
            clearUsbExclusiveInterruptedPlaybackIntent("pending_pause:$debugReason")
        }
        playbackRequestToken += 1
        playJob?.cancel()
        playJob = null
        pendingMediaLoadActive = false
        pendingMediaLoadPositionMs = action.persistPositionMs
        _playWhenReadyFlow.value = action.resumePlaybackAfterLoad
        _isPlayingFlow.value = false
        if (flushPlayerOutput) {
            runCatching {
                player.playWhenReady = false
                player.stop()
            }
            _playerPlaybackStateFlow.value = Player.STATE_IDLE
        }
        if (lyriconEnabled) {
            LyriconManager.setPlaybackState(false)
        }
        clearAudioRouteMuteSuppression(
            reason = debugReason,
            preserveExplicitRestore = shouldDeferAudioRouteMuteRestore(
                audioRouteMuteRequiresExplicitRestore
            )
        )
        persistPausedPlaybackState(
            forcePersist = forcePersist,
            positionMs = action.persistPositionMs,
            shouldResumePlayback = action.persistShouldResumePlayback,
            reason = debugReason
        )
        emitPlaybackCommand(
            type = "PAUSE",
            source = commandSource,
            positionMs = action.persistPositionMs,
            currentIndex = currentIndex
        )
        return
    }
    LyraLogger.d(
        "LYRA-PlayerManager",
        "pause requested: forcePersist=$forcePersist, source=$commandSource, allowFadeOut=$allowFadeOut, preserveMutedVolume=$preserveMutedVolume, reason=$debugReason, currentSong=${_currentSongFlow.value?.name}, isPlaying=${player.isPlaying}, playWhenReady=${player.playWhenReady}, stack=[${debugStackHint()}]"
    )
    cancelPendingPauseRequest()
    cancelPlaybackStartupWatchdog(reason = debugReason)
    updateResumePlaybackRequested(false)
    PlaybackTransitionWakeLock.release(playbackRequestToken, "pause:$debugReason")
    if (!internalUsbTransition) {
        clearUsbExclusiveInterruptedPlaybackIntent("pause:$debugReason")
    }
    playbackRequestToken += 1
    playJob?.cancel()
    playJob = null
    val effectiveAllowFadeOut = allowFadeOut && !shouldBypassUsbExclusivePauseFade(debugReason)
    val pauseVolumePlan = resolvePauseVolumePlan(
        allowFadeOut = effectiveAllowFadeOut,
        preserveMutedVolume = preserveMutedVolume,
        playbackFadeInEnabled = playbackFadeInEnabled,
        playbackFadeOutDurationMs = playbackFadeOutDurationMs,
        isPlayerInitialized = isPlayerInitialized()
    )
    if (pauseVolumePlan.shouldFadeOut) {
        val scheduledPauseToken = playbackRequestToken
        lateinit var scheduledPauseJob: Job
        scheduledPauseJob = mainScope.launch {
            try {
                fadeOutCurrentPlaybackIfNeeded(
                    enabled = true,
                    fadeOutDurationMs = playbackFadeOutDurationMs
                )
                if (scheduledPauseToken != playbackRequestToken) {
                    LyraLogger.d(
                        "LYRA-PlayerManager",
                        "暂停请求已过期，跳过淡出后的暂停: requestToken=$scheduledPauseToken, currentToken=$playbackRequestToken"
                    )
                    return@launch
                }
                pauseInternal(
                    forcePersist = forcePersist,
                    resetVolumeBeforePause = pauseVolumePlan.resetVolumeBeforePause,
                    restoreVolumeAfterPause = pauseVolumePlan.restoreVolumeAfterPause,
                    debugReason = debugReason,
                    flushPlayerOutput = flushPlayerOutput,
                )
            } finally {
                if (pendingPauseJob === scheduledPauseJob) {
                    pendingPauseJob = null
                }
            }
        }
        pendingPauseJob = scheduledPauseJob
    } else {
        pauseInternal(
            forcePersist = forcePersist,
            resetVolumeBeforePause = pauseVolumePlan.resetVolumeBeforePause,
            restoreVolumeAfterPause = pauseVolumePlan.restoreVolumeAfterPause,
            debugReason = debugReason,
            flushPlayerOutput = flushPlayerOutput,
        )
    }
    emitPlaybackCommand(
        type = "PAUSE",
        source = commandSource,
        positionMs = _playbackPositionMs.value,
        currentIndex = currentIndex
    )
}

private fun PlayerManager.shouldBypassUsbExclusivePauseFade(debugReason: String): Boolean {
    if (!usbExclusivePlaybackEnabled && !debugReason.contains("usb", ignoreCase = true)) {
        return false
    }
    val pathState = UsbExclusiveAudioPathTracker.state.value
    return pathState.effectivePath == UsbExclusiveAudioPathState.EFFECTIVE_NATIVE_USB ||
        pathState.fallbackReason?.contains("native", ignoreCase = true) == true ||
        pathState.fallbackReason?.contains("usb", ignoreCase = true) == true ||
        debugReason.contains("usb", ignoreCase = true)
}

private fun PlayerManager.pauseInternal(
    forcePersist: Boolean,
    resetVolumeBeforePause: Boolean,
    restoreVolumeAfterPause: Boolean,
    debugReason: String,
    flushPlayerOutput: Boolean,
) {
    pendingPauseJob = null
    updateResumePlaybackRequested(false)
    val currentSong = _currentSongFlow.value
    val currentPosition = player.currentPosition.coerceAtLeast(0L)
    val expectedDuration = currentSong?.durationMs?.takeIf { it > 0L } ?: player.duration
    val shouldForceFlushShortLocalSong =
        currentSong?.let(::isLocalSong) == true && expectedDuration in 1L..5_000L
    playbackRequestToken += 1
    playJob?.cancel()
    playJob = null
    cancelVolumeFade(resetToFull = resetVolumeBeforePause)
    val stackHint = Throwable().stackTrace.take(6).joinToString(" <- ") {
        "${it.fileName}:${it.lineNumber}"
    }
    LyraLogger.d(
        "LYRA-PlayerManager",
        "pauseInternal: reason=$debugReason, song=${currentSong?.name}, positionMs=$currentPosition, state=${playbackStateName(player.playbackState)}, playWhenReady=${player.playWhenReady}, forcePersist=$forcePersist, resetVolumeBeforePause=$resetVolumeBeforePause, restoreVolumeAfterPause=$restoreVolumeAfterPause, stack=[$stackHint]"
    )
    player.playWhenReady = false
    if (flushPlayerOutput) {
        player.stop()
        _playerPlaybackStateFlow.value = Player.STATE_IDLE
        stopProgressUpdates()
    } else {
        player.pause()
    }
    if (lyriconEnabled) {
        LyriconManager.setPlaybackState(false)
    }
    syncPlaybackStatsPlayingState(
        playing = false,
        reason = debugReason
    )
    if (shouldForceFlushShortLocalSong) {
        runCatching {
            player.seekTo(currentPosition.coerceAtMost(expectedDuration.coerceAtLeast(0L)))
        }
        _playbackPositionMs.value = currentPosition
    }
    if (restoreVolumeAfterPause && !isAudioRouteMuteSuppressed()) {
        runPlayerActionOnMainThread {
            if (isPlayerInitialized()) {
                player.volume = 1f
            }
        }
    }
    clearAudioRouteMuteSuppression(
        reason = debugReason,
        preserveExplicitRestore = shouldDeferAudioRouteMuteRestore(
            audioRouteMuteRequiresExplicitRestore
        )
    )
    persistLongFormPlaybackProgress(
        song = currentSong,
        positionMs = currentPosition,
        durationMs = maxOf(
            expectedDuration.coerceAtLeast(0L),
            _playbackDurationMs.value
        )
    )
    persistPausedPlaybackState(
        forcePersist = forcePersist,
        positionMs = currentPosition,
        shouldResumePlayback = false,
        reason = debugReason
    )
}

internal fun PlayerManager.togglePlayPauseImpl(allowFade: Boolean = true) {
    ensureInitialized()
    if (!initialized) return
    if (isAudioRouteMuteSuppressed()) {
        restoreAudioRouteMuteImpl()
        return
    }
    if (shouldPausePlaybackWhenToggling(
            resumePlaybackRequested = resumePlaybackRequested,
            pendingPauseJobActive = pendingPauseJob?.isActive == true,
            playerIsPlaying = player.isPlaying,
            playerPlayWhenReady = player.playWhenReady,
            playJobActive = playJob?.isActive == true
        )
    ) {
        pauseImpl(
            allowFadeOut = allowFade,
            debugReason = if (allowFade) "toggle_play_pause" else "skip_interval_editor"
        )
    } else {
        playImpl(allowFadeIn = allowFade)
    }
}

internal fun PlayerManager.seekToImpl(
    positionMs: Long,
    commandSource: PlaybackCommandSource = PlaybackCommandSource.LOCAL
) {
    ensureInitialized()
    if (!initialized) return
    if (commandSource == PlaybackCommandSource.LOCAL && shouldBlockLocalRoomControl(commandSource)) return
    val resolvedPositionMs = positionMs.coerceAtLeast(0L)
    playbackPositionGeneration += 1L
    LyraLogger.d(
        "LYRA-PlayerManager",
        "seekTo requested: positionMs=$resolvedPositionMs, source=$commandSource, currentSong=${_currentSongFlow.value?.name}, currentUrl=${_currentMediaUrl.value}, stack=[${debugStackHint()}]"
    )
    val currentSong = _currentSongFlow.value
    val currentUrl = _currentMediaUrl.value
    val currentPositionMs = player.currentPosition.coerceAtLeast(0L)
    val knownDurationMs = maxOf(
        player.duration.coerceAtLeast(0L),
        currentSong?.durationMs?.coerceAtLeast(0L) ?: 0L
    )
    val shouldExpediteYouTubeSeekRecovery =
        YouTubeSeekRefreshPolicy.shouldUseExpeditedRecoveryAfterSeek(
            song = currentSong,
            currentUrl = currentUrl,
            previousPositionMs = currentPositionMs,
            targetPositionMs = resolvedPositionMs,
            durationMs = knownDurationMs
        )
    val shouldRefreshYouTubeUrlBeforeSeek =
        YouTubeSeekRefreshPolicy.shouldRefreshUrlBeforeSeek(currentSong, currentUrl) ||
            shouldExpediteYouTubeSeekRecovery
    val pendingLoadActive = isPendingMediaLoadActive()
    // 正在装载新媒体时交给现有 pending-load 流程，避免替旧媒体启动一条并行刷新
    val seekExecutionAction = resolveSeekExecutionAction(
        pendingLoadActive = pendingLoadActive,
        urlRefreshRequested = shouldRefreshYouTubeUrlBeforeSeek
    )
    if (shouldRefreshYouTubeUrlBeforeSeek) {
        rememberPendingSeekPosition(resolvedPositionMs)
        expeditedYouTubeSeekRecoveryPending = shouldExpediteYouTubeSeekRecovery
    } else {
        clearPendingSeekPosition()
    }
    val pendingSeekAction = resolvePendingSeekAction(
        pendingLoadActive = pendingLoadActive,
        requestedPositionMs = resolvedPositionMs
    )
    pendingSeekAction.pendingSeekPositionMs?.let(::rememberPendingSeekPosition)
    pendingMediaLoadPositionMs = pendingSeekAction.exposedPositionMs
    if (seekExecutionAction.seekPlayerNow) {
        player.seekTo(resolvedPositionMs)
    }
    if (lyriconEnabled) {
        LyriconManager.setPosition(resolvedPositionMs)
    }
    updateExternalBluetoothLyricLine(resolvedPositionMs)
    synchronized(playbackStatsTracker) {
        playbackStatsTracker.onManualSeek(resolvedPositionMs)
    }
    _playbackPositionMs.value = resolvedPositionMs
    persistLongFormPlaybackProgress(
        song = currentSong,
        positionMs = resolvedPositionMs,
        durationMs = knownDurationMs
    )
    scheduleStatePersist(
        positionMs = pendingSeekAction.persistPositionMs,
        shouldResumePlayback = shouldResumePlaybackSnapshot()
    )
    emitPlaybackCommand(
        type = "SEEK",
        source = commandSource,
        positionMs = resolvedPositionMs,
        currentIndex = currentIndex
    )
    if (seekExecutionAction.refreshUrlInBackground) {
        refreshCurrentSongUrl(
            resumePositionMs = resolvedPositionMs,
            allowFallback = false,
            reason = if (shouldExpediteYouTubeSeekRecovery) {
                "youtube_seek_expedited_url_refresh"
            } else {
                "youtube_seek_url_refresh"
            },
            bypassCooldown = true,
            resumePlaybackAfterRefresh = shouldResumePlaybackSnapshot(),
            resumedPlaybackCommandSource = commandSource,
            youtubeRecoveryStrategy = youtubePlaybackRecoveryStrategyForSeek()
        )
    }
}

internal fun PlayerManager.nextImpl(
    force: Boolean = false,
    commandSource: PlaybackCommandSource = PlaybackCommandSource.LOCAL,
    bypassLoudVolumeWarning: Boolean = false,
    allowRememberedLongFormPosition: Boolean = false
) {
    ensureInitialized()
    if (!initialized) return
    if (shouldBlockLocalRoomControl(commandSource)) return
    if (currentPlaylist.isEmpty()) return
    val isShuffle = player.shuffleModeEnabled
    val useTransitionFade =
        playbackCrossfadeNextEnabled && (player.isPlaying || player.playWhenReady)
    LyraLogger.d(
        "LYRA-PlayerManager",
        "next requested: force=$force, source=$commandSource, isShuffle=$isShuffle, currentIndex=$currentIndex, queueSize=${currentPlaylist.size}, transitionFade=$useTransitionFade, stack=[${debugStackHint()}]"
    )
    val hasNextTrack = currentIndex < currentPlaylist.lastIndex ||
        force ||
        repeatModeSetting == Player.REPEAT_MODE_ALL
    if (!hasNextTrack) {
        LyraLogger.d("LYRA-Player", "Already at the end of the playlist.")
        return
    }
    if (requestUsbExclusiveLoudPlaybackConfirmation(
            commandSource = commandSource,
            bypassWarning = bypassLoudVolumeWarning,
            continuePlayback = {
                nextImpl(
                    force = force,
                    commandSource = commandSource,
                    bypassLoudVolumeWarning = true,
                    allowRememberedLongFormPosition = allowRememberedLongFormPosition
                )
            }
        )
    ) {
        return
    }

    if (currentIndex < currentPlaylist.lastIndex) {
        currentIndex++
    } else {
        if (force || repeatModeSetting == Player.REPEAT_MODE_ALL) {
            if (!reshuffleCurrentQueueForRepeatAllCycle()) {
                currentIndex = 0
            }
        } else {
            LyraLogger.d("LYRA-Player", "Already at the end of the playlist.")
            return
        }
    }
    playAtIndex(
        currentIndex,
        useTrackTransitionFade = useTransitionFade,
        commandSource = commandSource,
        allowRememberedLongFormPosition = allowRememberedLongFormPosition
    )
    emitPlaybackCommand(
        type = "NEXT",
        source = commandSource,
        queue = currentPlaylist.toList(),
        currentIndex = currentIndex,
        positionMs = _playbackPositionMs.value,
        force = force
    )
}

internal fun PlayerManager.previousImpl(
    commandSource: PlaybackCommandSource = PlaybackCommandSource.LOCAL,
    bypassLoudVolumeWarning: Boolean = false
) {
    ensureInitialized()
    if (!initialized) return
    if (shouldBlockLocalRoomControl(commandSource)) return
    if (currentPlaylist.isEmpty()) return
    val isShuffle = player.shuffleModeEnabled
    val useTransitionFade =
        playbackCrossfadeNextEnabled && (player.isPlaying || player.playWhenReady)
    LyraLogger.d(
        "LYRA-PlayerManager",
        "previous requested: source=$commandSource, isShuffle=$isShuffle, currentIndex=$currentIndex, queueSize=${currentPlaylist.size}, transitionFade=$useTransitionFade, stack=[${debugStackHint()}]"
    )
    val hasPreviousTrack = currentIndex > 0 || repeatModeSetting == Player.REPEAT_MODE_ALL
    if (!hasPreviousTrack) {
        LyraLogger.d("LYRA-Player", "Already at the start of the playlist.")
        return
    }
    if (requestUsbExclusiveLoudPlaybackConfirmation(
            commandSource = commandSource,
            bypassWarning = bypassLoudVolumeWarning,
            continuePlayback = {
                previousImpl(
                    commandSource = commandSource,
                    bypassLoudVolumeWarning = true
                )
            }
        )
    ) {
        return
    }

    if (currentIndex > 0) {
        currentIndex--
        playAtIndex(
            currentIndex,
            useTrackTransitionFade = useTransitionFade,
            commandSource = commandSource
        )
        emitPlaybackCommand(
            type = "PREVIOUS",
            source = commandSource,
            queue = currentPlaylist.toList(),
            currentIndex = currentIndex,
            positionMs = _playbackPositionMs.value
        )
    } else {
        if (repeatModeSetting == Player.REPEAT_MODE_ALL && currentPlaylist.isNotEmpty()) {
            currentIndex = currentPlaylist.lastIndex
            playAtIndex(
                currentIndex,
                useTrackTransitionFade = useTransitionFade,
                commandSource = commandSource
            )
            emitPlaybackCommand(
                type = "PREVIOUS",
                source = commandSource,
                queue = currentPlaylist.toList(),
                currentIndex = currentIndex,
                positionMs = _playbackPositionMs.value
            )
        } else {
            LyraLogger.d("LYRA-Player", "Already at the start of the playlist.")
        }
    }
}

internal fun PlayerManager.cycleRepeatModeImpl(
    commandSource: PlaybackCommandSource = PlaybackCommandSource.LOCAL
) {
    ensureInitialized()
    if (!initialized) return
    if (shouldBlockLocalRoomControl(commandSource)) return
    val previousMode = repeatModeSetting
    val newMode = when (repeatModeSetting) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
        else -> Player.REPEAT_MODE_OFF
    }
    repeatModeSetting = newMode
    syncExoRepeatMode()
    _repeatModeFlow.value = newMode
    LyraLogger.d(
        "LYRA-PlayerManager",
        "cycleRepeatMode: previousMode=$previousMode, newMode=$newMode, exoRepeatMode=${player.repeatMode}"
    )
    scheduleStatePersist()
    emitPlaybackCommand(
        type = "PLAYBACK_MODE",
        source = commandSource,
        repeatMode = newMode,
        shuffleEnabled = player.shuffleModeEnabled
    )
}

internal fun PlayerManager.setShuffleImpl(
    enabled: Boolean,
    commandSource: PlaybackCommandSource = PlaybackCommandSource.LOCAL
) {
    ensureInitialized()
    if (!initialized) return
    if (shouldBlockLocalRoomControl(commandSource)) return
    if (player.shuffleModeEnabled == enabled) {
        if (!enabled) {
            val hadRestoreSnapshot = shuffleRestorePlaylistReference != null
            clearShuffleRestoreQueueSnapshot()
            if (hadRestoreSnapshot) {
                lastPersistedPlaylistReference = null
                scheduleStatePersist()
            }
        }
        return
    }
    LyraLogger.d(
        "LYRA-PlayerManager",
        "setShuffle: enabled=$enabled, currentIndex=$currentIndex, queueSize=${currentPlaylist.size}"
    )
    if (enabled) {
        if (commandSource != PlaybackCommandSource.REMOTE_SYNC) {
            rememberShuffleRestoreQueueSnapshot()
            player.shuffleModeEnabled = true
            shuffleCurrentQueueForSequentialPlayback()
        } else {
            clearShuffleRestoreQueueSnapshot()
            player.shuffleModeEnabled = true
        }
    } else {
        if (commandSource != PlaybackCommandSource.REMOTE_SYNC) {
            restoreShuffleRestoreQueueSnapshot()
        } else {
            clearShuffleRestoreQueueSnapshot()
        }
        player.shuffleModeEnabled = false
    }
    scheduleStatePersist()
    _shuffleModeFlow.value = enabled
    emitPlaybackCommand(
        type = "PLAYBACK_MODE",
        source = commandSource,
        queue = currentPlaylist.toList(),
        currentIndex = currentIndex,
        repeatMode = repeatModeSetting,
        shuffleEnabled = enabled
    )
}

internal fun PlayerManager.startProgressUpdates() {
    if (!shouldRunPlaybackProgressUpdates(
            initialized = initialized,
            pendingMediaLoad = isPendingMediaLoadActive(),
            hasMediaItem = player.currentMediaItem != null,
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady
        )
    ) {
        return
    }
    if (progressJob?.isActive == true) return
    LyraLogger.d(
        "LYRA-PlayerManager",
        "startProgressUpdates: currentSong=${_currentSongFlow.value?.name}, playbackState=${playbackStateName(player.playbackState)}"
    )
    progressJob = mainScope.launch {
        var lastStatsUpdateAtMs = 0L
        while (isActive) {
            val updateIntervalMs = resolvePlaybackProgressUpdateIntervalMs(
                playbackProgressAdvanceReported = playbackProgressAdvanceReported,
                interactiveNowPlayingVisible = interactiveNowPlayingVisible,
                realtimeExternalLyricsActive = isExternalBluetoothLyricCadenceActive()
            )
            val positionMs = runCatching {
                resolveDisplayedPlaybackPosition(player.currentPosition.coerceAtLeast(0L))
            }.onFailure { error ->
                LyraLogger.w(
                    "LYRA-PlayerManager",
                    "progress update read failed for ${_currentSongFlow.value?.name}",
                    error
                )
            }.getOrNull()
            if (positionMs == null) {
                delay(updateIntervalMs)
                continue
            }
            _playbackPositionMs.value = positionMs
            if (!playbackProgressAdvanceReported && isPlaybackActuallyAdvancing()) {
                playbackProgressAdvanceReported = true
                startupStallRecoveryAttempts = 0
                cancelPlaybackStartupWatchdog(reason = "position_advanced")
                PlaybackTransitionWakeLock.release(
                    playbackRequestToken,
                    "position_advanced"
                )
                recordPlaybackRuntimeProgress(player.currentPosition)
                schedulePlaybackRuntimeWatchdog(reason = "position_advanced")
                syncPlaybackStatsPlayingState(
                    playing = true,
                    reason = "progress_position_advanced"
                )
            }
            if (playbackProgressAdvanceReported) {
                recordPlaybackRuntimeProgress(player.currentPosition)
                schedulePlaybackRuntimeWatchdog(reason = "progress_tick")
            }
            val durationMs = runCatching { player.duration.coerceAtLeast(0L) }
                .getOrDefault(_playbackDurationMs.value)
            if (durationMs > 0L) {
                _playbackDurationMs.value = durationMs
            }
            val currentSong = _currentSongFlow.value
            if (currentSong != null) {
                val userSkipPositionMs = BiliVideoSkipPlaybackController.nextSkipPosition(
                    song = currentSong,
                    currentPositionMs = positionMs,
                    durationMs = durationMs
                )
                if (userSkipPositionMs != null) {
                    LyraLogger.d(
                        "BiliVideoSkip",
                        "auto skipping interval: from=${positionMs}ms, to=${userSkipPositionMs}ms"
                    )
                    resolveBiliSkipSegmentPromptMessageRes(
                        promptsEnabled = biliSkipSegmentPromptEnabled,
                        source = BiliSkipSegmentSource.CUSTOM_INTERVAL
                    )?.let { messageRes ->
                        AppFeedback.showToast(
                            context = application,
                            message = getLocalizedString(messageRes)
                        )
                    }
                    seekTo(
                        positionMs = userSkipPositionMs,
                        commandSource = PlaybackCommandSource.LOCAL_SAFETY
                    )
                    AudioPlayerService.refreshPlaybackWidgetAfterSeekFromActiveService(
                        reason = "bili_video_auto_skip"
                    )
                    delay(updateIntervalMs)
                    continue
                }
                val skipPositionMs = BiliSponsorBlockPlaybackController.nextSkipPosition(
                    song = currentSong,
                    currentPositionMs = positionMs,
                    durationMs = durationMs
                )
                if (skipPositionMs != null) {
                    LyraLogger.d(
                        "BiliSponsorBlock",
                        "auto skipping segment: from=${positionMs}ms, to=${skipPositionMs}ms"
                    )
                    resolveBiliSkipSegmentPromptMessageRes(
                        promptsEnabled = biliSkipSegmentPromptEnabled,
                        source = BiliSkipSegmentSource.SPONSOR_BLOCK
                    )?.let { messageRes ->
                        AppFeedback.showToast(
                            context = application,
                            message = getLocalizedString(messageRes)
                        )
                    }
                    seekTo(
                        positionMs = skipPositionMs,
                        commandSource = PlaybackCommandSource.LOCAL_SAFETY
                    )
                    AudioPlayerService.refreshPlaybackWidgetAfterSeekFromActiveService(
                        reason = "bili_sponsor_block_auto_skip"
                    )
                    delay(updateIntervalMs)
                    continue
                }
            }
            if (lyriconEnabled) {
                LyriconManager.setPlaybackSpeed(playbackSoundConfig.speed)
                // 与高级歌词同源: 进度环原始媒体位置, 显示 lead 在 LyriconManager 内处理
                LyriconManager.setPosition(
                    mediaLyriconPositionMs(
                        positionMs = positionMs,
                        durationMs = durationMs,
                    )
                )
            }
            updateExternalBluetoothLyricLine(positionMs)
            maybePersistPlaybackProgress(positionMs)
            maybePersistLongFormPlaybackProgress(positionMs)
            val nowElapsedRealtimeMs = SystemClock.elapsedRealtime()
            if (
                lastStatsUpdateAtMs == 0L ||
                nowElapsedRealtimeMs - lastStatsUpdateAtMs >= PLAYBACK_PROGRESS_STATS_UPDATE_INTERVAL_MS
            ) {
                lastStatsUpdateAtMs = nowElapsedRealtimeMs
                val progressStatsSnapshot = consumePlaybackStatsProgress(positionMs)
                if (progressStatsSnapshot != null) {
                    markTrackEndHandledForStatsFallback()
                }
                persistPlaybackStatsSnapshotAsync(progressStatsSnapshot)
                maybePersistPlaybackStatsProgress()
            }
            delay(updateIntervalMs)
        }
    }
}

internal fun PlayerManager.stopProgressUpdatesImpl() {
    if (progressJob?.isActive == true) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "stopProgressUpdates: currentSong=${_currentSongFlow.value?.name}, currentPosition=${_playbackPositionMs.value}"
        )
    }
    progressJob?.cancel()
    progressJob = null
    resetPlaybackRuntimeWatchdog(reason = "progress_updates_stopped")
}

private fun PlayerManager.maybePersistPlaybackProgress(positionMs: Long) {
    if (currentPlaylist.isEmpty()) return
    if (!shouldResumePlaybackSnapshot()) return
    val now = SystemClock.elapsedRealtime()
    if (now - lastStatePersistAtMs < STATE_PERSIST_INTERVAL_MS) return
    lastStatePersistAtMs = now
    LyraLogger.d(
        "LYRA-PlayerManager",
        "maybePersistPlaybackProgress(): positionMs=$positionMs, queueSize=${currentPlaylist.size}, currentIndex=$currentIndex, song=${_currentSongFlow.value?.name}"
    )
    scheduleStatePersist(positionMs = positionMs, shouldResumePlayback = true)
}

private fun PlayerManager.maybePersistLongFormPlaybackProgress(positionMs: Long) {
    val song = _currentSongFlow.value ?: return
    val durationMs = maxOf(song.durationMs, _playbackDurationMs.value)
    if (!rememberLongFormPlaybackProgressEnabled) return
    if (durationMs < LONG_FORM_PLAYBACK_MIN_DURATION_MS) return
    val now = SystemClock.elapsedRealtime()
    if (now - lastLongFormPlaybackProgressPersistAtMs < STATE_PERSIST_INTERVAL_MS) return
    lastLongFormPlaybackProgressPersistAtMs = now
    persistLongFormPlaybackProgress(
        song = song,
        positionMs = positionMs,
        durationMs = durationMs
    )
}

private fun PlayerManager.consumePlaybackStatsProgress(positionMs: Long): PlaybackStatsSnapshot? {
    return synchronized(playbackStatsTracker) {
        playbackStatsTracker.onPlaybackProgress(positionMs)
    }
}

private fun PlayerManager.maybePersistPlaybackStatsProgress() {
    val snapshot = synchronized(playbackStatsTracker) {
        if (playbackStatsTracker.shouldFlushPeriodically()) {
            playbackStatsTracker.flushPeriodic()
        } else {
            null
        }
    }
    persistPlaybackStatsSnapshotAsync(snapshot)
}

internal fun PlayerManager.stopPlaybackPreservingQueueImpl(clearMediaUrl: Boolean = false) {
    LyraLogger.d(
        "LYRA-PlayerManager",
        "stopPlaybackPreservingQueue(): clearMediaUrl=$clearMediaUrl, queueSize=${currentPlaylist.size}, currentIndex=$currentIndex, currentSong=${_currentSongFlow.value?.name}, mediaUrlPresent=${!_currentMediaUrl.value.isNullOrBlank()}, stack=[${debugStackHint()}]"
    )
    cancelPendingPauseRequest(resetVolumeToFull = true)
    clearPlaybackDemandCacheKey(reason = "stop_playback_preserving_queue")
    playbackRequestToken += 1
    playJob?.cancel()
    playJob = null
    pendingMediaLoadActive = false
    cancelPlaybackStartupWatchdog(reason = "stop_playback_preserving_queue")
    clearActivePlaybackCandidates()
    currentYouTubePrefetchJob?.cancel()
    currentYouTubePrefetchJob = null
    currentYouTubePrefetchVideoIds = emptySet()
    lastHandledTrackEndKey = null
    updateResumePlaybackRequested(false)
    PlaybackTransitionWakeLock.release(
        playbackRequestToken,
        "stop_playback_preserving_queue"
    )
    lastAutoTrackAdvanceAtMs = 0L
    stopProgressUpdates()
    cancelVolumeFade(resetToFull = true)
    clearAudioRouteMuteSuppression(reason = "stop_playback_preserving_queue")
    persistCurrentLongFormPlaybackProgress()
    syncPlaybackStatsPlayingState(
        playing = false,
        reason = "stop_playback_preserving_queue"
    )
    runCatching { player.stop() }
    runCatching { player.clearMediaItems() }
    _isPlayingFlow.value = false
    if (lyriconEnabled) {
        LyriconManager.setPlaybackState(false)
    }
    _playWhenReadyFlow.value = false
    _playerPlaybackStateFlow.value = Player.STATE_IDLE
    clearPendingSeekPosition()
    _playbackPositionMs.value = 0L
    if (currentPlaylist.isEmpty()) {
        currentIndex = -1
        setCurrentSongForPlayback(null)
        _currentMediaUrl.value = null
        _currentPlaybackAudioInfo.value = null
        currentMediaUrlResolvedAtMs = 0L
    } else {
        currentIndex = currentIndex.coerceIn(0, currentPlaylist.lastIndex)
        setCurrentSongForPlayback(currentPlaylist.getOrNull(currentIndex))
        if (clearMediaUrl) {
            _currentMediaUrl.value = null
            _currentPlaybackAudioInfo.value = null
            currentMediaUrlResolvedAtMs = 0L
        }
    }
    consecutivePlayFailures = 0
    LyraLogger.d(
        "LYRA-PlayerManager",
        "stopPlaybackPreservingQueue(): completed, queueSize=${currentPlaylist.size}, currentIndex=$currentIndex, retainedSong=${_currentSongFlow.value?.name}, mediaUrlPresent=${!_currentMediaUrl.value.isNullOrBlank()}"
    )
    scheduleStatePersist()
}

internal fun PlayerManager.stopPlaybackImmediatelyImpl(
    reason: String,
    forcePersist: Boolean = true
) {
    pauseImpl(
        forcePersist = forcePersist,
        commandSource = PlaybackCommandSource.LOCAL_SAFETY,
        allowFadeOut = false,
        debugReason = reason,
        flushPlayerOutput = true,
    )
}
