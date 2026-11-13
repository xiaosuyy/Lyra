package com.lyra.player.core.player.resolver.netease

import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.absoluteValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import android.os.SystemClock
import com.lyra.player.core.api.search.CustomChannelSearchApi
import com.lyra.player.core.api.search.CustomChannelSong
import com.lyra.player.core.customsource.CustomSourceManager
import com.lyra.player.core.customsource.customSourceIdForChannelId
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.core.player.model.SongUrlResult
import com.lyra.player.core.player.policy.refresh.RefreshResolverSideEffects
import com.lyra.player.core.player.url.getCustomSourceAudioUrl
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.data.model.SongItem

private const val CROSS_PLATFORM_SEARCH_TIMEOUT_MS = 5_000L
private const val CROSS_PLATFORM_TOTAL_TIMEOUT_MS = 11_000L
private const val CROSS_PLATFORM_CANDIDATE_TIMEOUT_MS = 10_000L
private const val CROSS_PLATFORM_QUALITY_ATTEMPTS = 2
private const val CROSS_PLATFORM_MAX_RESOLVE_ATTEMPTS = 3
/** 歌名、歌手都命中后仍允许的时长误差 */
internal const val CROSS_PLATFORM_DURATION_TOLERANCE_MS = 8_000L

/** 按优先尝试的顺序排列，脚本缺失的渠道会在检索前剔除 */
private val crossPlatformSourceIds = listOf(
    MusicSourceIds.KUWO,
    MusicSourceIds.KUGOU,
    MusicSourceIds.MIGU,
    MusicSourceIds.TENCENT
)

private val crossPlatformNonTextRegex = Regex("[^\\p{L}\\p{N}]+")
private val crossPlatformWhitespaceRegex = Regex("\\s+")
private val crossPlatformArtistSplitRegex =
    Regex("[/、,，&＆;；]|\\b(?:feat|ft|with)\\b\\.?", RegexOption.IGNORE_CASE)
private val crossPlatformArtistAliasRegex = Regex("[（(\\[【][^)）\\]】]*[)）\\]】]")

internal data class CrossPlatformSourceMatch(
    val sourceId: String,
    val candidate: CustomChannelSong,
    val score: Int
)

/**
 * 网易云拿不到完整音源时，用歌名 + 歌手 + 时长到其他渠道找同一首歌。
 *
 * 只接受三项元数据都对得上的结果，命中后交给自定义音源脚本取直链；
 * 任何一步失败都静默返回 null，由调用方决定是否回落到试听片段。
 */
internal suspend fun PlayerManager.tryResolveNeteaseAutoCrossPlatformSource(
    song: SongItem,
    sideEffects: RefreshResolverSideEffects
): SongUrlResult? {
    if (!neteaseAutoSourceSwitchEnabled) return null
    return tryResolveAutoCrossPlatformSource(song, sideEffects)
}

internal suspend fun PlayerManager.tryResolveAutoCrossPlatformSource(
    song: SongItem,
    sideEffects: RefreshResolverSideEffects
): SongUrlResult? {
    val originalSourceId = customSourceIdForChannelId(song.channelId.orEmpty())
    val targetName = (song.originalName ?: song.name).trim()
    val targetArtist = (song.originalArtist ?: song.artist).trim()
    if (targetName.isBlank() || targetArtist.isBlank()) return null

    val searchableSources = crossPlatformSourceIds.filter { channelId ->
        val scriptSourceId = customSourceIdForChannelId(channelId) ?: return@filter false
        if (scriptSourceId == originalSourceId) return@filter false
        CustomSourceManager.availableQualities(application, scriptSourceId).isNotEmpty()
    }
    if (searchableSources.isEmpty()) {
        LyraLogger.d(
            "LYRA-PlayerManager",
            "跨平台换源跳过：没有可用于 ${crossPlatformSourceIds.joinToString("/")} 的音源脚本"
        )
        return null
    }

    LyraLogger.w(
        "LYRA-PlayerManager",
        "尝试同曲备用音源: song=$targetName, artist=$targetArtist, " +
            "duration=${song.durationMs}, sources=${searchableSources.joinToString(",")}"
    )

    val searchStartedAt = SystemClock.elapsedRealtime()
    val api = CustomChannelSearchApi(AppContainer.sharedOkHttpClient)
    val keyword = "$targetName $targetArtist"
    val searched = withTimeoutOrNull(CROSS_PLATFORM_SEARCH_TIMEOUT_MS) {
        coroutineScope {
            searchableSources.map { sourceId ->
                async {
                    val songs = runCatching {
                        searchCrossPlatformSongs(api, sourceId, keyword)
                    }.getOrElse { error ->
                        if (error is CancellationException) throw error
                        LyraLogger.w(
                            "LYRA-PlayerManager",
                            "跨平台搜索失败: source=$sourceId, keyword=$keyword, error=${error.message}"
                        )
                        emptyList()
                    }
                    songs.map { sourceId to it }
                }
            }.awaitAll().flatten()
        }
    } ?: return null
    LyraLogger.i(
        "LYRA-PlayerManager",
        "跨平台搜索完成: keyword=$keyword, 结果=${searched.size}, " +
            "耗时=${SystemClock.elapsedRealtime() - searchStartedAt}ms"
    )

    // 每个平台只保留分数最高的一首，避免同一平台的不同版本占满全部尝试名额
    val matches = searched.mapNotNull { (sourceId, candidate) ->
        val score = scoreNeteaseAutoCrossPlatformCandidate(
            song = song,
            candidateName = candidate.name,
            candidateArtist = candidate.artist,
            candidateDurationMs = candidate.durationMs
        )
        if (score < 0) null else CrossPlatformSourceMatch(sourceId, candidate, score)
    }.filter { match ->
        originalSourceId == null || matchesNeteaseLocalFallbackMetadata(song, buildCrossPlatformSong(match))
    }.sortedWith(
        compareByDescending<CrossPlatformSourceMatch> { it.score }
            .thenBy { (it.candidate.durationMs - song.durationMs).absoluteValue }
    ).distinctBy { it.sourceId }
        .take(CROSS_PLATFORM_MAX_RESOLVE_ATTEMPTS)

    if (matches.isEmpty()) {
        LyraLogger.w(
            "LYRA-PlayerManager",
            "跨平台换源没有匹配结果: song=$targetName, artist=$targetArtist, " +
                "searched=${searched.size}"
        )
        return null
    }

    LyraLogger.i(
        "LYRA-PlayerManager",
        "跨平台候选: " + matches.joinToString("; ") {
            "${it.sourceId}:${it.candidate.name}/${it.candidate.artist}/" +
                "${it.candidate.durationMs}/score=${it.score}"
        }
    )

    // 各平台同时取链，谁先返回完整直链就用谁，避免慢脚本拖住后面的平台
    val resolveStartedAt = SystemClock.elapsedRealtime()
    return withTimeoutOrNull(CROSS_PLATFORM_TOTAL_TIMEOUT_MS) {
        coroutineScope {
            val pending = AtomicInteger(matches.size)
            val winner = CompletableDeferred<SongUrlResult.Success?>()
            matches.forEach { match ->
                launch {
                    val result = runCatching {
                        resolveCrossPlatformMatch(match, sideEffects)
                    }.getOrElse { error ->
                        if (error is CancellationException) throw error
                        LyraLogger.w(
                            "LYRA-PlayerManager",
                            "跨平台取链异常: source=${match.sourceId}, error=${error.message}"
                        )
                        null
                    }
                    if (result is SongUrlResult.Success) {
                        LyraLogger.w(
                            "LYRA-PlayerManager",
                            "跨平台换源成功: song=$targetName, source=${match.sourceId}, " +
                                "matched=${match.candidate.name}/${match.candidate.artist}/" +
                                "${match.candidate.durationMs}, score=${match.score}, " +
                                "耗时=${SystemClock.elapsedRealtime() - resolveStartedAt}ms"
                        )
                        winner.complete(result)
                    } else if (pending.decrementAndGet() == 0) {
                        winner.complete(null)
                    }
                }
            }
            val result = winner.await()
            coroutineContext[Job]?.cancelChildren()
            result
        }
    }
}

private suspend fun PlayerManager.resolveCrossPlatformMatch(
    match: CrossPlatformSourceMatch,
    sideEffects: RefreshResolverSideEffects
): SongUrlResult? {
    val scriptSourceId = customSourceIdForChannelId(match.sourceId) ?: return null
    val candidateSong = buildCrossPlatformSong(match)
    val result = getCustomSourceAudioUrl(
        song = candidateSong,
        sourceId = scriptSourceId,
        sideEffects = sideEffects,
        maxQualityAttempts = CROSS_PLATFORM_QUALITY_ATTEMPTS,
        timeoutMs = CROSS_PLATFORM_CANDIDATE_TIMEOUT_MS
    )
    return if (result is SongUrlResult.Success) {
        result.copy(
            durationMs = match.candidate.durationMs.takeIf { it > 0L } ?: result.durationMs,
            noticeMessage = null
        )
    } else {
        null
    }
}

private suspend fun searchCrossPlatformSongs(
    api: CustomChannelSearchApi,
    sourceId: String,
    keyword: String
): List<CustomChannelSong> = when (sourceId) {
    MusicSourceIds.TENCENT -> api.searchTencent(keyword)
    MusicSourceIds.KUGOU -> api.searchKugou(keyword)
    MusicSourceIds.KUWO -> api.searchKuwo(keyword)
    MusicSourceIds.MIGU -> api.searchMigu(keyword)
    else -> emptyList()
}

private fun buildCrossPlatformSong(match: CrossPlatformSourceMatch): SongItem {
    val candidate = match.candidate
    val stableId = CustomChannelSearchApi.stableChannelSongId(
        "${match.sourceId}|${candidate.platformId}"
    )
    return SongItem(
        id = stableId,
        name = candidate.name,
        artist = candidate.artist,
        album = candidate.album,
        albumId = candidate.albumId,
        durationMs = candidate.durationMs,
        coverUrl = candidate.coverUrl,
        channelId = match.sourceId,
        audioId = candidate.platformId,
        subAudioId = candidate.albumAudioId
    )
}

/**
 * 打分规则：歌名必须完全一致，歌手必须有一位命中，时长误差在容差内；三项全对才返回正分。
 *
 * 歌名不再用包含匹配：「100」会命中「100种生活」、「晴天」会命中「晴天 (Live)」，
 * 换源后播出来的就不是同一首歌了。
 *
 * 返回 -1 表示这条候选不可用。
 */
internal fun scoreNeteaseAutoCrossPlatformCandidate(
    song: SongItem,
    candidateName: String,
    candidateArtist: String,
    candidateDurationMs: Long
): Int {
    val targetTitle = compactCrossPlatformText(song.originalName ?: song.name)
    val candidateTitle = compactCrossPlatformText(candidateName)
    if (targetTitle.length < 2 || candidateTitle != targetTitle) return -1

    val targetArtists = crossPlatformArtistTokens(song.originalArtist ?: song.artist)
    val candidateArtists = crossPlatformArtistTokens(candidateArtist)
    if (targetArtists.isEmpty() || candidateArtists.isEmpty()) return -1
    if (targetArtists.none { it in candidateArtists }) return -1

    var durationScore = 10
    if (song.durationMs > 0L) {
        if (candidateDurationMs <= 0L) return -1
        val diffMs = (candidateDurationMs - song.durationMs).absoluteValue
        if (diffMs > CROSS_PLATFORM_DURATION_TOLERANCE_MS) return -1
        durationScore = when {
            diffMs <= 2_000L -> 30
            diffMs <= 5_000L -> 20
            else -> 12
        }
    }

    return 120 + durationScore
}

private fun normalizeCrossPlatformText(value: String): String {
    return crossPlatformWhitespaceRegex
        .replace(crossPlatformNonTextRegex.replace(value.lowercase(), " "), " ")
        .trim()
}

private fun compactCrossPlatformText(value: String): String {
    return normalizeCrossPlatformText(value).replace(" ", "")
}

/** 多歌手按 / 、 & feat. 等切分后逐位比较，避免「周杰」命中「周杰伦」这类包含匹配 */
private fun crossPlatformArtistTokens(value: String): List<String> {
    val withoutAlias = crossPlatformArtistAliasRegex.replace(value, " ")
    return withoutAlias.split(crossPlatformArtistSplitRegex)
        .map { compactCrossPlatformText(it) }
        .filter { it.isNotBlank() }
}
