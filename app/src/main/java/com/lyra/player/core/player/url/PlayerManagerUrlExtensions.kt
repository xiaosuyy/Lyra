@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.lyra.player.core.player.url

import android.net.Uri
import android.os.SystemClock
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.StuckPlayerException
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.lyra.player.R
import com.lyra.player.core.api.bili.BiliSponsorBlockTarget
import com.lyra.player.core.api.bili.resolveBiliSong
import com.lyra.player.core.api.gdstudio.GdStudioApiClient
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.core.download.GlobalDownloadManager
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.core.player.download.AudioDownloadManager
import com.lyra.player.core.player.lifecycle.updateAudioOffloadPreferences
import com.lyra.player.core.player.model.PlaybackAudioInfo
import com.lyra.player.core.player.model.PlaybackAudioSource
import com.lyra.player.core.player.model.PlayerEvent
import com.lyra.player.core.player.model.SongUrlResult
import com.lyra.player.core.player.model.mergeLocalPlaybackAudioInfoWithRemoteQuality
import com.lyra.player.core.player.policy.command.PlaybackCommandSource
import com.lyra.player.core.player.policy.refresh.RefreshDeferredCompletion
import com.lyra.player.core.player.policy.refresh.RefreshRequestSemantics
import com.lyra.player.core.player.policy.refresh.RefreshResolverSideEffects
import com.lyra.player.core.player.policy.refresh.RefreshResultSideEffects
import com.lyra.player.core.player.policy.refresh.RefreshResultKind
import com.lyra.player.core.player.policy.refresh.RefreshSideEffectGate
import com.lyra.player.core.player.policy.command.resolvePlaybackStartPlan
import com.lyra.player.core.player.policy.refresh.resolveRefreshApplyAction
import com.lyra.player.core.player.policy.refresh.resolveRefreshedMediaStartPosition
import com.lyra.player.core.player.policy.refresh.shouldApplyRefreshResult
import com.lyra.player.core.player.policy.refresh.YouTubePlaybackRecoveryStrategy
import com.lyra.player.core.player.playback.advanceAfterPlaybackFailure
import com.lyra.player.core.player.playback.BiliSponsorBlockPlaybackController
import com.lyra.player.core.player.playback.BiliVideoSkipPlaybackController
import com.lyra.player.core.player.playback.preparePlayerForManagedStart
import com.lyra.player.core.player.playback.startProgressUpdates
import com.lyra.player.core.player.prefetch.consumeGenericUrlPrefetch
import com.lyra.player.core.player.quality.effectiveBiliQuality
import com.lyra.player.core.player.quality.effectiveNeteaseQuality
import com.lyra.player.core.player.quality.effectiveYouTubeQuality
import com.lyra.player.core.player.resolver.netease.NeteasePlaybackResponseParser
import com.lyra.player.core.player.resolver.netease.tryResolveNeteaseAutoBiliSource
import com.lyra.player.core.player.resolver.netease.tryResolveNeteaseAutoCrossPlatformSource
import com.lyra.player.core.player.resolver.channel.getChannelSongAudioUrl
import com.lyra.player.core.player.resolver.netease.tryResolveNeteaseMatchedLocalSource
import com.lyra.player.core.customsource.CUSTOM_SOURCE_NETEASE
import com.lyra.player.core.customsource.CustomSourceManager
import com.lyra.player.core.customsource.buildCustomSourceMusicInfo
import com.lyra.player.core.customsource.customSourceIdForSong
import com.lyra.player.core.player.watchdog.configureActivePlaybackCandidates
import com.lyra.player.core.player.watchdog.currentPlaybackCandidate
import com.lyra.player.core.player.watchdog.resetPlaybackProgressAdvanceBaseline
import com.lyra.player.core.player.watchdog.schedulePlaybackStartupWatchdog
import com.lyra.player.data.model.recoverNeteaseRemoteSourceFromStaleLocalCopy
import com.lyra.player.data.model.sameIdentityAs
import com.lyra.player.data.platform.bili.BiliAudioStreamInfo
import com.lyra.player.data.platform.bili.BiliVideoSkipTarget
import com.lyra.player.data.platform.youtube.extractYouTubeMusicVideoId
import com.lyra.player.data.model.SongItem
import com.lyra.player.core.logging.LyraLogger
import java.io.File

internal const val OFFLINE_CACHE_URL_PREFIX = "http://offline.cache/"

/** 网易云试听片段使用的缓存 key 前缀（与 buildNeteasePreviewCacheKey 保持一致） */
private const val NETEASE_PREVIEW_CACHE_KEY_PREFIX = "netease-preview-v1-"
/**
 * 自定义源单次取链的最长等待时间。
 *
 * 脚本可能因为网络或风控一直不回调，如果没有上限，播放会永远停在“加载中”。
 */
private const val CUSTOM_SOURCE_QUALITY_TIMEOUT_MS = 6_000L
/** 自定义源整体取链超时（含音源脚本加载） */
private const val CUSTOM_SOURCE_TOTAL_TIMEOUT_MS = 20_000L
/** 网易云受限/试听资源兜底时最多尝试的音质数量 */
internal const val NETEASE_CUSTOM_SOURCE_QUALITY_ATTEMPTS = 3
/** 网易云受限/试听资源兜底取链的总超时，超时后尽快回落到试听片段 */
internal const val NETEASE_CUSTOM_SOURCE_TIMEOUT_MS = 12_000L
internal const val YOUTUBE_PLAYBACK_PREFER_M4A = false
internal const val YOUTUBE_STABLE_RECOVERY_QUALITY = "high"

internal data class CachedResourceIntegrity(
    val isComplete: Boolean,
    val requiresRepair: Boolean,
    val coveredLength: Long
)

internal enum class CachePrefetchReadiness {
    COMPLETE,
    READY_FOR_PREFETCH,
    UNAVAILABLE
}

internal fun inspectCachedResourceSpans(
    spans: Collection<CacheSpan>,
    contentLength: Long
): CachedResourceIntegrity {
    if (contentLength <= 0L || spans.isEmpty()) {
        return CachedResourceIntegrity(
            isComplete = false,
            requiresRepair = false,
            coveredLength = 0L
        )
    }

    var coveredUntil = 0L
    var requiresRepair = false
    var hasGap = false
    for (span in spans.sortedBy { it.position }) {
        val file = span.file
        val hasValidFile = span.isCached &&
            span.position >= 0L &&
            span.length > 0L &&
            file?.isFile == true &&
            file.length() == span.length
        if (!hasValidFile) {
            requiresRepair = true
            continue
        }

        val endPosition = span.position + span.length
        if (endPosition < span.position) {
            requiresRepair = true
            continue
        }
        if (
            span.position >= contentLength ||
            endPosition > contentLength
        ) {
            requiresRepair = true
            continue
        }
        if (span.position > coveredUntil) {
            hasGap = true
            continue
        }
        if (span.position < coveredUntil) {
            requiresRepair = true
            continue
        }
        coveredUntil = maxOf(coveredUntil, endPosition)
    }

    return CachedResourceIntegrity(
        isComplete = coveredUntil >= contentLength && !requiresRepair && !hasGap,
        requiresRepair = requiresRepair,
        coveredLength = coveredUntil
    )
}

internal suspend fun PlayerManager.resolveSongUrl(
    song: SongItem,
    forceRefresh: Boolean = false,
    youtubeRecoveryStrategy: YouTubePlaybackRecoveryStrategy? = null,
    sideEffects: RefreshResolverSideEffects = RefreshResolverSideEffects(),
    allowGenericPrefetchCache: Boolean = true,
    playbackRequestTokenOverride: Long? = null,
    shouldApplyCacheMutation: () -> Boolean = { true }
): SongUrlResult {
    LyraLogger.d(
        "LYRA-PlayerManager",
        "resolveSongUrl: song=${song.name}, source=${song.album}, forceRefresh=$forceRefresh, streamUrl=${song.streamUrl}, currentUrl=${_currentMediaUrl.value}, stack=[${debugStackHint()}]"
    )
    if (!forceRefresh && isDirectStreamUrl(song.streamUrl)) {
        prepareBiliPlaybackSkipsForResolvedPlayback(song, playbackRequestTokenOverride)
        return SongUrlResult.Success(song.streamUrl.orEmpty())
    }
    if (isLocalSong(song)) {
        val localMediaUri = localMediaSource(song)
        if (localMediaUri != null && isReadableLocalMediaUri(localMediaUri)) {
            val playbackAudioInfo = localMediaUri.toLocalPlaybackUri()
                ?.let { buildLocalPlaybackAudioInfo(it, application) }
                ?: buildLocalPlaybackAudioInfo(song, application)
            prepareBiliPlaybackSkipsForResolvedPlayback(song, playbackRequestTokenOverride)
            return SongUrlResult.Success(
                url = toPlayableLocalUrl(localMediaUri) ?: localMediaUri,
                audioInfo = playbackAudioInfo
            )
        }
        song.recoverNeteaseRemoteSourceFromStaleLocalCopy()?.let { recoveredSong ->
            LyraLogger.w(
                "LYRA-PlayerManager",
                "Deleted downloaded reference found in remote entry, retry as Netease: " +
                    "song=${song.name}, stale=$localMediaUri"
            )
            return resolveSongUrl(
                song = recoveredSong,
                forceRefresh = forceRefresh,
                youtubeRecoveryStrategy = youtubeRecoveryStrategy,
                sideEffects = sideEffects,
                allowGenericPrefetchCache = allowGenericPrefetchCache,
                playbackRequestTokenOverride = playbackRequestTokenOverride,
                shouldApplyCacheMutation = shouldApplyCacheMutation
            )
        }
        return SongUrlResult.Failure
    }

    val localResult = checkLocalCache(song, sideEffects)
    if (localResult != null) {
        prepareBiliPlaybackSkipsForResolvedPlayback(song, playbackRequestTokenOverride)
        LyraLogger.d(
            "LYRA-PlayerManager",
            "resolveSongUrl: hit local playback cache for song=${song.name}"
        )
        return localResult
    }
    val isYouTubeTrack = isYouTubeMusicTrack(song)
    val cacheKey = computeCacheKey(
        song = song,
        youtubeQualityOverride = youtubeRecoveryStrategy?.preferredQualityOverride,
        youtubePreferM4aOverride = youtubeRecoveryStrategy?.preferM4a
    )
    val cacheKeyIsUnsafe = loadPlaybackCacheKeySafety(cacheKey)
    val cacheIntegrity = if (forceRefresh) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "resolveSongUrl: bypass complete YouTube cache for forced refresh: $cacheKey"
        )
        CachedResourceIntegrity(false, false, 0L)
    } else {
        inspectExoPlayerCache(cacheKey)
    }
    if (cacheIntegrity.requiresRepair) {
        invalidateCachedResourceForPlaybackRecovery(
            cacheKey = cacheKey,
            reason = "resolve_integrity_check",
            shouldApplyMutation = shouldApplyCacheMutation
        )
    }
    var hasCachedData = !forceRefresh &&
        cacheIntegrity.isComplete &&
        !cacheKeyIsUnsafe
    if (hasCachedData) {
        val cachedDescriptor = cache?.readCachedPlaybackDescriptor(cacheKey)
        val cachedAudioInfo = cachedDescriptor?.toPlaybackAudioInfo {
            getLocalizedString(it)
        }
        val cachedContentLength = cache?.let {
            ContentMetadata.getContentLength(it.getContentMetadata(cacheKey))
        } ?: 0L
        if (
            cachedAudioInfo == null ||
            !cachedDescriptor.matchesCachedContentLength(cachedContentLength)
        ) {
            LyraLogger.w(
                "LYRA-PlayerManager",
                "完整缓存描述符缺失或长度不匹配，淘汰旧资源并重新解析: key=$cacheKey"
            )
            val removed = invalidateCachedResourceForPlaybackRecovery(
                cacheKey = cacheKey,
                reason = "missing_playback_descriptor",
                shouldApplyMutation = shouldApplyCacheMutation
            )
            hasCachedData = false
            if (!removed) {
                LyraLogger.w(
                    "LYRA-PlayerManager",
                    "无法移除缺少描述符的缓存，跳过离线复用: key=$cacheKey"
                )
            }
        }
    }
    if (hasCachedData) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "命中完整缓存，直接走离线缓存地址: $cacheKey"
        )
        val cachedDescriptor = cache?.readCachedPlaybackDescriptor(cacheKey)
        val cachedAudioInfo = cachedDescriptor?.toPlaybackAudioInfo {
            getLocalizedString(it)
        } ?: return SongUrlResult.Failure
        prepareBiliPlaybackSkipsForResolvedPlayback(song, playbackRequestTokenOverride)
        val cachedResult = SongUrlResult.Success(
            url = "$OFFLINE_CACHE_URL_PREFIX$cacheKey",
            durationMs = song.durationMs.takeIf { it > 0L },
            audioInfo = cachedAudioInfo,
            mimeType = cachedAudioInfo.mimeType,
            expectedContentLength = cachedDescriptor.expectedContentLength,
            representationIdentity = cachedDescriptor.representationIdentity,
            cacheKeyOverride = cacheKey
        )
        return cachedResult
    }
    if (!forceRefresh && allowGenericPrefetchCache && !isYouTubeTrack) {
        consumeGenericUrlPrefetch(cacheKey)?.let { prefetchedResult ->
            prepareBiliPlaybackSkipsForResolvedPlayback(song)
            return prefetchedResult
        }
    }
    var neteasePermanentFailure = false
    var channelPermanentFailure = false
    val result = retrySongUrlResolution(
        shouldRetry = {
            neteasePermanentFailure.not() && channelPermanentFailure.not() &&
                it is SongUrlResult.Failure
        }
    ) { retryAttempt ->
        neteasePermanentFailure = false
        val isFinalAttempt = retryAttempt == SONG_URL_RESOLUTION_RETRY_COUNT
        if (retryAttempt > 0) {
            LyraLogger.w(
                "LYRA-PlayerManager",
                "resolveSongUrl: retry=$retryAttempt/$SONG_URL_RESOLUTION_RETRY_COUNT, song=${song.name}, source=${song.album}"
            )
        }
        val suppressError = hasCachedData || !isFinalAttempt
        when {
            isYouTubeTrack -> getYouTubeMusicAudioUrl(
                song = song,
                suppressError = suppressError,
                forceRefresh = forceRefresh,
                youtubeRecoveryStrategy = youtubeRecoveryStrategy,
                sideEffects = sideEffects
            )
            isBiliTrack(song) -> getBiliAudioUrl(
                song = song,
                suppressError = suppressError,
                sideEffects = sideEffects,
                playbackRequestTokenOverride = playbackRequestTokenOverride
            )
            isGdStudioTrack(song) -> getGdStudioAudioUrl(
                song = song,
                sideEffects = sideEffects
            )
            customSourceIdForSong(song) != null -> {
                val channelResult = getChannelSongAudioUrl(
                    song = song,
                    sourceId = customSourceIdForSong(song).orEmpty(),
                    sideEffects = sideEffects
                )
                if (channelResult is SongUrlResult.Failure) {
                    // 渠道脚本 + 同名换源都拿不到时，重复整轮只会再次等待，直接结束重试
                    channelPermanentFailure = true
                }
                channelResult
            }
            else -> {
                val neteaseResult = try {
                    getNeteaseSongUrl(
                        song = song,
                        suppressError = suppressError,
                        sideEffects = sideEffects,
                        onPermanentFailure = { neteasePermanentFailure = true }
                    )
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    LyraLogger.w(
                        "LYRA-PlayerManager",
                        "网易云取链异常，改用自定义源试试: ${error.message}"
                    )
                    SongUrlResult.Failure
                }
                val isPreviewClip = neteaseResult is SongUrlResult.Success &&
                    neteaseResult.cacheKeyOverride
                        ?.startsWith(NETEASE_PREVIEW_CACHE_KEY_PREFIX) == true
                if (neteaseResult is SongUrlResult.Success && !isPreviewClip) {
                    neteaseResult
                } else {
                    // GD API 与自定义音源脚本互斥：
                    // GD 开启时由 GD 负责兜底，GD 关闭时才继续走自定义音源。
                    val fallbackResult = if (isGdApiEnabled()) {
                        getNeteaseGdFallbackAudioUrl(song = song)
                    } else {
                        getCustomSourceAudioUrl(
                            song = song,
                            sourceId = CUSTOM_SOURCE_NETEASE,
                            sideEffects = sideEffects,
                            maxQualityAttempts = NETEASE_CUSTOM_SOURCE_QUALITY_ATTEMPTS,
                            timeoutMs = NETEASE_CUSTOM_SOURCE_TIMEOUT_MS
                        )
                    }
                    if (fallbackResult is SongUrlResult.Success) {
                        fallbackResult
                    } else {
                        // 兜底都失败时直接播放试听片段，不再弹出“试听资源”提示。
                        (neteaseResult as? SongUrlResult.Success)
                            ?.copy(noticeMessage = null)
                            ?: neteaseResult
                    }
                }
            }
        }
    }

    return if (result is SongUrlResult.Failure && hasCachedData) {
        LyraLogger.d("LYRA-PlayerManager", "远端解析失败但缓存完整，回退到离线缓存地址: $cacheKey")
        val fallbackDescriptor = cache?.readCachedPlaybackDescriptor(cacheKey)
        val fallbackAudioInfo = fallbackDescriptor?.toPlaybackAudioInfo {
            getLocalizedString(it)
        }
        SongUrlResult.Success(
            url = "$OFFLINE_CACHE_URL_PREFIX$cacheKey",
            audioInfo = fallbackAudioInfo,
            mimeType = fallbackAudioInfo?.mimeType,
            expectedContentLength = fallbackDescriptor?.expectedContentLength,
            representationIdentity = fallbackDescriptor?.representationIdentity,
            cacheKeyOverride = cacheKey,
            durationMs = song.durationMs.takeIf { it > 0L }
        )
    } else {
        result
    }
}

private fun PlayerManager.prepareBiliPlaybackSkipsForResolvedPlayback(
    song: SongItem,
    playbackRequestTokenOverride: Long? = null
) {
    if (
        !isBiliTrack(song) ||
        _currentSongFlow.value?.sameIdentityAs(song) != true
    ) {
        return
    }
    val requestToken = playbackRequestTokenOverride ?: playbackRequestToken
    BiliSponsorBlockPlaybackController.prepareActiveBiliTrackTarget(
        song = song,
        requestToken = requestToken,
        scope = ioScope
    )
    BiliVideoSkipPlaybackController.prepareActiveBiliTrackTarget(
        song = song,
        requestToken = requestToken,
        scope = ioScope
    )
}


private fun String.toLocalPlaybackUri(): Uri? {
    return if (startsWith("/")) {
        Uri.fromFile(File(this))
    } else {
        runCatching { toUri() }.getOrNull()
    }
}

internal fun PlayerManager.shouldAttemptUrlRefresh(
    error: PlaybackException,
    song: SongItem?,
    isOfflineCache: Boolean
): Boolean {
    if (song == null) return false
    return shouldAttemptCachedPlaybackRepair(
        error = error,
        isOfflineCache = isOfflineCache,
        isYouTubeTrack = isYouTubeMusicTrack(song),
        isLocalSong = isLocalSong(song)
    )
}

internal fun shouldAttemptCachedPlaybackRepair(
    error: PlaybackException,
    isOfflineCache: Boolean,
    isYouTubeTrack: Boolean,
    isLocalSong: Boolean
): Boolean {
    if (isOfflineCache) return true
    if (isLocalSong) return false
    if (isYouTubeTrack) {
        return shouldAttemptYouTubePlaybackRecovery(error, isOfflineCache)
    }
    return isRecoverableRemotePlaybackCacheError(error)
}

internal fun shouldInvalidateCachedResourceForPlaybackRecovery(
    error: PlaybackException
): Boolean = isRecoverableRemotePlaybackCacheError(error)

internal fun shouldInvalidateCacheForPlaybackRecovery(
    error: PlaybackException,
    isOfflineCache: Boolean
): Boolean {
    return shouldInvalidateCachedResourceForPlaybackRecovery(error) ||
        (isOfflineCache && isRecoverableCachedMediaFormatError(error))
}

internal fun shouldInvalidateCacheAfterPlaybackFailure(
    shouldInvalidateCache: Boolean,
    isOfflineCache: Boolean
): Boolean = shouldInvalidateCache && !isOfflineCache

internal fun isRecoverableRemotePlaybackCacheError(error: PlaybackException): Boolean {
    return error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
        (
            error.errorCode == PlaybackException.ERROR_CODE_TIMEOUT &&
                !shouldTreatPlaybackFailureAsTrackEnd(error)
            )
}

internal fun shouldTreatPlaybackFailureAsTrackEnd(error: PlaybackException): Boolean {
    return error.stuckPlayerExceptionOrNull()?.stuckType ==
        StuckPlayerException.STUCK_PLAYING_NOT_ENDING
}

internal fun shouldAdvanceAfterStuckTrackEnd(
    error: PlaybackException,
    playbackRequested: Boolean
): Boolean = playbackRequested && shouldTreatPlaybackFailureAsTrackEnd(error)

private fun PlaybackException.stuckPlayerExceptionOrNull(): StuckPlayerException? {
    return generateSequence(cause) { it.cause }
        .filterIsInstance<StuckPlayerException>()
        .firstOrNull()
}

internal fun PlayerManager.youtubePlaybackRecoveryStrategyForError(
    error: PlaybackException,
    song: SongItem?,
    isOfflineCache: Boolean
): YouTubePlaybackRecoveryStrategy? {
    if (song == null || !isYouTubeMusicTrack(song)) return null
    return resolveYouTubePlaybackRecoveryStrategy(error, isOfflineCache)
}

internal fun shouldAttemptYouTubePlaybackRecovery(
    error: PlaybackException,
    isOfflineCache: Boolean
): Boolean {
    return if (isOfflineCache) {
        isRecoverableYouTubeOfflineCacheError()
    } else {
        isRecoverableYouTubeRemotePlaybackError(error)
    }
}

internal fun resolveYouTubePlaybackRecoveryStrategy(
    error: PlaybackException,
    isOfflineCache: Boolean
): YouTubePlaybackRecoveryStrategy? {
    if (!shouldAttemptYouTubePlaybackRecovery(error, isOfflineCache)) return null
    return YouTubePlaybackRecoveryStrategy(
        preferredQualityOverride = YOUTUBE_STABLE_RECOVERY_QUALITY,
        requireDirect = shouldRequireDirectOnYouTubeRecovery(error, isOfflineCache),
        preferM4a = true,
        allowUnverifiedDirectFallback =
            error.errorCode != PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
    )
}

internal fun youtubePlaybackRecoveryStrategyForSeek(): YouTubePlaybackRecoveryStrategy {
    return YouTubePlaybackRecoveryStrategy(
        preferredQualityOverride = YOUTUBE_STABLE_RECOVERY_QUALITY,
        requireDirect = false,
        preferM4a = true,
        allowUnverifiedDirectFallback = false
    )
}

/**
 * googlevideo 拒了直链时不能再强制直链, 否则恢复必然拿回同一条 403
 *
 * 机房和被风控的出口上直链常年不可用, 只有放开这个约束才能落到 HLS
 */
internal fun shouldRequireDirectOnYouTubeRecovery(
    error: PlaybackException,
    isOfflineCache: Boolean
): Boolean {
    if (isOfflineCache) return true
    return error.errorCode != PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
}

internal fun offlineCacheKeyFromUrl(url: String?): String? {
    return url
        ?.takeIf { it.startsWith(OFFLINE_CACHE_URL_PREFIX) }
        ?.removePrefix(OFFLINE_CACHE_URL_PREFIX)
        ?.takeIf { it.isNotBlank() }
}

private fun isRecoverableYouTubeOfflineCacheError(): Boolean = true

private fun isRecoverableYouTubeRemotePlaybackError(error: PlaybackException): Boolean {
    return error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ||
        error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
        (
            error.errorCode == PlaybackException.ERROR_CODE_TIMEOUT &&
                !shouldTreatPlaybackFailureAsTrackEnd(error)
            ) ||
        isRecoverableCachedMediaFormatError(error)
}

private fun isRecoverableCachedMediaFormatError(error: PlaybackException): Boolean {
    return error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
        error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
        error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
        error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
        error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
        error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ||
        error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
}

private fun PlayerManager.resumePlaybackFallback(
    seekPositionMs: Long?,
    resumePlaybackAfterRefresh: Boolean
) {
    mainScope.launch {
        val resolvedSeekPositionMs = seekPositionMs?.coerceAtLeast(0L)
        if (resolvedSeekPositionMs != null) {
            player.seekTo(resolvedSeekPositionMs)
            _playbackPositionMs.value = resolvedSeekPositionMs
        }
        player.playWhenReady = resumePlaybackAfterRefresh
        if (resumePlaybackAfterRefresh) {
            applyAudioFocusPolicyOnMainThread()
            player.play()
            startProgressUpdates()
            schedulePlaybackStartupWatchdog(reason = "refresh_fallback")
        } else {
            player.pause()
        }
    }
}

internal fun PlayerManager.cancelUrlRefreshIfNotReusableForPendingLoad(
    song: SongItem,
    resumePositionMs: Long,
    requestGeneration: Long,
    commandSource: PlaybackCommandSource
) {
    val semantics = buildRefreshRequestSemantics(
        songKey = computeCacheKey(song),
        requestGeneration = requestGeneration,
        resumePositionMs = resumePositionMs,
        positionGeneration = playbackPositionGeneration,
        allowFallback = false,
        reason = "playAtIndex_pending_load",
        fallbackSeekPositionMs = null,
        resumePlaybackAfterRefresh = true,
        resumedPlaybackCommandSource = commandSource
    )
    if (urlRefreshController.cancelIfNotReusable(semantics)) {
        urlRefreshInProgress = false
    }
}

internal fun PlayerManager.refreshCurrentSongUrlImpl(
    resumePositionMs: Long,
    allowFallback: Boolean,
    reason: String,
    bypassCooldown: Boolean = false,
    fallbackSeekPositionMs: Long? = null,
    resumePlaybackAfterRefresh: Boolean = true,
    resumedPlaybackCommandSource: PlaybackCommandSource? = null,
    youtubeRecoveryStrategy: YouTubePlaybackRecoveryStrategy? = null,
    cacheKeyToInvalidateBeforeResolve: String? = null
) {
    val song = _currentSongFlow.value ?: return
    if (isLocalSong(song)) return
    LyraLogger.d(
        "LYRA-PlayerManager",
        "refreshCurrentSongUrl: song=${song.name}, resumePositionMs=$resumePositionMs, allowFallback=$allowFallback, reason=$reason, bypassCooldown=$bypassCooldown, resumePlaybackAfterRefresh=$resumePlaybackAfterRefresh, commandSource=$resumedPlaybackCommandSource, stack=[${debugStackHint()}]"
    )
    val cacheKey = computeCacheKey(
        song = song,
        youtubeQualityOverride = youtubeRecoveryStrategy?.preferredQualityOverride,
        youtubePreferM4aOverride = youtubeRecoveryStrategy?.preferM4a
    )
    val semantics = buildRefreshRequestSemantics(
        songKey = cacheKey,
        requestGeneration = playbackRequestToken,
        resumePositionMs = resumePositionMs,
        positionGeneration = playbackPositionGeneration,
        allowFallback = allowFallback,
        reason = reason,
        fallbackSeekPositionMs = fallbackSeekPositionMs,
        resumePlaybackAfterRefresh = resumePlaybackAfterRefresh,
        resumedPlaybackCommandSource = resumedPlaybackCommandSource,
        youtubeRecoveryStrategy = youtubeRecoveryStrategy,
        cacheKeyToInvalidateBeforeResolve = cacheKeyToInvalidateBeforeResolve
    )
    val now = SystemClock.elapsedRealtime()
    if (urlRefreshController.currentSemantics() == null &&
        !bypassCooldown &&
        lastUrlRefreshKey == cacheKey &&
        now - lastUrlRefreshAtMs < URL_REFRESH_COOLDOWN_MS
    ) {
        LyraLogger.w(
            "LYRA-PlayerManager",
            "refreshCurrentSongUrl throttled by cooldown: key=$cacheKey, reason=$reason, delta=${now - lastUrlRefreshAtMs}ms"
        )
        if (allowFallback) {
            resumePlaybackFallback(
                seekPositionMs = fallbackSeekPositionMs,
                resumePlaybackAfterRefresh = resumePlaybackAfterRefresh
            )
        } else {
            clearPendingSeekPosition()
            consecutivePlayFailures++
            postPlayerEvent(PlayerEvent.ShowError(getLocalizedString(R.string.player_playback_network_error)))
            if (consecutivePlayFailures >= MAX_CONSECUTIVE_FAILURES) {
                mainScope.launch { stopPlaybackPreservingQueue(clearMediaUrl = true) }
            } else {
                mainScope.launch {
                    advanceAfterPlaybackFailure(source = "refresh_cooldown")
                }
            }
        }
        return
    }

    lastUrlRefreshKey = cacheKey
    lastUrlRefreshAtMs = now

    var refreshJob: kotlinx.coroutines.Job? = null
    var refreshDeferred: CompletableDeferred<SongUrlResult>? = null
    val start = urlRefreshController.startOrReuse(
        semantics = semantics,
        start = {
            val deferred = CompletableDeferred<SongUrlResult>()
            refreshDeferred = deferred
            val job = ioScope.launch(start = CoroutineStart.LAZY) {
                runRefreshOperation(
                    semantics = semantics,
                    song = song,
                    deferred = deferred
                )
            }
            refreshJob = job
            PlayerManager.UrlRefreshOperation(
                semantics = semantics,
                deferred = deferred,
                job = job
            )
        },
        cancel = {
            refreshJob?.cancel()
            refreshDeferred?.let { RefreshDeferredCompletion(it).cancel() }
        },
        fallback = {}
    )
    if (!start.startedNew) {
        ioScope.launch {
            runCatching { start.operation.deferred.await() }
        }
        return
    }
    if (!urlRefreshController.isCurrent(semantics)) {
        start.operation.job.cancel()
        RefreshDeferredCompletion(start.operation.deferred).cancel()
        return
    }
    urlRefreshInProgress = true
    start.operation.job.start()
}

private suspend fun PlayerManager.runRefreshOperation(
    semantics: RefreshRequestSemantics,
    song: SongItem,
    deferred: CompletableDeferred<SongUrlResult>
) {
    try {
        LyraLogger.d("LYRA-PlayerManager", "Refreshing stream url (${semantics.reason}): ${semantics.songKey}")
        semantics.cacheKeyToInvalidateBeforeResolve?.let { staleCacheKey ->
            invalidateCachedResourceBeforeResolve(
                cacheKey = staleCacheKey,
                reason = semantics.reason,
                shouldApplyMutation = { canApplyRefreshResult(semantics, song) }
            )
        }
        val result = resolveSongUrl(
            song = song,
            forceRefresh = isYouTubeMusicTrack(song),
            youtubeRecoveryStrategy = semantics.youtubeRecoveryStrategy,
            sideEffects = RefreshResolverSideEffects(refreshSideEffectGate(semantics, song)),
            playbackRequestTokenOverride = semantics.requestGeneration,
            shouldApplyCacheMutation = { canApplyRefreshResult(semantics, song) }
        )
        deferred.complete(result)
        handleRefreshResult(semantics, song, result)
    } catch (error: CancellationException) {
        RefreshDeferredCompletion(deferred).cancel(error)
    } catch (error: Exception) {
        RefreshDeferredCompletion(deferred).completeExceptionally(error)
        LyraLogger.e("LYRA-PlayerManager", "refresh stream url failed (${semantics.reason})", error)
        handleRefreshResult(semantics, song, SongUrlResult.Failure)
    } finally {
        if (urlRefreshController.isCurrent(semantics)) {
            urlRefreshController.clear(semantics)
            urlRefreshInProgress = false
        } else {
            urlRefreshController.clear(semantics)
        }
    }
}

private suspend fun PlayerManager.handleRefreshResult(
    semantics: RefreshRequestSemantics,
    song: SongItem,
    result: SongUrlResult
) {
    val accepted = canApplyRefreshResult(semantics, song)
    when {
        result is SongUrlResult.Success -> {
            val action = resolveRefreshApplyAction(
                accepted = accepted,
                resultKind = RefreshResultKind.SUCCESS
            )
            val gate = refreshSideEffectGate(semantics, song)
            if (!action.updateDuration ||
                !RefreshResultSideEffects(gate).updateDuration {
                    maybeUpdateSongDuration(song, result.durationMs ?: 0L)
                }
            ) return
            withContext(Dispatchers.Main) {
                val applied = applyResolvedMediaItem(
                    gate = gate,
                    semantics = semantics,
                    song = _currentSongFlow.value ?: song,
                    result = result,
                    mimeType = result.mimeType,
                    expectedContentLength = result.expectedContentLength,
                    audioInfo = result.audioInfo,
                    cacheKeyOverride = result.cacheKeyOverride,
                    resumePositionMs = semantics.resumePositionMs,
                    resumePlaybackAfterRefresh = semantics.resumePlaybackAfterRefresh
                )
                if (!applied) return@withContext
                if (!gate.runMutation { consecutivePlayFailures = 0 }) return@withContext
                if (
                    semantics.resumePlaybackAfterRefresh &&
                    semantics.resumedPlaybackCommandSource == PlaybackCommandSource.LOCAL
                ) {
                    gate.runMutation {
                        emitPlaybackCommand(
                            type = "PLAY",
                            source = semantics.resumedPlaybackCommandSource,
                            positionMs = semantics.resumePositionMs.coerceAtLeast(0L),
                            currentIndex = currentIndex
                        )
                    }
                }
            }
        }
        semantics.allowFallback -> {
            val action = resolveRefreshApplyAction(
                accepted = accepted,
                resultKind = RefreshResultKind.FALLBACK
            )
            if (action.fallbackPlayPause) {
                withContext(Dispatchers.Main) {
                    val gate = refreshSideEffectGate(semantics, song)
                    val resolvedSeekPositionMs = semantics.fallbackSeekPositionMs?.coerceAtLeast(0L)
                    if (resolvedSeekPositionMs != null) {
                        if (!gate.runMutation {
                                player.seekTo(resolvedSeekPositionMs)
                                _playbackPositionMs.value = resolvedSeekPositionMs
                            }
                        ) return@withContext
                    }
                    gate.runMutation {
                        player.playWhenReady = semantics.resumePlaybackAfterRefresh
                        if (semantics.resumePlaybackAfterRefresh) {
                            applyAudioFocusPolicyOnMainThread()
                            player.play()
                            startProgressUpdates()
                        } else {
                            player.pause()
                        }
                    }
                }
            }
        }
        else -> {
            val action = resolveRefreshApplyAction(
                accepted = accepted,
                resultKind = RefreshResultKind.FAILURE
            )
            if (!action.emitFailureError) return
            val gate = refreshSideEffectGate(semantics, song)
            if (!gate.runMutation { clearPendingSeekPosition() }) return
            if (!gate.runMutation {
                    postPlayerEvent(PlayerEvent.ShowError(getLocalizedString(R.string.player_playback_network_error)))
                }
            ) return
            withContext(Dispatchers.Main) {
                refreshSideEffectGate(semantics, song).runMutation {
                    pause(commandSource = PlaybackCommandSource.REMOTE_SYNC)
                }
            }
        }
    }
}

private fun PlayerManager.refreshSideEffectGate(
    semantics: RefreshRequestSemantics,
    song: SongItem
) = RefreshSideEffectGate { canApplyRefreshResult(semantics, song) }

private fun PlayerManager.canApplyRefreshResult(
    semantics: RefreshRequestSemantics,
    song: SongItem
): Boolean {
    return _currentSongFlow.value?.sameIdentityAs(song) == true &&
        shouldApplyRefreshResult(
            owner = semantics,
            current = semantics.copy(requestGeneration = playbackRequestToken),
            currentRequestGeneration = playbackRequestToken,
            ownerActive = urlRefreshController.isCurrent(semantics)
        )
}

private fun buildRefreshRequestSemantics(
    songKey: String,
    requestGeneration: Long,
    resumePositionMs: Long,
    positionGeneration: Long,
    allowFallback: Boolean,
    reason: String,
    fallbackSeekPositionMs: Long?,
    resumePlaybackAfterRefresh: Boolean,
    resumedPlaybackCommandSource: PlaybackCommandSource?,
    youtubeRecoveryStrategy: YouTubePlaybackRecoveryStrategy? = null,
    cacheKeyToInvalidateBeforeResolve: String? = null
) = RefreshRequestSemantics(
    songKey = songKey,
    requestGeneration = requestGeneration,
    resumePositionMs = resumePositionMs.coerceAtLeast(0L),
    positionGeneration = positionGeneration,
    fallbackSeekPositionMs = fallbackSeekPositionMs?.coerceAtLeast(0L),
    resumePlaybackAfterRefresh = resumePlaybackAfterRefresh,
    allowFallback = allowFallback,
    reason = reason,
    resumedPlaybackCommandSource = resumedPlaybackCommandSource,
    youtubeRecoveryStrategy = youtubeRecoveryStrategy,
    cacheKeyToInvalidateBeforeResolve = cacheKeyToInvalidateBeforeResolve
)

private suspend fun PlayerManager.applyResolvedMediaItem(
    gate: RefreshSideEffectGate,
    semantics: RefreshRequestSemantics,
    song: SongItem,
    result: SongUrlResult.Success,
    mimeType: String?,
    expectedContentLength: Long?,
    audioInfo: PlaybackAudioInfo?,
    cacheKeyOverride: String?,
    resumePositionMs: Long,
    resumePlaybackAfterRefresh: Boolean
): Boolean {
    if (!gate.runMutation {}) return false

    val cacheKey = cacheKeyOverride ?: computeCacheKey(song)
    configureActivePlaybackCandidates(
        result = result,
        resumePositionMs = resumePositionMs,
        commandSource = semantics.resumedPlaybackCommandSource ?: PlaybackCommandSource.LOCAL,
        resetRecoveryAttempts = !semantics.reason.startsWith("startup_stall_") &&
            !semantics.reason.startsWith("runtime_stall_")
    )
    val selectedCandidate = currentPlaybackCandidate()
    val selectedUrl = selectedCandidate?.url ?: result.url
    val selectedAudioInfo = selectedCandidate?.audioInfo ?: audioInfo
    val selectedMimeType = selectedCandidate?.mimeType ?: mimeType
    val selectedExpectedContentLength =
        selectedCandidate?.expectedContentLength ?: expectedContentLength
    val selectedRepresentationIdentity =
        selectedCandidate?.representationIdentity ?: result.representationIdentity
    val cacheSynchronization = synchronizeCachedPlaybackDescriptor(
        cacheKey = cacheKey,
        audioInfo = selectedAudioInfo,
        expectedContentLength = selectedExpectedContentLength,
        representationIdentity = selectedRepresentationIdentity,
        shouldApplyMutation = { gate.runMutation {} }
    )
    if (!gate.runMutation {}) return false
    val mediaItem = buildMediaItem(
        song = song,
        url = selectedUrl,
        cacheKey = cacheKey,
        mimeType = selectedMimeType,
        allowCustomCacheKey = cacheSynchronization.allowsCustomCacheKey()
    )

    if (!gate.runMutation { _currentMediaUrl.value = selectedUrl }) return false
    if (!gate.runMutation { _currentPlaybackAudioInfo.value = selectedAudioInfo }) return false
    if (!gate.runMutation { currentMediaUrlResolvedAtMs = SystemClock.elapsedRealtime() }) return false
    if (!gate.runSuspendingMutation { persistState() }) return false

    var applied = false
    withContext(Dispatchers.Main) {
        val observedPositionBelongsToRequestedMedia =
            loadedMediaRequestToken == semantics.requestGeneration
        if (!gate.runMutation {
                updateAudioOffloadPreferences("refreshed_stream_source")
            }
        ) return@withContext
        if (!gate.runMutation {
                preparePlayerForManagedStart(
                    resolvePlaybackStartPlan(shouldFadeIn = false, fadeDurationMs = 0L)
                )
            }
        ) return@withContext
        if (!gate.runMutation { resetTrackEndDeduplicationState() }) return@withContext
        if (!gate.runMutation { applyWakeModeForPlaybackUrl(selectedUrl) }) return@withContext
        if (!gate.runMutation { player.setMediaItem(mediaItem) }) return@withContext
        if (!gate.runMutation { loadedMediaRequestToken = semantics.requestGeneration }) return@withContext
        if (!gate.runMutation { pendingMediaLoadActive = false }) return@withContext
        if (!gate.runMutation { syncExoRepeatMode() }) return@withContext
        val startPositionMs = resolveRefreshedMediaStartPosition(
            pendingSeekPositionMs = pendingSeekPositionOrNull(),
            requestedResumePositionMs = resumePositionMs,
            observedPlaybackPositionMs = _playbackPositionMs.value,
            requestedPositionGeneration = semantics.positionGeneration,
            currentPositionGeneration = playbackPositionGeneration,
            observedPositionBelongsToRequestedMedia = observedPositionBelongsToRequestedMedia
        )
        if (startPositionMs > 0) {
            if (!gate.runMutation {
                    player.seekTo(startPositionMs)
                    _playbackPositionMs.value = startPositionMs
                }
            ) return@withContext
        }
        if (!gate.runMutation { resetPlaybackProgressAdvanceBaseline(startPositionMs) }) return@withContext
        if (!gate.runMutation { clearPendingSeekPosition() }) return@withContext
        if (!gate.runMutation { player.prepare() }) return@withContext
        if (!gate.runMutation { player.playWhenReady = resumePlaybackAfterRefresh }) return@withContext
        if (!gate.runMutation {
                if (resumePlaybackAfterRefresh) {
                    applyAudioFocusPolicyOnMainThread()
                    player.play()
                    startProgressUpdates()
                    schedulePlaybackStartupWatchdog(reason = "refresh_applied")
                } else {
                    player.pause()
                }
            }
        ) return@withContext
        applied = true
    }
    return applied
}

private fun PlayerManager.checkLocalCache(
    song: SongItem,
    sideEffects: RefreshResolverSideEffects = RefreshResolverSideEffects()
): SongUrlResult? {
    val context = application
    if (!AudioDownloadManager.mayHaveIndexedLocalDownload(context, song)) {
        return null
    }
    val localReference = AudioDownloadManager.getLocalPlaybackUri(context, song) ?: return null
    if (!isReadableLocalMediaUri(localReference)) {
        LyraLogger.w(
            "LYRA-PlayerManager",
            "checkLocalCache: 命中不可读本地引用，回退远端解析 song=${song.name}, reference=$localReference"
        )
        sideEffects.scanLocalFiles {
            GlobalDownloadManager.scanLocalFiles(context, forceRefresh = true)
        }
        return null
    }
    val durationMs = if (song.durationMs <= 0L) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            val localUri = localReference.toUri()
            when (localUri.scheme?.lowercase()) {
                "content", "android.resource" -> retriever.setDataSource(context, localUri)
                "file" -> retriever.setDataSource(localUri.path)
                null, "" -> retriever.setDataSource(localReference)
                else -> retriever.setDataSource(context, localUri)
            }
            retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    } else {
        null
    }
    val localAudioInfo = buildLocalPlaybackAudioInfo(localReference.toUri(), application)
    return SongUrlResult.Success(
        url = localReference,
        durationMs = durationMs,
        audioInfo = mergeLocalPlaybackAudioInfoWithRemoteQuality(
            localAudioInfo = localAudioInfo,
            previousAudioInfo = _currentPlaybackAudioInfo.value
                ?.takeIf { _currentSongFlow.value?.sameIdentityAs(song) == true }
        )
    )
}

internal fun PlayerManager.inspectExoPlayerCache(
    cacheKey: String
): CachedResourceIntegrity {
    val mediaCache = cache ?: return CachedResourceIntegrity(false, false, 0L)
    return try {
        val cachedSpans = mediaCache.getCachedSpans(cacheKey)
        if (cachedSpans.isEmpty()) {
            return CachedResourceIntegrity(false, false, 0L)
        }

        val contentLength = ContentMetadata.getContentLength(
            mediaCache.getContentMetadata(cacheKey)
        )
        if (contentLength <= 0L) {
            LyraLogger.d("LYRA-PlayerManager", "缓存命中但缺少内容长度，视为未完成缓存: $cacheKey")
            return CachedResourceIntegrity(false, false, 0L)
        }

        val integrity = inspectCachedResourceSpans(cachedSpans, contentLength)
        when {
            integrity.requiresRepair -> LyraLogger.w(
                "LYRA-PlayerManager",
                "缓存 span 文件缺失或长度异常，标记为损坏: key=$cacheKey, " +
                    "covered=${integrity.coveredLength}/$contentLength"
            )

            integrity.isComplete -> LyraLogger.d(
                "LYRA-PlayerManager",
                "缓存完整可用: $cacheKey, length=$contentLength, spans=${cachedSpans.size}"
            )

            else -> LyraLogger.d(
                "LYRA-PlayerManager",
                "缓存未完整覆盖: $cacheKey, " +
                    "covered=${integrity.coveredLength}/$contentLength"
            )
        }
        integrity
    } catch (e: Exception) {
        LyraLogger.w("LYRA-PlayerManager", "检查缓存完整性失败: ${e.message}")
        CachedResourceIntegrity(false, true, 0L)
    }
}

internal fun PlayerManager.hasCompleteExoPlayerCache(cacheKey: String): Boolean {
    return inspectExoPlayerCache(cacheKey).isComplete
}

internal suspend fun PlayerManager.prepareExoPlayerCacheForPrefetch(
    cacheKey: String,
    shouldApplyMutation: () -> Boolean = { true }
): CachePrefetchReadiness {
    val currentCache = cache ?: return CachePrefetchReadiness.UNAVAILABLE

    if (loadPlaybackCacheKeySafety(cacheKey)) {
        if (cache !== currentCache) return CachePrefetchReadiness.UNAVAILABLE
        return if (
            invalidateCachedResourceForPlaybackRecovery(
                cacheKey = cacheKey,
                reason = "prefetch_unsafe_cache_key",
                shouldApplyMutation = shouldApplyMutation
            )
        ) {
            CachePrefetchReadiness.READY_FOR_PREFETCH
        } else {
            CachePrefetchReadiness.UNAVAILABLE
        }
    }

    val integrity = inspectExoPlayerCache(cacheKey)
    if (cache !== currentCache) return CachePrefetchReadiness.UNAVAILABLE
    if (integrity.isComplete) return CachePrefetchReadiness.COMPLETE
    if (!integrity.requiresRepair) return CachePrefetchReadiness.READY_FOR_PREFETCH

    return if (
        invalidateCachedResourceForPlaybackRecovery(
            cacheKey = cacheKey,
            reason = "prefetch_integrity_check",
            shouldApplyMutation = shouldApplyMutation
        )
    ) {
        CachePrefetchReadiness.READY_FOR_PREFETCH
    } else {
        CachePrefetchReadiness.UNAVAILABLE
    }
}

internal suspend fun PlayerManager.invalidateMismatchedCachedResource(
    cacheKey: String,
    expectedContentLength: Long?,
    shouldApplyMutation: () -> Boolean = { true }
) = withContext(Dispatchers.IO) {
    val expectedLength = expectedContentLength?.takeIf { it > 0L } ?: return@withContext
    val mediaCache = cache ?: return@withContext

    try {
        val cachedSpans = mediaCache.getCachedSpans(cacheKey)
        if (cachedSpans.isEmpty()) return@withContext

        val cachedContentLength = ContentMetadata.getContentLength(
            mediaCache.getContentMetadata(cacheKey)
        )
        if (!shouldReplaceCachedPreviewResource(cachedContentLength, expectedLength)) {
            return@withContext
        }

        LyraLogger.w(
            "LYRA-PlayerManager",
            "缓存疑似预览片段，移除旧缓存以便重新拉取完整资源: key=$cacheKey, cached=$cachedContentLength, expected=$expectedLength"
        )
        if (!shouldApplyMutation()) return@withContext
        mediaCache.removeResource(cacheKey)
    } catch (e: Exception) {
        LyraLogger.w(
            "LYRA-PlayerManager",
            "移除不匹配缓存失败: key=$cacheKey, error=${e.message}"
        )
    }
}

internal fun PlayerManager.currentPlaybackCacheKeyForRecovery(): String? {
    offlineCacheKeyFromUrl(_currentMediaUrl.value)?.let { return it }

    if (isPlayerInitialized()) {
        player.currentMediaItem?.localConfiguration?.customCacheKey
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
    }

    val song = _currentSongFlow.value ?: return null
    if (isLocalSong(song)) return null
    return currentPlaybackCandidate()?.cacheKeyOverride
        ?.takeIf { it.isNotBlank() }
        ?: computeCacheKey(song).takeIf { it.isNotBlank() }
}

internal suspend fun PlayerManager.invalidateCachedResourceForPlaybackRecovery(
    cacheKey: String,
    reason: String,
    shouldApplyMutation: () -> Boolean = { true }
): Boolean = withContext(Dispatchers.IO) {
    if (cacheKey.isBlank()) return@withContext false
    val mediaCache = cache ?: return@withContext false
    if (cache !== mediaCache || !shouldApplyMutation()) return@withContext false
    try {
        if (cache !== mediaCache || !shouldApplyMutation()) return@withContext false
        mediaCache.removeResource(cacheKey)
        if (cache !== mediaCache) return@withContext false
        if (mediaCache.getCachedSpans(cacheKey).isNotEmpty()) {
            markPlaybackCacheKeyUnsafe(mediaCache, cacheKey)
            LyraLogger.w(
                "LYRA-PlayerManager",
                "异常播放缓存未完全移除: key=$cacheKey, reason=$reason"
            )
            return@withContext false
        }
        if (cache !== mediaCache || !shouldApplyMutation()) return@withContext false
        if (!clearPlaybackCacheKeyUnsafe(mediaCache, cacheKey)) {
            LyraLogger.w(
                "LYRA-PlayerManager",
                "异常播放缓存标记未清除，跳过缓存复用: key=$cacheKey, reason=$reason"
            )
            return@withContext false
        }
        LyraLogger.w(
            "LYRA-PlayerManager",
            "已移除异常播放缓存: key=$cacheKey, reason=$reason"
        )
        true
    } catch (e: Exception) {
        if (cache === mediaCache) {
            markPlaybackCacheKeyUnsafe(mediaCache, cacheKey)
        }
        LyraLogger.w(
            "LYRA-PlayerManager",
            "移除异常播放缓存失败: key=$cacheKey, reason=$reason, error=${e.message}"
        )
        false
    }
}

private suspend fun PlayerManager.invalidateCachedResourceBeforeResolve(
    cacheKey: String,
    reason: String,
    shouldApplyMutation: () -> Boolean
) = invalidateCachedResourceForPlaybackRecovery(cacheKey, reason, shouldApplyMutation)

/** 是否为 GD 音乐台歌曲（通过 GD API 取直链）。 */
private fun isGdStudioTrack(song: SongItem): Boolean =
    song.channelId?.trim()?.lowercase() == MusicSourceIds.GDSTUDIO &&
        !song.audioId.isNullOrBlank()

/**
 * 用 GD 音乐台 API 解析播放直链。
 *
 * GD 的曲目 ID 与背后的音乐源绑定，因此 [SongItem.subAudioId] 保存了
 * netease / joox / bilibili 等 GD 音源名，缺省回退到 netease。
 */
private suspend fun PlayerManager.getGdStudioAudioUrl(
    song: SongItem,
    sideEffects: RefreshResolverSideEffects = RefreshResolverSideEffects()
): SongUrlResult = withContext(Dispatchers.IO) {
    val trackId = song.audioId?.takeIf { it.isNotBlank() }
    if (trackId == null) {
        LyraLogger.w("LYRA-PlayerManager", "GD 歌曲缺少曲目 ID: song=${song.name}")
        return@withContext SongUrlResult.Failure
    }
    val gdSource = song.subAudioId?.takeIf { it.isNotBlank() } ?: "netease"
    val api = GdStudioApiClient(AppContainer.sharedOkHttpClient)
    var url: String? = null
    for (br in intArrayOf(320, 128)) {
        url = api.resolveAudioUrl(source = gdSource, trackId = trackId, br = br)
        if (!url.isNullOrBlank()) break
    }
    if (url.isNullOrBlank()) {
        LyraLogger.w(
            "LYRA-PlayerManager",
            "GD 取直链失败: source=$gdSource id=$trackId song=${song.name}"
        )
        SongUrlResult.Failure
    } else {
        LyraLogger.i(
            "LYRA-PlayerManager",
            "GD 取直链成功: source=$gdSource id=$trackId song=${song.name} url=${url.take(160)}"
        )
        SongUrlResult.Success(url = url)
    }
}

/**
 * 用自定义源（音源脚本）解析播放直链。
 */
internal suspend fun PlayerManager.getCustomSourceAudioUrl(
    song: SongItem,
    sourceId: String,
    sideEffects: RefreshResolverSideEffects = RefreshResolverSideEffects(),
    maxQualityAttempts: Int = Int.MAX_VALUE,
    timeoutMs: Long = CUSTOM_SOURCE_TOTAL_TIMEOUT_MS
): SongUrlResult = withContext(Dispatchers.IO) {
    if (sourceId.isBlank()) {
        return@withContext SongUrlResult.Failure
    }
    val musicInfo = buildCustomSourceMusicInfo(sourceId, song)
    val resolvedUrl = withTimeoutOrNull(timeoutMs.coerceAtLeast(1L)) {
        val qualities = CustomSourceManager.availableQualities(application, sourceId)
        if (qualities.isEmpty()) {
            LyraLogger.i(
                "LYRA-PlayerManager",
                "自定义源未声明该音源，无法取直链: source=$sourceId song=${song.name}"
            )
            return@withTimeoutOrNull null
        }
        val attemptedQualities = qualities.take(maxQualityAttempts.coerceAtLeast(1))
        var resolved: String? = null
        for (quality in attemptedQualities) {
            val qualityStartedAt = android.os.SystemClock.elapsedRealtime()
            val url = withTimeoutOrNull(CUSTOM_SOURCE_QUALITY_TIMEOUT_MS) {
                CustomSourceManager.resolveMusicUrl(
                    context = application,
                    sourceId = sourceId,
                    quality = quality,
                    musicInfo = musicInfo
                )
            }
            if (!url.isNullOrBlank()) {
                LyraLogger.i(
                    "LYRA-PlayerManager",
                    "自定义源取直链成功: source=$sourceId quality=$quality song=${song.name} " +
                        "url=${url.take(160)}"
                )
                resolved = url
                break
            }
            LyraLogger.w(
                "LYRA-PlayerManager",
                "自定义源音质尝试失败: source=$sourceId quality=$quality song=${song.name}, " +
                    "耗时=${android.os.SystemClock.elapsedRealtime() - qualityStartedAt}ms"
            )
        }
        resolved
    }
    if (!resolvedUrl.isNullOrBlank()) {
        return@withContext SongUrlResult.Success(
            url = resolvedUrl,
            durationMs = song.durationMs.takeIf { it > 0L }
        )
    }
    LyraLogger.w(
        "LYRA-PlayerManager",
        "自定义源没有返回可用直链: source=$sourceId song=${song.name}, " +
            "超时上限=${timeoutMs}ms, 最多音质次数=$maxQualityAttempts"
    )
    SongUrlResult.Failure
}

/**
 * 网易云歌曲在「仅试听 / 无权限」时，尝试用 GD API 取完整直链。
 *
 * 取不到时静默返回失败，由调用方决定是否回落到试听片段。
 */
internal suspend fun PlayerManager.getNeteaseGdFallbackAudioUrl(
    song: SongItem
): SongUrlResult = withContext(Dispatchers.IO) {
    if (!isNeteaseTrack(song) || song.id <= 0L) return@withContext SongUrlResult.Failure
    val api = GdStudioApiClient(AppContainer.sharedOkHttpClient)
    var url: String? = null
    for (br in intArrayOf(320, 128)) {
        url = api.resolveAudioUrl(
            source = MusicSourceIds.NETEASE,
            trackId = song.id.toString(),
            br = br
        )
        if (!url.isNullOrBlank()) break
    }
    if (url.isNullOrBlank()) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "GD 网易云兜底未取到直链: id=${song.id} song=${song.name}"
        )
        return@withContext SongUrlResult.Failure
    }
    LyraLogger.i(
        "LYRA-PlayerManager",
        "GD 网易云兜底成功: id=${song.id} song=${song.name} url=${url.take(160)}"
    )
    SongUrlResult.Success(url = url)
}

/** GD API 当前是否开启（用于决定网易云兜底由 GD 还是自定义音源负责）。 */
internal suspend fun isGdApiEnabled(): Boolean = runCatching {
    AppContainer.settingsRepo.gdApiEnabledFlow.first()
}.getOrDefault(false)

private fun isNeteaseTrack(song: SongItem): Boolean {
    val channel = song.channelId?.trim()
    return channel.isNullOrEmpty() ||
        channel.equals(MusicSourceIds.NETEASE, ignoreCase = true)
}

internal suspend fun PlayerManager.getNeteaseSongUrl(
    song: SongItem,
    suppressError: Boolean = false,
    sideEffects: RefreshResolverSideEffects = RefreshResolverSideEffects(),
    allowLocalFallback: Boolean = true,
    allowPreviewFallback: Boolean = true,
    allowAutoSourceFallback: Boolean = true,
    onPermanentFailure: () -> Unit = {}
): SongUrlResult = withContext(Dispatchers.IO) {
    try {
        val effectiveQuality = effectiveNeteaseQuality()
        val qualityCandidates = buildNeteaseQualityCandidates(effectiveQuality)
        var previewFallback: SongUrlResult.Success? = null
        var lastFailureReason: NeteasePlaybackResponseParser.FailureReason? = null
        var requiresLogin = false

        for ((index, quality) in qualityCandidates.withIndex()) {
            val resp = neteaseClient.getSongDownloadUrl(
                song.id,
                level = quality
            )
            LyraLogger.d("LYRA-PlayerManager", "id=${song.id}, level=$quality, resp=$resp")

            when (val parsed = NeteasePlaybackResponseParser.parsePlayback(resp, song.durationMs)) {
                is NeteasePlaybackResponseParser.PlaybackResult.RequiresLogin -> {
                    requiresLogin = true
                    if (shouldRetryNeteaseWithLowerQualityAfterLogin(
                            qualityIndex = index,
                            lastQualityIndex = qualityCandidates.lastIndex
                        )
                    ) {
                        LyraLogger.w(
                            "LYRA-PlayerManager",
                            "当前音质需要登录，继续尝试更低音质: id=${song.id}, level=$quality"
                        )
                        continue
                    }
                    break
                }

                is NeteasePlaybackResponseParser.PlaybackResult.Success -> {
                    val success = buildNeteaseSuccessResult(
                        parsed = parsed,
                        resolvedQualityKey = quality,
                        fallbackDurationMs = song.durationMs,
                        getLocalizedString = { getLocalizedString(it) }
                    ).let { result ->
                        if (parsed.notice == NeteasePlaybackResponseParser.Notice.PREVIEW_CLIP) {
                            result.copy(
                                cacheKeyOverride = buildNeteasePreviewCacheKey(
                                    songId = song.id,
                                    preferredQuality = quality
                                )
                            )
                        } else {
                            result
                        }
                    }
                    if (parsed.notice != NeteasePlaybackResponseParser.Notice.PREVIEW_CLIP) {
                        if (quality != effectiveQuality) {
                            LyraLogger.w(
                                "LYRA-PlayerManager",
                                "当前音质不可用，已自动降级: id=${song.id}, preferred=$effectiveQuality, resolved=$quality"
                            )
                        }
                        return@withContext success
                    }

                    previewFallback = success
                    if (index < qualityCandidates.lastIndex) {
                        LyraLogger.w(
                            "LYRA-PlayerManager",
                            "当前音质仅返回试听片段，继续尝试更低音质: id=${song.id}, level=$quality"
                        )
                        continue
                    }
                }

                is NeteasePlaybackResponseParser.PlaybackResult.Failure -> {
                    lastFailureReason = parsed.reason
                    if (index < qualityCandidates.lastIndex &&
                        shouldRetryNeteaseWithLowerQuality(parsed.reason)
                    ) {
                        LyraLogger.w(
                            "LYRA-PlayerManager",
                            "当前音质不可播放，继续尝试更低音质: id=${song.id}, level=$quality, reason=${parsed.reason}"
                        )
                        continue
                    }
                    break
                }
            }
        }

        // 本地兜底覆盖试听 / 无权限 / 加载失败等所有未拿到可播直链的出口；
        // 开关与匹配命中在 tryResolveNeteaseMatchedLocalSource 内部判定。
        if (allowLocalFallback) {
            tryResolveNeteaseMatchedLocalSource(song)?.let {
                return@withContext it
            }
        }
        // 本地文件优先，其次按歌名 / 歌手 / 时长到其他平台找同一首歌；
        // 自动换源开关的判定在各自 resolver 内部完成。
        if (allowAutoSourceFallback) {
            tryResolveNeteaseAutoCrossPlatformSource(song, sideEffects)?.let {
                return@withContext it
            }
            if (previewFallback != null ||
                lastFailureReason == NeteasePlaybackResponseParser.FailureReason.NO_PERMISSION
            ) {
                tryResolveNeteaseAutoBiliSource(song, sideEffects)?.let {
                    return@withContext it
                }
            }
        }

        if (allowPreviewFallback) {
            // 试听片段静默播放，不再弹出“仅试听 / 受限资源”提示
            previewFallback?.let { return@withContext it.copy(noticeMessage = null) }
        }

        if (requiresLogin) {
            onPermanentFailure()
            return@withContext SongUrlResult.RequiresLogin
        }

        // 已经拿到明确的平台响应（无权限 / 无直链）时，重复请求不会改变结果
        onPermanentFailure()
        SongUrlResult.Failure
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        LyraLogger.e("LYRA-PlayerManager", "Failed to get url", e)
        SongUrlResult.Failure
    }
}

private suspend fun PlayerManager.getBiliAudioUrl(
    song: SongItem,
    suppressError: Boolean = false,
    sideEffects: RefreshResolverSideEffects = RefreshResolverSideEffects(),
    playbackRequestTokenOverride: Long? = null
): SongUrlResult = withContext(Dispatchers.IO) {
    try {
        val resolved = resolveBiliSong(song, biliClient)
        if (resolved == null || resolved.cid == 0L) {
            return@withContext SongUrlResult.Failure
        }

        if (_currentSongFlow.value?.sameIdentityAs(song) == true) {
            val requestToken = playbackRequestTokenOverride ?: playbackRequestToken
            BiliSponsorBlockPlaybackController.onBiliTrackResolved(
                song = song,
                target = BiliSponsorBlockTarget(
                    bvid = resolved.videoInfo.bvid,
                    cid = resolved.cid,
                    durationMs = resolved.pageInfo
                        ?.durationSec
                        ?.toLong()
                        ?.times(1_000L)
                        ?.takeIf { it > 0L }
                        ?: song.durationMs
                ),
                requestToken = requestToken,
                scope = ioScope
            )
            BiliVideoSkipPlaybackController.onBiliTrackResolved(
                song = song,
                target = BiliVideoSkipTarget(
                    bvid = resolved.videoInfo.bvid,
                    cid = resolved.cid
                ),
                requestToken = requestToken
            )
        }

        val (availableStreams, audioStream) = biliRepo.getAudioWithDecision(
            resolved.videoInfo.bvid,
            resolved.cid,
            preferredKeyOverride = effectiveBiliQuality()
        )

        if (audioStream?.url != null) {
            LyraLogger.d("LYRA-PlayerManager-BiliAudioUrl", audioStream.url)
            SongUrlResult.Success(
                url = audioStream.url,
                candidateUrls = audioStream.candidateUrls,
                mimeType = audioStream.mimeType,
                expectedContentLength = null,
                audioInfo = buildBiliPlaybackAudioInfo(audioStream, availableStreams) {
                    getLocalizedString(it)
                },
                representationIdentity = buildBiliRepresentationIdentity(audioStream)
            )
        } else {
            SongUrlResult.Failure
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        LyraLogger.e("LYRA-PlayerManager", "Failed to get Bili play url", e)
        SongUrlResult.Failure
    }
}

private suspend fun PlayerManager.getYouTubeMusicAudioUrl(
    song: SongItem,
    suppressError: Boolean = false,
    forceRefresh: Boolean = false,
    youtubeRecoveryStrategy: YouTubePlaybackRecoveryStrategy? = null,
    sideEffects: RefreshResolverSideEffects = RefreshResolverSideEffects()
): SongUrlResult = withContext(Dispatchers.IO) {
    val videoId = extractYouTubeMusicVideoId(song.mediaUri)
    if (videoId.isNullOrBlank()) {
        return@withContext SongUrlResult.Failure
    }

    val resolveStartedAtMs = System.currentTimeMillis()
    try {
        val preferredQuality = youtubeRecoveryStrategy?.preferredQualityOverride
            ?: effectiveYouTubeQuality()
        val requireDirect = youtubeRecoveryStrategy?.requireDirect ?: false
        // 首播保留用户选择; 出错恢复时才切到更稳的 m4a 直链
        val preferM4a = youtubeRecoveryStrategy?.preferM4a ?: false
        val resolvedPlayableAudio = youtubeMusicPlaybackRepository.getBestPlayableAudio(
            videoId = videoId,
            preferredQualityOverride = preferredQuality,
            forceRefresh = forceRefresh,
            requireDirect = requireDirect,
            preferM4a = preferM4a,
            shareInFlight = youtubeRecoveryStrategy == null,
            allowUnverifiedDirectFallback =
                youtubeRecoveryStrategy?.allowUnverifiedDirectFallback ?: true
        )?.takeIf { it.url.isNotBlank() }
        if (resolvedPlayableAudio != null) {
            sideEffects.updateDuration {
                maybeUpdateSongDuration(song, resolvedPlayableAudio.durationMs)
            }
            LyraLogger.d(
                "LYRA-PlayerManager",
                "Resolved YouTube Music stream: videoId=$videoId, quality=$preferredQuality, recovery=${youtubeRecoveryStrategy != null}, preferM4a=$preferM4a, type=${resolvedPlayableAudio.streamType}, mime=${resolvedPlayableAudio.mimeType}, contentLength=${resolvedPlayableAudio.contentLength}, elapsedMs=${System.currentTimeMillis() - resolveStartedAtMs}"
            )
            SongUrlResult.Success(
                url = resolvedPlayableAudio.url,
                durationMs = resolvedPlayableAudio.durationMs.takeIf { it > 0L },
                mimeType = resolvedPlayableAudio.mimeType,
                expectedContentLength = resolvedPlayableAudio.contentLength,
                audioInfo = buildYouTubePlaybackAudioInfo(resolvedPlayableAudio) {
                    getLocalizedString(it)
                },
                representationIdentity = buildYouTubeRepresentationIdentity(resolvedPlayableAudio),
                cacheKeyOverride = youtubeRecoveryStrategy?.let { strategy ->
                    computeYouTubeCacheKey(
                        videoId = videoId,
                        preferredQuality = strategy.preferredQualityOverride,
                        preferM4a = strategy.preferM4a
                    )
                }
            )
        } else {
            LyraLogger.w(
                "LYRA-PlayerManager",
                "Resolve YouTube Music stream returned empty: videoId=$videoId, elapsedMs=${System.currentTimeMillis() - resolveStartedAtMs}"
            )
            SongUrlResult.Failure
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        LyraLogger.e(
            "LYRA-PlayerManager",
            "Failed to get YouTube Music play url: videoId=$videoId, elapsedMs=${System.currentTimeMillis() - resolveStartedAtMs}",
            e
        )
        SongUrlResult.Failure
    }
}
