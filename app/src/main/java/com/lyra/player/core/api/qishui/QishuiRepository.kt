package com.lyra.player.core.api.qishui

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
 * File: com.lyra.player.core.api.qishui/QishuiRepository
 * Created: 2026/11/8
 */

import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.data.model.SongItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException

internal enum class QishuiRecommendationMode(val storageKey: String) {
    DEFAULT("default"),
    FAMILIAR("familiar"),
    FRESH("fresh");

    companion object {
        fun fromStorageKey(value: String?): QishuiRecommendationMode =
            entries.firstOrNull { it.storageKey == value } ?: DEFAULT
    }
}

internal class QishuiLoginRequiredException : IOException("请先登录汽水音乐")

/**
 * 汽水音乐三种推荐模式。
 *
 * 官方 App 的“默认/熟悉/新鲜”没有可在网页端复用的公开接口
 * （`/luna/feed/song-tab` 仅接受 App 上下文），这里用同一账号会话
 * 的发现页电台与账号收藏/歌单组合出等效结果：
 * - 默认：账号个性化发现电台混合
 * - 熟悉：账号收藏与自建歌单里的歌曲
 * - 新鲜：跳过个性化最靠前的电台，从更靠后的电台取样
 */
internal class QishuiRepository(
    private val api: QishuiApiClient,
    private val sessionStore: com.lyra.player.data.auth.qishui.QishuiSessionStore
) {
    private val modeCursors = mutableMapOf<QishuiRecommendationMode, Int>()

    fun isLoggedIn(): Boolean = sessionStore.getSessionOnce().isLoggedIn

    fun onLoggedOut() {
        modeCursors.clear()
    }

    suspend fun loadTracks(
        mode: QishuiRecommendationMode,
        limit: Int = 30
    ): List<SongItem> {
        if (!isLoggedIn()) throw QishuiLoginRequiredException()
        val tracks = when (mode) {
            QishuiRecommendationMode.DEFAULT -> loadDefaultMode()
            QishuiRecommendationMode.FAMILIAR -> loadFamiliarMode()
            QishuiRecommendationMode.FRESH -> loadFreshMode()
        }
        val filtered = tracks
            .filter { it.durationMs <= 0L || it.durationMs >= MIN_TRACK_DURATION_MS }
            .distinctBy { it.id }
        LyraLogger.d(
            "LYRA-Qishui",
            "loadTracks: mode=${mode.storageKey}, raw=${tracks.size}, filtered=${filtered.size}"
        )
        if (filtered.isEmpty()) throw IOException("汽水音乐暂时没有可推荐的歌曲")
        return filtered.shuffled().take(limit).map(QishuiTrack::toSongItem)
    }

    private suspend fun loadDefaultMode(): List<QishuiTrack> {
        val radios = discoverRadiosSafely()
        return sampleRadios(radios, radioCount = RADIO_BATCH_SIZE).shuffled()
    }

    private suspend fun loadFamiliarMode(): List<QishuiTrack> {
        val familiar = mutableListOf<QishuiTrack>()
        runCatching { api.collectionTracks() }
            .onSuccess { familiar += it }
            .onFailure { logFailure("collection", it) }
        runCatching { api.userPlaylists() }
            .getOrNull()
            ?.filter { it.trackCount > 0 }
            ?.shuffled()
            ?.take(PLAYLIST_BATCH_SIZE)
            ?.forEach { playlist ->
                runCatching { api.playlistTracks(playlist.id, count = 30) }
                    .onSuccess { familiar += it }
                    .onFailure { logFailure("playlist:${playlist.id}", it) }
            }
        if (familiar.distinctBy { it.id }.size >= MIN_FAMILIAR_TRACKS) {
            return familiar
        }
        // 收藏为空时用账号个性化电台补足，避免熟悉模式直接空列表
        val radios = discoverRadiosSafely()
        familiar += sampleRadios(radios, radioCount = RADIO_BATCH_SIZE)
        return familiar
    }

    private suspend fun loadFreshMode(): List<QishuiTrack> {
        val radios = discoverRadiosSafely()
        if (radios.isEmpty()) return emptyList()
        val start = (radios.size / 2).coerceAtLeast(1)
        val freshPool = radios.drop(start).ifEmpty { radios }
        val offset = modeCursors[QishuiRecommendationMode.FRESH] ?: 0
        val ordered = freshPool.drop(offset % freshPool.size) + freshPool.take(offset % freshPool.size)
        modeCursors[QishuiRecommendationMode.FRESH] = offset + RADIO_BATCH_SIZE
        return sampleRadios(ordered, radioCount = RADIO_BATCH_SIZE, limitPerRadio = 12)
    }

    private suspend fun discoverRadiosSafely(): List<QishuiRadio> =
        runCatching { api.discoverRadios() }
            .onFailure { logFailure("discover", it) }
            .getOrDefault(emptyList())

    private suspend fun sampleRadios(
        radios: List<QishuiRadio>,
        radioCount: Int,
        limitPerRadio: Int = 10
    ): List<QishuiTrack> = coroutineScope {
        val picked = radios.shuffled().take(radioCount)
        val batches = picked.map { radio ->
            async {
                runCatching { api.radioTracks(radio.id, count = limitPerRadio) }
                    .onFailure { logFailure("radio:${radio.id}", it) }
                    .getOrDefault(emptyList())
            }
        }
        batches.awaitAll().flatten().distinctBy { it.id }
    }

    private fun logFailure(scope: String, error: Throwable) {
        LyraLogger.w("LYRA-Qishui", "load failed: $scope, ${error.message.orEmpty()}")
    }

    private companion object {
        const val MIN_TRACK_DURATION_MS = 50_000L
        const val RADIO_BATCH_SIZE = 6
        const val PLAYLIST_BATCH_SIZE = 3
        const val MIN_FAMILIAR_TRACKS = 10
    }
}
