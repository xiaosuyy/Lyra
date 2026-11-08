package com.lyra.player.core.api.gdstudio

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
 * File: com.lyra.player.core.api.gdstudio/GdStudioApiClient
 */

import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lyra.player.core.logging.LyraLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "GdStudioApi"
private const val BASE_URL = "https://music-api.gdstudio.xyz/api.php"

/** GD 音乐台搜索返回的单首歌曲。 */
internal data class GdStudioSong(
    /** 曲目 ID（track_id），播放时需要原样传回。 */
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val picId: String?,
    val lyricId: String?,
    /** GD 背后的音乐源，例如 netease / joox / bilibili。 */
    val gdSource: String
)

/**
 * GD 音乐台（music-api.gdstudio.xyz）API 客户端。
 *
 * 只负责 HTTP 请求和 JSON 解析，不做缓存、限流或 UI 映射。
 * 注意官方限制：5 分钟内不超过 50 次请求。
 */
internal class GdStudioApiClient(
    private val okHttpClient: OkHttpClient
) {

    suspend fun search(
        source: String,
        keyword: String,
        count: Int = 20,
        page: Int = 1
    ): List<GdStudioSong> {
        if (keyword.isBlank()) return emptyList()
        val url = buildString {
            append(BASE_URL)
            append("?types=search")
            append("&source=").append(encode(source))
            append("&name=").append(encode(keyword))
            append("&count=").append(count.coerceAtLeast(1))
            append("&pages=").append(page.coerceAtLeast(1))
        }
        val raw = get(url) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val artist = joinArtist(item.optJSONArray("artist"))
                    add(
                        GdStudioSong(
                            id = id,
                            name = name,
                            artist = artist,
                            album = item.optString("album"),
                            picId = item.optString("pic_id").takeIf { it.isNotBlank() },
                            lyricId = item.optString("lyric_id").takeIf { it.isNotBlank() },
                            gdSource = item.optString("source").ifBlank { source }
                        )
                    )
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "搜索解析失败 source=$source keyword=$keyword: ${it.message}")
        }.getOrDefault(emptyList())
    }

    /** 解析歌曲直链；失败或没有版权时返回 null。 */
    suspend fun resolveAudioUrl(
        source: String,
        trackId: String,
        br: Int = 320
    ): String? {
        if (trackId.isBlank()) return null
        val url = "$BASE_URL?types=url&source=${encode(source)}" +
            "&id=${encode(trackId)}&br=$br"
        val raw = get(url) ?: return null
        return runCatching {
            JSONObject(raw).optString("url").takeIf { it.startsWith("http") }
        }.onFailure {
            LyraLogger.w(TAG, "取直链解析失败 source=$source id=$trackId: ${it.message}")
        }.getOrNull()
    }

    /** 解析专辑图直链；失败返回 null。 */
    suspend fun resolvePicUrl(
        source: String,
        picId: String,
        size: Int = 300
    ): String? {
        if (picId.isBlank()) return null
        val url = "$BASE_URL?types=pic&source=${encode(source)}" +
            "&id=${encode(picId)}&size=$size"
        val raw = get(url) ?: return null
        return runCatching {
            JSONObject(raw).optString("url").takeIf { it.startsWith("http") }
        }.onFailure {
            LyraLogger.w(TAG, "取封面解析失败 source=$source id=$picId: ${it.message}")
        }.getOrNull()
    }

    /** 解析 LRC 歌词（原文 + 翻译）；失败返回 null。 */
    suspend fun resolveLyric(
        source: String,
        lyricId: String
    ): Pair<String?, String?>? {
        if (lyricId.isBlank()) return null
        val url = "$BASE_URL?types=lyric&source=${encode(source)}&id=${encode(lyricId)}"
        val raw = get(url) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val lyric = json.optString("lyric").takeIf { it.isNotBlank() }
            val tlyric = json.optString("tlyric").takeIf { it.isNotBlank() }
            if (lyric == null && tlyric == null) null else lyric to tlyric
        }.onFailure {
            LyraLogger.w(TAG, "取歌词解析失败 source=$source id=$lyricId: ${it.message}")
        }.getOrNull()
    }

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", GD_USER_AGENT)
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    LyraLogger.w(TAG, "请求失败 ${response.code}: $url")
                    return@use null
                }
                response.body?.string()
            }
        }.onFailure {
            LyraLogger.w(TAG, "请求异常: ${it.message}, url=$url")
        }.getOrNull()
    }

    private fun joinArtist(array: JSONArray?): String {
        if (array == null || array.length() == 0) return ""
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val value = array.optString(index).trim()
                if (value.isNotEmpty()) add(value)
            }
        }.joinToString("/")
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val GD_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
