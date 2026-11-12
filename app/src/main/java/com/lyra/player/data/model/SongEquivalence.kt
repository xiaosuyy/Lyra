package com.lyra.player.data.model

/*
 * LyraPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 LyraPlayer developers
 * https://github.com/cwuom/Lyra
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
 * File: com.lyra.player.data.model/SongEquivalence
 * Created: 2026/11/12
 */

import com.lyra.player.data.local.media.LocalSongSupport
import java.util.Locale
import kotlin.math.abs

/**
 * 收藏用的同曲判定。
 *
 * 同一首歌在网易云 / QQ 音乐等平台的 id 不同, [sameIdentityAs] 无法跨源匹配,
 * 这里用「标题 + 主歌手 + 时长近似」补一层。仅用于「我喜欢的音乐」,
 * 队列去重、缓存等仍依赖精确身份。
 */
fun SongItem.sameFavoriteSongAs(other: SongItem?): Boolean {
    if (other == null) return false
    if (sameIdentityAs(other)) return true
    if (LocalSongSupport.isLocalSong(this, null) || LocalSongSupport.isLocalSong(other, null)) {
        return false
    }
    val key = favoriteSongMatchKey() ?: return false
    if (key != other.favoriteSongMatchKey()) return false
    return favoriteDurationsMatch(durationMs, other.durationMs)
}

/** 跨平台收藏匹配键（标题|主歌手，已归一化）；信息不足时返回 null。 */
fun SongItem.favoriteSongMatchKey(): String? {
    if (LocalSongSupport.isLocalSong(this, null)) return null
    val normalizedTitle = name
        .replace(favoriteWhitespaceRegex, " ")
        .trim()
        .lowercase(Locale.US)
    if (normalizedTitle.isEmpty()) return null
    val primaryArtist = artist
        .split(favoriteArtistSeparatorRegex)
        .firstOrNull()
        ?.replace(favoriteWhitespaceRegex, " ")
        ?.trim()
        ?.lowercase(Locale.US)
        .orEmpty()
    if (primaryArtist.isEmpty()) return null
    return "$normalizedTitle|$primaryArtist"
}

/** 批量判断歌曲是否已在收藏中，避免逐条 O(n) 扫描。 */
class FavoriteSongEquivalenceIndex(songs: List<SongItem>) {
    private val identities: Set<SongIdentity> = songs.mapTo(HashSet(songs.size)) { it.identity() }
    private val durationsByKey: Map<String, List<Long>> = run {
        val index = mutableMapOf<String, MutableList<Long>>()
        songs.forEach { song ->
            val key = song.favoriteSongMatchKey() ?: return@forEach
            index.getOrPut(key) { mutableListOf() }.add(song.durationMs)
        }
        index
    }

    fun contains(song: SongItem): Boolean {
        if (song.identity() in identities) return true
        val key = song.favoriteSongMatchKey() ?: return false
        val durations = durationsByKey[key] ?: return false
        return durations.any { favoriteDurationsMatch(it, song.durationMs) }
    }
}

private const val FAVORITE_SONG_DURATION_TOLERANCE_MS = 3_000L

private val favoriteWhitespaceRegex = Regex("[\\s\\u3000]+")

// 多歌手 / 合唱时取第一位主歌手；feat. / ft. / vs. 之后视为客串
private val favoriteArtistSeparatorRegex =
    Regex("[/、,，&;；]|\\s+(?i:feat\\.?|ft\\.?|vs\\.?)\\s+")

private fun favoriteDurationsMatch(firstMs: Long, secondMs: Long): Boolean {
    if (firstMs <= 0L || secondMs <= 0L) return true
    return abs(firstMs - secondMs) <= FAVORITE_SONG_DURATION_TOLERANCE_MS
}
