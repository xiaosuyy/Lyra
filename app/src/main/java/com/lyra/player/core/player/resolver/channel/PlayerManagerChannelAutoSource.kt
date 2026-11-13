package com.lyra.player.core.player.resolver.channel

import kotlin.math.absoluteValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.core.player.model.SongUrlResult
import com.lyra.player.core.player.policy.refresh.RefreshResolverSideEffects
import com.lyra.player.core.player.resolver.netease.matchesNeteaseLocalFallbackMetadata
import com.lyra.player.core.player.resolver.netease.tryResolveNeteaseMatchedLocalSource
import com.lyra.player.core.player.resolver.netease.tryResolveAutoCrossPlatformSource
import com.lyra.player.core.player.url.getCustomSourceAudioUrl
import com.lyra.player.core.player.url.getNeteaseSongUrl
import com.lyra.player.data.model.SongItem
import android.os.SystemClock

private const val TAG = "LYRA-PlayerManager"

private const val CHANNEL_CUSTOM_SOURCE_TIMEOUT_MS = 12_000L
private const val CHANNEL_CUSTOM_SOURCE_QUALITY_ATTEMPTS = 2
private const val CHANNEL_NETEASE_TIMEOUT_MS = 6_000L
private const val CHANNEL_NETEASE_SEARCH_TIMEOUT_MS = 4_000L
private const val CHANNEL_NETEASE_SEARCH_LIMIT = 20
private const val CHANNEL_TOTAL_TIMEOUT_MS = 14_000L

private data class NeteaseSongCandidate(
    val id: Long,
    val name: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String?
) {
    fun toSongItem(): SongItem = SongItem(
        id = id,
        name = name,
        artist = artist,
        album = album,
        albumId = 0L,
        durationMs = durationMs,
        coverUrl = coverUrl
    )
}

internal suspend fun PlayerManager.getChannelSongAudioUrl(
    song: SongItem,
    sourceId: String,
    sideEffects: RefreshResolverSideEffects = RefreshResolverSideEffects()
): SongUrlResult = withContext(Dispatchers.IO) {
    if (sourceId.isBlank()) return@withContext SongUrlResult.Failure

    tryResolveNeteaseMatchedLocalSource(song, allowIdMatch = false)?.let {
        LyraLogger.i(TAG, "渠道歌曲命中本地同曲，直接播放: ${song.name}")
        return@withContext it
    }

    val startedAt = SystemClock.elapsedRealtime()
    val result = withTimeoutOrNull(CHANNEL_TOTAL_TIMEOUT_MS) {
        // 原平台与备用同曲并行竞速，谁先拿到可用直链就用谁；
        // 备用同曲在返回前做一次轻量音频身份校验，避免换到另一段录音。
        raceChannelAudioSources(
            script = {
                getCustomSourceAudioUrl(
                    song = song,
                    sourceId = sourceId,
                    sideEffects = sideEffects,
                    maxQualityAttempts = CHANNEL_CUSTOM_SOURCE_QUALITY_ATTEMPTS,
                    timeoutMs = CHANNEL_CUSTOM_SOURCE_TIMEOUT_MS
                )
            },
            matched = {
                raceChannelAudioSources(
                    script = {
                        tryResolveAutoCrossPlatformSource(song, sideEffects) ?: SongUrlResult.Failure
                    },
                    matched = {
                        withTimeoutOrNull(CHANNEL_NETEASE_TIMEOUT_MS) {
                            resolveMatchedNeteaseSong(song, sideEffects)
                        }
                    }
                )
            }
        )
    } ?: SongUrlResult.Failure
    LyraLogger.i(
        TAG,
        "渠道歌曲取链完成: source=$sourceId, song=${song.name}, " +
            "success=${result is SongUrlResult.Success}, 耗时=${SystemClock.elapsedRealtime() - startedAt}ms"
    )
    result
}

internal suspend fun raceChannelAudioSources(
    script: suspend () -> SongUrlResult,
    matched: suspend () -> SongUrlResult?
): SongUrlResult = coroutineScope {
    val tasks = listOf(script, matched).map { resolve ->
        async {
            try {
                resolve()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                LyraLogger.w(TAG, "渠道歌曲取链异常: ${error.message}")
                null
            }
        }
    }
    val pending = tasks.toMutableList()
    try {
        while (pending.isNotEmpty()) {
            val (completed, result) = select {
                pending.forEach { task -> task.onAwait { task to it } }
            }
            pending.remove(completed)
            if (result is SongUrlResult.Success) return@coroutineScope result
        }
        SongUrlResult.Failure
    } finally {
        tasks.forEach { it.cancel() }
    }
}

private suspend fun PlayerManager.resolveMatchedNeteaseSong(
    song: SongItem,
    sideEffects: RefreshResolverSideEffects
): SongUrlResult? {
    val targetName = (song.originalName ?: song.name).trim()
    val targetArtist = (song.originalArtist ?: song.artist).trim()
    if (targetName.isBlank() || targetArtist.isBlank()) return null

    val keyword = "$targetName $targetArtist"
    val raw = withTimeoutOrNull(CHANNEL_NETEASE_SEARCH_TIMEOUT_MS) {
        withContext(Dispatchers.IO) {
            runCatching {
                neteaseClient.searchSongsCancellable(
                    keyword = keyword,
                    limit = CHANNEL_NETEASE_SEARCH_LIMIT,
                    usePersistedCookies = true
                )
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                LyraLogger.w(TAG, "渠道歌曲网易云搜索失败: keyword=$keyword, error=${error.message}")
                null
            }
        }
    } ?: return null

    val candidates = parseNeteaseSearchCandidates(raw)
    if (candidates.isEmpty()) {
        LyraLogger.d(TAG, "渠道歌曲网易云搜索无结果: keyword=$keyword")
        return null
    }

    val best = candidates
        .filter { matchesNeteaseLocalFallbackMetadata(song, it.toSongItem()) }
        .minByOrNull { (it.durationMs - song.durationMs).absoluteValue }
        ?: run {
        LyraLogger.w(
            TAG,
            "渠道歌曲网易云无匹配结果: song=$targetName/$targetArtist, 候选=${candidates.size}"
        )
        return null
    }

    val resolved = getNeteaseSongUrl(
        song = best.toSongItem(),
        suppressError = true,
        sideEffects = sideEffects,
        allowLocalFallback = false,
        allowPreviewFallback = false,
        allowAutoSourceFallback = false
    )
    return (resolved as? SongUrlResult.Success)?.copy(noticeMessage = null)?.also {
        LyraLogger.i(TAG, "渠道歌曲同曲匹配成功: song=${song.name}, matchedId=${best.id}")
    }
}

private fun parseNeteaseSearchCandidates(raw: String?): List<NeteaseSongCandidate> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val response = JSONObject(raw)
        if (response.optInt("code") != 200) {
            LyraLogger.w(
                TAG,
                "渠道歌曲匹配搜索失败: code=${response.optInt("code")}, " +
                    "message=${response.optString("message", response.optString("msg"))}"
            )
            return@runCatching emptyList()
        }
        val songs = response.optJSONObject("result")?.optJSONArray("songs")
            ?: return@runCatching emptyList()
        buildList(songs.length()) {
            for (index in 0 until songs.length()) {
                val item = songs.optJSONObject(index) ?: continue
                val id = item.optLong("id", 0L)
                if (id <= 0L) continue
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                val artistArray = item.optJSONArray("ar")
                    ?: item.optJSONArray("artists")
                    ?: continue
                val artist = (0 until artistArray.length())
                    .mapNotNull { artistIndex ->
                        artistArray.optJSONObject(artistIndex)
                            ?.optString("name")
                            ?.takeIf { it.isNotBlank() }
                    }
                    .joinToString(" / ")
                val albumObject = item.optJSONObject("al") ?: item.optJSONObject("album")
                val durationMs = item.optLong("dt", 0L)
                    .takeIf { it > 0L }
                    ?: item.optLong("duration", 0L)
                add(
                    NeteaseSongCandidate(
                        id = id,
                        name = name,
                        artist = artist,
                        album = albumObject?.optString("name").orEmpty(),
                        durationMs = durationMs,
                        coverUrl = albumObject?.optString("picUrl")?.takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }.getOrElse { error ->
        LyraLogger.w(TAG, "渠道歌曲网易云搜索结果解析失败: ${error.message}")
        emptyList()
    }
}