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
 * File: com.lyra.player.core.api.qishui/QishuiParsing
 * Created: 2026/11/8
 */

import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.data.model.SongItem
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject

internal data class QishuiTrack(
    val id: String,
    val name: String,
    val artists: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long
) {
    fun toSongItem(): SongItem {
        val numericId = id.toLongOrNull()
            ?: ("qishui-$id").hashCode().toLong()
        return SongItem(
            id = numericId,
            name = name,
            artist = artists,
            album = album.ifBlank { QISHUI_SOURCE_TAG },
            albumId = 0L,
            durationMs = durationMs,
            coverUrl = coverUrl,
            channelId = MusicSourceIds.QISHUI,
            audioId = id
        )
    }

    companion object {
        const val QISHUI_SOURCE_TAG = "汽水音乐"
    }
}

internal data class QishuiAudio(
    val url: String,
    val bitrate: Int,
    val durationMs: Long?,
    val expiresAtMs: Long?,
    val isPreview: Boolean
)

internal object QishuiParsing {
    fun objects(array: JSONArray?): List<JSONObject> =
        (0 until (array?.length() ?: 0)).mapNotNull { array?.optJSONObject(it) }

    fun text(json: JSONObject?, key: String): String =
        json?.opt(key)?.takeUnless { it == JSONObject.NULL }?.toString().orEmpty()

    fun image(value: Any?): String? = when (value) {
        is String -> value.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        is JSONArray -> image(value.opt(0))
        is JSONObject -> image(value.opt("url"))
            ?: image(value.opt("urls"))?.let { base ->
                val uri = text(value, "uri")
                if (uri.isNotBlank() && !base.endsWith(uri)) base + uri else base
            }
            ?: image(value.opt("uri"))
        else -> null
    }

    fun track(resource: JSONObject): QishuiTrack? {
        val entity = resource.optJSONObject("entity") ?: resource
        val track = entity.optJSONObject("track_wrapper")?.optJSONObject("track")
            ?: entity.optJSONObject("track") ?: entity
        val trackId = text(track, "id").takeIf(String::isNotBlank) ?: return null
        val name = text(track, "name").ifBlank { text(track, "trackName") }
        if (name.isBlank()) return null
        val album = track.optJSONObject("album")
        val artwork = image(album?.opt("url_cover")) ?: image(album?.opt("cover_url"))
        return QishuiTrack(
            id = trackId,
            name = name,
            artists = objects(track.optJSONArray("artists"))
                .map { artist ->
                    text(artist, "name").ifBlank {
                        text(artist, "simple_display_name").ifBlank {
                            text(artist.optJSONObject("user_info"), "nickname")
                        }
                    }
                }
                .filter(String::isNotBlank)
                .joinToString("/"),
            album = text(album, "name"),
            coverUrl = artwork,
            durationMs = track.optLong("duration", track.optLong("duration_ms", 0L))
                .takeIf { it > 0L }
                ?: 0L
        )
    }

    fun tracksFromFeed(body: JSONObject): List<QishuiTrack> =
        objects(body.optJSONArray("items")).mapNotNull(::track)

    fun tracksFromResources(body: JSONObject, key: String): List<QishuiTrack> =
        objects(body.optJSONArray(key)).mapNotNull(::track)

    /** 从 `/luna/discover` 的 `discover_feed_radio` 块提取电台。 */
    fun radiosFromDiscover(body: JSONObject): List<QishuiRadio> {
        val blocks = objects(body.optJSONArray("blocks"))
        val radioBlock = blocks.firstOrNull { text(it, "type") == "discover_feed_radio" }
            ?: blocks.firstOrNull { block ->
                objects(block.optJSONArray("inner_block"))
                    .any { text(it, "type") == "discover_radio" }
            }
        val inner = objects(radioBlock?.optJSONArray("inner_block"))
            .ifEmpty { blocks.flatMap { objects(it.optJSONArray("inner_block")) } }
        return inner
            .filter { text(it, "type") == "discover_radio" }
            .flatMap { block ->
                val fallbackTitle = text(block, "title")
                val fallbackId = text(block, "inner_block_id")
                objects(block.optJSONArray("resources")).mapNotNull { resource ->
                    val radio = resource.optJSONObject("entity")?.optJSONObject("radio")
                    val id = radio?.let { text(it, "radio_id") }
                        ?.takeIf(String::isNotBlank)
                        ?: text(resource, "resource_id").takeIf(String::isNotBlank)
                        ?: fallbackId.takeIf(String::isNotBlank)
                        ?: return@mapNotNull null
                    val title = radio?.let { text(it, "radio_name") }
                        ?.takeIf(String::isNotBlank)
                        ?: text(resource.optJSONObject("style"), "title")
                            .takeIf(String::isNotBlank)
                        ?: fallbackTitle
                    QishuiRadio(id = id, name = title)
                }
            }
            .distinctBy { it.id }
    }

    fun tracksFromCollection(body: JSONObject): List<QishuiTrack> {
        val collections = objects(body.optJSONArray("mixed_collections"))
        return collections.flatMap { item ->
            listOfNotNull(
                item.optJSONObject("track"),
                item.optJSONObject("media")?.optJSONObject("track")
            ).mapNotNull { trackJson ->
                // 收藏项里的 track 是裸 track 对象，没有 track_wrapper 包装
                track(JSONObject().put("track", trackJson))
            }
        }.distinctBy { it.id }
    }

    fun audio(
        body: JSONObject,
        fullDurationMs: Long?,
        preferHighQuality: Boolean = true
    ): QishuiAudio? {
        val player = body.optJSONObject("track_player") ?: return null
        val model = player.optJSONObject("video_model")
            ?: runCatching { JSONObject(text(player, "video_model")) }.getOrNull()
            ?: return null
        val rawList = model.opt("video_list")
        val variants = when (rawList) {
            is JSONArray -> objects(rawList)
            is JSONObject -> rawList.keys().asSequence()
                .mapNotNull { rawList.optJSONObject(it) }
                .toList()
            else -> emptyList()
        }
        if (text(model, "spade_a").isNotBlank() || text(player, "spade_a").isNotBlank()) {
            return null
        }
        val candidates = variants
            .filter { text(it, "spade_a").isBlank() }
            .mapNotNull { variant ->
                val url = listOf(text(variant, "main_url"), text(variant, "backup_url"))
                    .firstOrNull(::isAudioUrl) ?: return@mapNotNull null
                val bitrate = variant.optJSONObject("video_meta")?.optInt("bitrate", 0) ?: 0
                Triple(url, bitrate, variant)
            }
            .sortedBy { it.second }
        val selected = (if (preferHighQuality) candidates.lastOrNull() else candidates.firstOrNull())
            ?: return null
        val duration = (model.optDouble("video_duration", 0.0) * 1_000.0)
            .toLong()
            .takeIf { it > 0L }
        val expiry = player.optLong("expire_at", model.optLong("url_expire", 0L))
            .takeIf { it > 0L }
            ?.let { if (it < 10_000_000_000L) it * 1_000L else it }
        val isPreview = player.optBoolean("is_preview") ||
            (duration != null && fullDurationMs != null && fullDurationMs > 0L &&
                duration + 2_000L < fullDurationMs)
        return QishuiAudio(
            url = selected.first,
            bitrate = selected.second,
            durationMs = duration,
            expiresAtMs = expiry,
            isPreview = isPreview
        )
    }

    fun isAudioUrl(value: String): Boolean {
        val url = value.toHttpUrlOrNull() ?: return false
        return url.isHttps && url.username.isEmpty() && url.password.isEmpty() &&
            AUDIO_HOSTS.any { url.host == it || url.host.endsWith(".$it") }
    }

    private val AUDIO_HOSTS = listOf(
        "douyin.com",
        "qishui.com",
        "douyinvod.com",
        "bytedance.com",
        "bytedance.net",
        "byted.org",
        "bytecdn.cn",
        "bytecdntp.com",
        "snssdk.com"
    )

    fun isQishuiAudioHost(host: String): Boolean {
        val normalized = host.lowercase()
        return AUDIO_HOSTS.any { normalized == it || normalized.endsWith(".$it") }
    }
}

internal data class QishuiRadio(val id: String, val name: String)
