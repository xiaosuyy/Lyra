package com.lyra.player.core.api.search

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
 * File: com.lyra.player.core.api.search/CustomChannelCoverCache
 * Created: 2026/9/24
 */

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import okhttp3.Request

private const val TAG = "CustomChannelCover"
private const val COVER_DIR_NAME = "custom_channel_covers"
private const val COVER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; LyraPlayer) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
private const val COVER_DOWNLOAD_PARALLELISM = 6
private val coverDownloadSemaphore = Semaphore(COVER_DOWNLOAD_PARALLELISM)

/**
 * 渠道歌曲（酷狗 / 咪咕）封面缓存。
 *
 * 这些平台的封面域名在部分设备/网络下用图片组件直接拉会失败，
 * 所以这里先用应用自己的网络栈把封面下载到本地缓存，再把本地文件交给图片组件显示。
 */
internal object CustomChannelCoverCache {
    private fun coverDir(context: Context): File =
        // 放在 filesDir 而不是 cacheDir：渠道歌曲存进歌单/队列后仍然引用这个路径，
        // 系统清理缓存目录时不该把已经落库的封面一起清掉。
        File(context.filesDir, COVER_DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun coverDigest(url: String): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(url.toByteArray(Charsets.UTF_8))
            .take(10)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun existingCoverFile(context: Context, url: String): File? {
        val digest = coverDigest(url)
        val formats = listOf("jpg", "jpeg", "png", "webp", "gif")
        return coverDir(context).listFiles { file -> file.isFile && file.name.startsWith("$digest.") }
            ?.filter { file ->
                file.length() > 0L && file.extension.lowercase() in formats
            }
            ?.maxByOrNull { it.length() }
    }

    /**
     * 按图片真实格式决定扩展名：图片组件要靠扩展名选解码器，扩展名不对就显示不出来。
     */
    private fun imageExtension(bytes: ByteArray): String {
        return when {
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpg"
            bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "png"
            bytes.size >= 12 && bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() -> "webp"
            bytes.size >= 3 && bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() -> "gif"
            else -> "jpg"
        }
    }

    /**
     * 返回可以交给图片组件显示的地址：下载成功给本地文件路径，失败返回 null。
     */
    suspend fun ensureLocalCover(context: Context, url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (!url.startsWith("http://") && !url.startsWith("https://")) return url
        existingCoverFile(context, url)?.let { return Uri.fromFile(it).toString() }
        return withContext(Dispatchers.IO) {
            runCatching {
                val builder = Request.Builder()
                    .url(url)
                    .header("User-Agent", COVER_USER_AGENT)
                if (url.contains("kugou", ignoreCase = true)) {
                    builder.header("Referer", "https://www.kugou.com/")
                }
                AppContainer.sharedOkHttpClient.newCall(builder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        LyraLogger.w(TAG, "封面下载失败 ${response.code}: $url")
                        return@use null
                    }
                    val bytes = response.body?.bytes() ?: return@use null
                    if (bytes.isEmpty()) return@use null
                    val file = File(coverDir(context), coverDigest(url) + "." + imageExtension(bytes))
                    file.writeBytes(bytes)
                    Uri.fromFile(file).toString()
                }
            }.onFailure {
                LyraLogger.w(TAG, "封面下载异常: ${it.message}")
            }.getOrNull()
        }
    }

    /**
     * 批量把远程封面换成本地缓存地址。
     */
    suspend fun ensureLocalCovers(
        context: Context,
        songs: List<CustomChannelSong>
    ): List<CustomChannelSong> {
        if (songs.isEmpty()) return songs
        val localized = coroutineScope {
            songs.map { song ->
                async {
                    // 并发下载封面会互相抢带宽导致超时，这里限制同时只下载几张
                    val local = coverDownloadSemaphore.withPermit {
                        ensureLocalCover(context, song.coverUrl)
                    }
                    if (local.isNullOrBlank()) song else song.copy(localCoverUrl = local)
                }
            }.awaitAll()
        }
        LyraLogger.i(
            TAG,
            "封面本地化: ${localized.count { !it.localCoverUrl.isNullOrBlank() }}/${songs.size}" +
                " 示例=${localized.firstOrNull()?.localCoverUrl?.take(90)}"
        )
        return localized
    }
}
