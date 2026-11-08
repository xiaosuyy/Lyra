package com.lyra.player.core.player.resolver.qishui

/*
 * LyraPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 LyraPlayer developers
 * https://github.com/cwuom/Lyra
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.lyra.player.core.player.resolver.qishui/PlayerManagerQishuiResolver
 * Created: 2026/11/8
 */

import com.lyra.player.R
import com.lyra.player.core.api.qishui.QishuiTrack
import com.lyra.player.core.customsource.CUSTOM_SOURCE_NETEASE
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.core.player.model.PlayerEvent
import com.lyra.player.core.player.model.SongUrlResult
import com.lyra.player.core.player.policy.refresh.RefreshResolverSideEffects
import com.lyra.player.core.player.url.NETEASE_CUSTOM_SOURCE_QUALITY_ATTEMPTS
import com.lyra.player.core.player.url.NETEASE_CUSTOM_SOURCE_TIMEOUT_MS
import com.lyra.player.core.player.url.getCustomSourceAudioUrl
import com.lyra.player.core.player.url.getNeteaseGdFallbackAudioUrl
import com.lyra.player.core.player.url.getNeteaseSongUrl
import com.lyra.player.core.player.url.isGdApiEnabled
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.data.model.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue

internal const val QISHUI_PREVIEW_CACHE_KEY_PREFIX = "qishui-preview-v1-"

private const val NETEASE_MATCH_LIMIT = 10
private const val NETEASE_MATCH_MIN_SCORE = 70
private const val MATCH_CACHE_LIMIT = 200
private val qishuiMatchCache = ConcurrentHashMap<String, QishuiNeteaseMatch>()

private data class QishuiNeteaseMatch(
    val songId: Long,
    val name: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String?
)

internal fun buildQishuiPlaybackCacheKey(trackId: String): String {
    val safe = trackId.trim().ifBlank { "unknown" }
    return "qishui-$safe-hq"
}

internal fun buildQishuiPreviewCacheKey(trackId: String): String =
    "$QISHUI_PREVIEW_CACHE_KEY_PREFIX${trackId.trim()}"

/**
 * 汽水音乐取链：优先完整播放，只拿到试听片段时走与心动模式相同的
 * 网易云匹配 → GD / 自定义音源兜底链路。
 */
internal suspend fun PlayerManager.getQishuiAudioUrl(
    song: SongItem,
    suppressError: Boolean = false,
    sideEffects: RefreshResolverSideEffects = RefreshResolverSideEffects()
): SongUrlResult = withContext(Dispatchers.IO) {
    val trackId = song.audioId?.takeIf(String::isNotBlank)
    if (trackId == null) {
        LyraLogger.w("LYRA-Qishui", "汽水歌曲缺少曲目 ID: song=${song.name}")
        return@withContext failure(suppressError, sideEffects)
    }
    val track = QishuiTrack(
        id = trackId,
        name = song.name,
        artists = song.artist,
        album = song.album,
        coverUrl = song.coverUrl,
        durationMs = song.durationMs
    )
    val audio = try {
        AppContainer.qishuiApiClient.resolveAudio(track)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: IOException) {
        LyraLogger.w(
            "LYRA-Qishui",
            "取链失败: song=${song.name}, error=${error.message.orEmpty()}"
        )
        null
    }
    if (audio == null) {
        return@withContext failure(suppressError, sideEffects)
    }
    if (!audio.isPreview) {
        LyraLogger.d(
            "LYRA-Qishui",
            "完整播放: song=${song.name}, bitrate=${audio.bitrate}"
        )
        return@withContext SongUrlResult.Success(
            url = audio.url,
            durationMs = audio.durationMs ?: song.durationMs.takeIf { it > 0L }
        )
    }

    LyraLogger.d("LYRA-Qishui", "仅返回试听片段，尝试网易云兜底: song=${song.name}")
    val fallback = resolveQishuiPreviewFallback(song)
    fallback ?: SongUrlResult.Success(
        url = audio.url,
        durationMs = audio.durationMs,
        isPreviewClip = true,
        cacheKeyOverride = buildQishuiPreviewCacheKey(trackId),
        noticeMessage = null
    )
}

private suspend fun PlayerManager.resolveQishuiPreviewFallback(
    song: SongItem
): SongUrlResult? {
    val match = findQishuiNeteaseMatch(song) ?: return null
    val matchedSong = SongItem(
        id = match.songId,
        name = match.name,
        artist = match.artist,
        album = match.album,
        albumId = 0L,
        durationMs = match.durationMs.takeIf { it > 0L } ?: song.durationMs,
        coverUrl = match.coverUrl,
        channelId = MusicSourceIds.NETEASE
    )
    val direct = getNeteaseSongUrl(
        song = matchedSong,
        suppressError = true,
        allowLocalFallback = true,
        allowPreviewFallback = false,
        allowAutoBiliFallback = false
    )
    if (direct is SongUrlResult.Success && !direct.isPreviewClip) {
        LyraLogger.i(
            "LYRA-Qishui",
            "网易云直连命中: qishui=${song.name}, netease=${match.songId}"
        )
        return direct
    }
    val fallback = if (isGdApiEnabled()) {
        getNeteaseGdFallbackAudioUrl(matchedSong)
    } else {
        getCustomSourceAudioUrl(
            song = matchedSong,
            sourceId = CUSTOM_SOURCE_NETEASE,
            maxQualityAttempts = NETEASE_CUSTOM_SOURCE_QUALITY_ATTEMPTS,
            timeoutMs = NETEASE_CUSTOM_SOURCE_TIMEOUT_MS
        )
    }
    if (fallback is SongUrlResult.Success) {
        LyraLogger.i(
            "LYRA-Qishui",
            "网易云兜底命中: qishui=${song.name}, netease=${match.songId}"
        )
        return fallback
    }
    return null
}

private suspend fun findQishuiNeteaseMatch(song: SongItem): QishuiNeteaseMatch? {
    val trackId = song.audioId.orEmpty()
    qishuiMatchCache[trackId]?.let { return it }
    val title = song.name.trim()
    val artist = song.artist.trim()
    val queries = listOf("$title $artist", title)
        .map { it.trim() }
        .filter(String::isNotBlank)
        .distinct()
    for (query in queries) {
        val match = runCatching { searchNeteaseMatch(song, query) }
            .getOrElse { error ->
                if (error is CancellationException) throw error
                LyraLogger.w("LYRA-Qishui", "网易云搜索失败: query=$query, ${error.message}")
                null
            } ?: continue
        if (qishuiMatchCache.size >= MATCH_CACHE_LIMIT) {
            qishuiMatchCache.keys.firstOrNull()?.let(qishuiMatchCache::remove)
        }
        if (trackId.isNotBlank()) qishuiMatchCache[trackId] = match
        return match
    }
    return null
}

private suspend fun searchNeteaseMatch(
    song: SongItem,
    query: String
): QishuiNeteaseMatch? {
    val response = AppContainer.neteaseClient.searchSongsCancellable(
        keyword = query,
        limit = NETEASE_MATCH_LIMIT,
        usePersistedCookies = true
    )
    val songs = JSONObject(response).optJSONObject("result")?.optJSONArray("songs")
        ?: return null
    return (0 until songs.length())
        .mapNotNull { index -> songs.optJSONObject(index) }
        .mapNotNull { json ->
            val id = json.optLong("id", 0L).takeIf { it > 0L } ?: return@mapNotNull null
            val name = json.optString("name").takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val artist = runCatching {
                val artists = json.optJSONArray("ar")
                (0 until (artists?.length() ?: 0))
                    .mapNotNull { artists?.optJSONObject(it)?.optString("name") }
                    .filter(String::isNotBlank)
                    .joinToString("/")
            }.getOrDefault("")
            val albumJson = json.optJSONObject("al")
            QishuiNeteaseMatch(
                songId = id,
                name = name,
                artist = artist,
                album = albumJson?.optString("name").orEmpty(),
                durationMs = json.optLong("dt", 0L),
                coverUrl = albumJson?.optString("picUrl")?.takeIf(String::isNotBlank)
            )
        }
        .map { match -> match to scoreMatch(song, match) }
        .filter { it.second >= NETEASE_MATCH_MIN_SCORE }
        .maxByOrNull { it.second }
        ?.first
}

private fun scoreMatch(song: SongItem, match: QishuiNeteaseMatch): Int {
    val normalizedTitle = normalizeText(match.name)
    val normalizedArtist = normalizeText(match.artist)
    val songTitle = song.name
    val songArtist = song.artist
    var score = 0
    score += if (normalizedTitle.contains(compact(songTitle))) 55 else tokenOverlap(normalizedTitle, songTitle)
    if (songArtist.isNotBlank() &&
        (normalizedTitle.contains(compact(songArtist)) || normalizedArtist.contains(compact(songArtist)))
    ) {
        score += 25
    }
    if (song.durationMs > 0L && match.durationMs > 0L) {
        val diff = (match.durationMs - song.durationMs).absoluteValue
        score += when {
            diff <= 8_000L -> 30
            diff <= 20_000L -> 22
            diff <= 45_000L -> 12
            match.durationMs > song.durationMs * 2 -> -15
            else -> 0
        }
    }
    return score
}

private val nonTextRegex = Regex("[^\\p{L}\\p{N}]+")
private val whitespaceRegex = Regex("\\s+")

private fun normalizeText(value: String): String =
    whitespaceRegex.replace(nonTextRegex.replace(value.lowercase(), " "), " ").trim()

private fun compact(value: String): String = normalizeText(value).replace(" ", "")

private fun tokenOverlap(normalizedText: String, rawNeedle: String): Int {
    val tokens = normalizeText(rawNeedle).split(' ').filter { it.length >= 2 }
    if (tokens.isEmpty()) return 0
    val hits = tokens.count(normalizedText::contains)
    return when (hits) {
        tokens.size -> 35
        in 1 until tokens.size -> 18
        else -> 0
    }
}

private fun PlayerManager.failure(
    suppressError: Boolean,
    sideEffects: RefreshResolverSideEffects
): SongUrlResult {
    if (!suppressError) {
        sideEffects.emitError {
            postPlayerEvent(
                PlayerEvent.ShowError(getLocalizedString(R.string.error_no_play_url))
            )
        }
    }
    return SongUrlResult.Failure
}
