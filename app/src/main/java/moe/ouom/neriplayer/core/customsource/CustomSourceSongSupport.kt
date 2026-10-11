package moe.ouom.neriplayer.core.customsource

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * File: moe.ouom.neriplayer.core.customsource/CustomSourceSongSupport
 * Created: 2026/9/24
 */

import moe.ouom.neriplayer.core.source.MusicSourceIds
import moe.ouom.neriplayer.data.model.SongItem
import org.json.JSONObject

/** 各平台在歌曲记录里的来源标记前缀 */
internal const val KUWO_SOURCE_TAG = "Kuwo"
internal const val KUGOU_SOURCE_TAG = "Kugou"
internal const val MIGU_SOURCE_TAG = "Migu"
internal const val TENCENT_SOURCE_TAG = "Tencent"

/** 自定义源使用的音源 id */
internal const val CUSTOM_SOURCE_KUWO = "kw"
internal const val CUSTOM_SOURCE_KUGOU = "kg"
internal const val CUSTOM_SOURCE_MIGU = "mg"
internal const val CUSTOM_SOURCE_NETEASE = "wy"
internal const val CUSTOM_SOURCE_QQ = "tx"

/**
 * 自定义源取链的音质优先级。
 *
 * 放在最前面的是"最容易直接播放成功"的音质：无损链接经常需要额外的请求头或会过期，
 * 先用 320k / 128k 保证能放，无损作为兜底。
 */
internal val customSourceQualityPreference =
    listOf("320k", "128k", "192k", "flac", "flac24bit")

/** 渠道音源在 [SongItem.channelId] 中使用的稳定标识（与 [MusicSourceIds] 对齐） */
internal const val CHANNEL_ID_KUWO = MusicSourceIds.KUWO
internal const val CHANNEL_ID_KUGOU = MusicSourceIds.KUGOU
internal const val CHANNEL_ID_MIGU = MusicSourceIds.MIGU
internal const val CHANNEL_ID_TENCENT = MusicSourceIds.TENCENT

/**
 * 平台 channelId → 自定义源音源 id。
 *
 * 这是判断歌曲来源的**首选**依据，使用明确的数据字段而不是字符串拼接。
 */
internal fun customSourceIdForChannelId(channelId: String?): String? =
    when (channelId?.trim()?.lowercase()) {
        CHANNEL_ID_KUWO -> CUSTOM_SOURCE_KUWO
        CHANNEL_ID_KUGOU -> CUSTOM_SOURCE_KUGOU
        CHANNEL_ID_MIGU -> CUSTOM_SOURCE_MIGU
        CHANNEL_ID_TENCENT -> CUSTOM_SOURCE_QQ
        else -> null
    }

/**
 * 旧数据兼容：早期版本把来源写在 album 前缀里（如 `Kuwo|专辑名`）。
 *
 * 新数据不再写入该前缀，仅用于让历史歌单 / 收藏 / 播放历史仍能正确取链。
 */
internal fun legacyCustomSourceIdFromAlbum(album: String?): String? {
    if (album.isNullOrBlank()) return null
    return when {
        album.startsWith("$KUWO_SOURCE_TAG|") -> CUSTOM_SOURCE_KUWO
        album.startsWith("$KUGOU_SOURCE_TAG|") -> CUSTOM_SOURCE_KUGOU
        album.startsWith("$MIGU_SOURCE_TAG|") -> CUSTOM_SOURCE_MIGU
        album.startsWith("$TENCENT_SOURCE_TAG|") -> CUSTOM_SOURCE_QQ
        else -> null
    }
}

/**
 * 判断这首歌曲是不是需要靠自定义源取直链的渠道歌曲（酷我 / 酷狗 / 咪咕 / QQ）。
 *
 * 判定优先级：`channelId`（明确字段）> `album` 前缀（历史数据兼容）。
 */
internal fun customSourceIdForSong(song: SongItem): String? {
    customSourceIdForChannelId(song.channelId)?.let { return it }
    return legacyCustomSourceIdFromAlbum(song.album)
}

/**
 * 组装交给音源脚本的 musicInfo。
 *
 * 各平台脚本取字段的习惯不一样（酷我用 songmid、酷狗用 hash、咪咕用 copyrightId），
 * 这里把所有常见字段都带上，脚本按需取用。
 */
internal fun buildCustomSourceMusicInfo(
    sourceId: String,
    song: SongItem,
    platformIdOverride: String? = null
): JSONObject {
    val platformId = platformIdOverride
        ?.takeIf { it.isNotBlank() }
        ?: song.audioId?.takeIf { it.isNotBlank() }
        ?: song.id.toString()
    val durationSeconds = (song.durationMs / 1000L).coerceAtLeast(0L)
    return JSONObject()
        .put("source", sourceId)
        .put("songmid", platformId)
        .put("songId", platformId)
        .put("id", song.id)
        .put("hash", platformId)
        .put("album_id", song.albumId)
        .put("albumId", song.albumId)
        .put("album_audio_id", platformId)
        .put("copyrightId", platformId)
        .put("name", song.name)
        .put("songname", song.name)
        .put("singer", song.artist)
        .put("singername", song.artist)
        .put("albumName", song.album)
        .put("duration", durationSeconds)
        .put("interval", durationSeconds)
        .put("durationMs", song.durationMs)
}
