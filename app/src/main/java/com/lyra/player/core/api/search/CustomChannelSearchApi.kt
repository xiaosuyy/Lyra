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
 * File: com.lyra.player.core.api.search/CustomChannelSearchApi
 * Created: 2026/9/24
 */

import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import com.lyra.player.core.logging.LyraLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "CustomChannelSearch"
private const val SEARCH_PAGE_SIZE = 20
private const val PLAYLIST_SONGS_LIMIT = 50
private const val MIGU_REFERER = "https://m.music.migu.cn/"
private const val KUGOU_COVER_SIZE = "240"
private const val DETAIL_PARALLELISM = 6
// UA 与第三方聚合 key 等易失效 / 敏感配置统一放在 CustomChannelSearchConfig 中
private val TENCENT_SONG_MID_PATTERN = Regex("/songDetail/([A-Za-z0-9]+)")
/** QQ 搜索结果少于这个数量时，用歌手歌曲/榜单匹配来补足 */
private const val TENCENT_SEARCH_MIN_RESULTS = 20
private const val TENCENT_SINGER_SONGS_LIMIT = 30
/** 补足搜索结果时参考的榜单 */
private val TENCENT_MATCH_TOP_IDS = listOf(26, 27, 4, 62)

/** 详情接口逐首补齐，用信号量限制并发，避免一次打太多请求 */
private val detailSemaphore = Semaphore(DETAIL_PARALLELISM)

/**
 * 其他渠道搜索（酷我 / 酷狗 / 咪咕）的搜索结果。
 */
internal data class CustomChannelSong(
    val platformId: String,
    val name: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String?,
    val albumId: Long = 0L,
    val albumAudioId: String? = null,
    val detailId: String? = null,
    /** 应用自己下载好的本地封面路径（远端地址仍保留在 coverUrl 里） */
    val localCoverUrl: String? = null
)

/** 渠道歌手搜索结果 */
data class CustomChannelArtist(
    val id: String,
    val name: String,
    val coverUrl: String? = null
)

/** 渠道歌单搜索结果 */
data class CustomChannelPlaylist(
    val id: String,
    val name: String,
    val coverUrl: String? = null,
    val songCount: Int = 0,
    val creator: String? = null
)

/**
 * 酷我 / 酷狗 / 咪咕 的搜索接口。
 *
 * 这三个平台的播放直链拿不到，需要配合自定义源（音源脚本）使用：
 * 搜索结果里的 platformId 会作为 musicInfo 交给音源脚本去取链。
 */
internal class CustomChannelSearchApi(private val okHttpClient: OkHttpClient) {
    suspend fun searchKuwo(keyword: String): List<CustomChannelSong> {
        val url = "http://search.kuwo.cn/r.s?all=" + encode(keyword) +
            "&ft=music&itemset=web_2013&client=kt&pn=0&rn=$SEARCH_PAGE_SIZE&rformat=json&encoding=utf8"
        val raw = get(url) ?: return emptyList()
        return runCatching {
            // 酷我返回的是单引号"JSON"，先规范化再解析
            val json = JSONObject(raw.replace('\'', '"'))
            val list = json.optJSONArray("abslist") ?: JSONArray()
            buildList(list.length()) {
                for (index in 0 until list.length()) {
                    val item = list.optJSONObject(index) ?: continue
                    val platformId = item.optString("MUSICRID").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("NAME").takeIf { it.isNotBlank() } ?: continue
                    add(
                        CustomChannelSong(
                            platformId = platformId,
                            name = name,
                            artist = item.optString("ARTIST"),
                            album = item.optString("ALBUM"),
                            durationMs = item.optLong("DURATION", 0L) * 1000L,
                            coverUrl = item.optString("web_albumpic_short")
                                .takeIf { it.isNotBlank() }
                                ?.let { "https://img1.kuwo.cn/star/albumcover/$it" },
                            albumId = item.optLong("ALBUMID", 0L)
                        )
                    )
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "酷我搜索解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    suspend fun searchKugou(keyword: String): List<CustomChannelSong> {
        val url = "http://mobilecdn.kugou.com/api/v3/search/song?format=json&keyword=" +
            encode(keyword) + "&page=1&pagesize=$SEARCH_PAGE_SIZE&showtype=1"
        val raw = get(url) ?: return emptyList()
        return runCatching {
            val info = JSONObject(raw).optJSONObject("data")?.optJSONArray("info") ?: JSONArray()
            val songs = buildList(info.length()) {
                for (index in 0 until info.length()) {
                    val item = info.optJSONObject(index) ?: continue
                    val hash = item.optString("hash").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("songname").takeIf { it.isNotBlank() } ?: continue
                    val albumId = item.optString("album_id").toLongOrNull() ?: 0L
                    add(
                        CustomChannelSong(
                            platformId = hash,
                            name = name,
                            artist = item.optString("singername"),
                            album = item.optString("album_name"),
                            durationMs = item.optLong("duration", 0L) * 1000L,
                            coverUrl = item.optJSONObject("trans_param")
                                ?.optString("union_cover")
                                ?.takeIf { it.isNotBlank() }
                                ?.replace("{size}", KUGOU_COVER_SIZE),
                            albumId = albumId,
                            albumAudioId = item.optLong("album_audio_id", 0L)
                                .takeIf { it > 0L }
                                ?.toString()
                        )
                    )
                }
            }
            val resolvedSongs = coroutineScope {
                songs.map { song ->
                    if (!song.coverUrl.isNullOrBlank() || song.albumId <= 0L) {
                        async { song }
                    } else {
                        async { song.copy(coverUrl = kugouAlbumCover(song.albumId)) }
                    }
                }.awaitAll()
            }
            LyraLogger.i(
                TAG,
                "酷狗搜索 $keyword 命中 ${songs.size} 首，示例=" +
                    songs.take(3).joinToString("; ") { "${it.name}/${it.artist}#${it.coverUrl}" }
            )
            resolvedSongs
        }.onFailure {
            LyraLogger.w(TAG, "酷狗搜索解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    /**
     * 老接口 stdmusic/{album_id}.jpg 只会返回默认黑胶占位图，
     * 缺少 union_cover 时用专辑接口补一次真实封面。
     */
    private suspend fun kugouAlbumCover(albumId: Long): String? {
        if (albumId <= 0L) return null
        val raw = get("http://mobilecdn.kugou.com/api/v3/album/info?albumid=$albumId") ?: return null
        return runCatching {
            JSONObject(raw)
                .optJSONObject("data")
                ?.optString("imgurl")
                ?.takeIf { it.isNotBlank() }
                ?.replace("{size}", KUGOU_COVER_SIZE)
        }.getOrNull()
    }

    /**
     * 咪咕搜索结果不带时长，封面也只有缩略图，这里补一次详情：
     * 取真实时长（歌词匹配要用）和更大尺寸封面。
     */
    private suspend fun miguSongDetail(contentId: String): Pair<Long, String?>? {
        if (contentId.isBlank()) return null
        val raw = get(
            "https://app.c.nf.migu.cn/MIGUM2.0/v1.0/content/resourceinfo.do" +
                "?resourceType=2&resourceId=$contentId",
            referer = "https://m.music.migu.cn/"
        ) ?: return null
        return runCatching {
            val resource = JSONObject(raw)
                .optJSONArray("resource")
                ?.optJSONObject(0)
                ?: return@runCatching null
            val durationMs = parseClockDurationMs(resource.optString("length"))
            val cover = resource.optJSONArray("albumImgs")
                ?.let { images ->
                    (0 until images.length()).mapNotNull { index ->
                        images.optJSONObject(index)?.optString("img")?.takeIf { it.isNotBlank() }
                    }
                }
                ?.lastOrNull()
            durationMs to cover
        }.getOrNull()
    }

    private fun parseClockDurationMs(value: String?): Long {
        val parts = value?.trim().orEmpty().split(":")
        if (parts.size !in 2..3) return 0L
        val numbers = parts.map { it.toLongOrNull() ?: return 0L }
        val seconds = numbers.fold(0L) { acc, part -> acc * 60L + part }
        return seconds * 1000L
    }

    suspend fun searchMigu(keyword: String): List<CustomChannelSong> {
        val url = "https://app.c.nf.migu.cn/MIGUM2.0/v1.0/content/search_all.do?text=" +
            encode(keyword) +
            "&pageNo=1&pageSize=$SEARCH_PAGE_SIZE&isCopyright=1&sort=1&searchSwitch=%7B%22song%22%3A1%7D"
        val raw = get(url, referer = "https://m.music.migu.cn/") ?: return emptyList()
        return runCatching {
            val list = JSONObject(raw)
                .optJSONObject("songResultData")
                ?.optJSONArray("result")
                ?: JSONArray()
            val songs = buildList(list.length()) {
                for (index in 0 until list.length()) {
                    val item = list.optJSONObject(index) ?: continue
                    val copyrightId = item.optString("copyrightId").takeIf { it.isNotBlank() }
                        ?: continue
                    val detailId = item.optString("contentId").takeIf { it.isNotBlank() }
                    val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val singer = item.optJSONArray("singers")
                        ?.let { singers ->
                            (0 until singers.length()).mapNotNull { singerIndex ->
                                singers.optJSONObject(singerIndex)?.optString("name")
                                    ?.takeIf { it.isNotBlank() }
                            }
                        }
                        .orEmpty()
                        .joinToString(" / ")
                    val album = item.optJSONArray("albums")?.optJSONObject(0)
                    val cover = item.optJSONArray("imgItems")
                        ?.let { images ->
                            (0 until images.length()).mapNotNull { imageIndex ->
                                images.optJSONObject(imageIndex)?.optString("img")
                                    ?.takeIf { it.isNotBlank() }
                            }
                        }
                        ?.lastOrNull()
                    add(
                        CustomChannelSong(
                            platformId = copyrightId,
                            name = name,
                            artist = singer,
                            album = album?.optString("name").orEmpty(),
                            durationMs = 0L,
                            coverUrl = cover,
                            albumId = album?.optString("id")?.toLongOrNull() ?: 0L,
                            detailId = detailId
                        )
                    )
                }
            }
            val resolvedSongs = coroutineScope {
                songs.map { song ->
                    val contentId = song.detailId
                    if (contentId.isNullOrBlank()) {
                        async { song }
                    } else {
                        async {
                            detailSemaphore.withPermit {
                                val detail = miguSongDetail(contentId) ?: return@withPermit song
                                song.copy(
                                    durationMs = detail.first.takeIf { it > 0L }
                                        ?: song.durationMs,
                                    coverUrl = detail.second ?: song.coverUrl
                                )
                            }
                        }
                    }
                }.awaitAll()
            }
            LyraLogger.i(
                TAG,
                "migu search $keyword -> ${resolvedSongs.size} songs, " +
                    "with cover=${resolvedSongs.count { !it.coverUrl.isNullOrBlank() }}, " +
                    "with duration=${resolvedSongs.count { it.durationMs > 0L }}"
            )
            resolvedSongs
        }.onFailure {
            LyraLogger.w(TAG, "咪咕搜索解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    /**
     * QQ 音乐搜索。
     *
     * 播放直链同样交给自定义源脚本（tx 平台），搜索结果里的 platformId 用 songmid，
     * 脚本侧 musicInfo.songmid 就能直接取链。
     */
    suspend fun searchTencent(keyword: String): List<CustomChannelSong> {
        val collected = LinkedHashMap<String, CustomChannelSong>()

        // 1) 联想接口直接命中的歌曲
        val suggestions = tencentSuggestSongs(keyword)
        if (suggestions.isNotEmpty()) {
            resolveTencentSuggestions(suggestions).forEach { song ->
                collected.putIfAbsent(song.platformId, song)
            }
        }

        // 2) 联想命中的歌手 → 拉取该歌手的热门歌曲（原来只有 4 首，这里补齐）
        if (collected.size < TENCENT_SEARCH_MIN_RESULTS) {
            tencentSuggestSingers(keyword).take(2).forEach { singer ->
                tencentSingerSongs(singer.singerMid).forEach { song ->
                    collected.putIfAbsent(song.platformId, song)
                }
            }
        }

        // 3) 仍不够时，用榜单里匹配关键词的歌曲补充
        if (collected.size < TENCENT_SEARCH_MIN_RESULTS) {
            tencentChartMatches(keyword).forEach { song ->
                collected.putIfAbsent(song.platformId, song)
            }
        }

        LyraLogger.i(
            TAG,
            "tencent search $keyword -> ${collected.size} songs (suggest=${suggestions.size}), " +
                "with cover=${collected.values.count { !it.coverUrl.isNullOrBlank() }}"
        )
        if (collected.isNotEmpty()) return collected.values.toList()
        // 联想接口没命中时，用音源脚本同款聚合接口按关键词兜底取一首
        tencentAggregateFallback(keyword)?.let { return listOf(it) }
        // 兜底：旧版搜索接口（部分网络/地区仍可用）
        return searchTencentLegacy(keyword)
    }

    /** 联想命中的歌手 */
    private suspend fun tencentSuggestSingers(keyword: String): List<TencentSingerSuggestion> {
        val url = "https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?key=" +
            encode(keyword) + "&format=json&inCharset=utf8&outCharset=utf-8"
        val raw = get(url, referer = "https://y.qq.com/", userAgent = CustomChannelSearchConfig.DESKTOP_USER_AGENT)
            ?: return emptyList()
        return runCatching {
            val list = JSONObject(raw)
                .optJSONObject("data")
                ?.optJSONObject("singer")
                ?.optJSONArray("itemlist")
                ?: JSONArray()
            buildList(list.length()) {
                for (index in 0 until list.length()) {
                    val item = list.optJSONObject(index) ?: continue
                    val mid = item.optString("mid").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                    add(
                        TencentSingerSuggestion(
                            singerMid = mid,
                            name = name,
                            picUrl = item.optString("pic").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "QQ 歌手联想解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    /** QQ 歌手热门歌曲 */
    private suspend fun tencentSingerSongs(
        singerMid: String,
        num: Int = TENCENT_SINGER_SONGS_LIMIT
    ): List<CustomChannelSong> {
        if (singerMid.isBlank()) return emptyList()
        val payload = JSONObject()
            .put("comm", JSONObject().put("ct", "24").put("cv", "0"))
            .put(
                "req",
                JSONObject()
                    .put("module", "music.web_singer_info_svr")
                    .put("method", "get_singer_detail_info")
                    .put(
                        "param",
                        JSONObject()
                            .put("sort", 5)
                            .put("singermid", singerMid)
                            .put("sin", 0)
                            .put("num", num)
                    )
            )
            .toString()
        val url = "https://u.y.qq.com/cgi-bin/musicu.fcg?format=json&data=" + encode(payload)
        val raw = get(url, referer = "https://y.qq.com/", userAgent = CustomChannelSearchConfig.DESKTOP_USER_AGENT)
            ?: return emptyList()
        return runCatching {
            val list = JSONObject(raw)
                .optJSONObject("req")
                ?.optJSONObject("data")
                ?.optJSONArray("songlist")
                ?: JSONArray()
            buildList(list.length()) {
                for (index in 0 until list.length()) {
                    val song = list.optJSONObject(index)?.let(::tencentSongFromJson) ?: continue
                    add(song)
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "QQ 歌手歌曲解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    /** 榜单里关键词匹配的歌曲（补足搜索结果） */
    private suspend fun tencentChartMatches(keyword: String): List<CustomChannelSong> {
        val needle = keyword.trim()
        if (needle.isBlank()) return emptyList()
        val result = ArrayList<CustomChannelSong>()
        TENCENT_MATCH_TOP_IDS.forEach { topId ->
            searchTencentTopList(topId).forEach { song ->
                if (song.name.contains(needle, ignoreCase = true) ||
                    song.artist.contains(needle, ignoreCase = true)
                ) {
                    result.add(song)
                }
            }
        }
        return result.distinctBy { it.platformId }
    }

    private fun tencentSongFromJson(obj: JSONObject): CustomChannelSong? {
        val songMid = obj.optString("mid").takeIf { it.isNotBlank() }
            ?: obj.optString("songmid").takeIf { it.isNotBlank() }
            ?: return null
        val name = obj.optString("name").takeIf { it.isNotBlank() }
            ?: obj.optString("songname").takeIf { it.isNotBlank() }
            ?: return null
        val album = obj.optJSONObject("album")
        val singers = obj.optJSONArray("singer")
            ?.let { array ->
                (0 until array.length()).mapNotNull { singerIndex ->
                    array.optJSONObject(singerIndex)
                        ?.optString("name")
                        ?.takeIf { it.isNotBlank() }
                }
            }
            .orEmpty()
            .joinToString(" / ")
        val albumMid = album?.optString("mid")?.takeIf { it.isNotBlank() }
            ?: obj.optString("albummid").takeIf { it.isNotBlank() }
        val albumIdValue = album?.optLong("id", 0L) ?: 0L
        return CustomChannelSong(
            platformId = songMid,
            name = name,
            artist = singers,
            album = album?.optString("name")?.takeIf { it.isNotBlank() }
                ?: obj.optString("albumname"),
            durationMs = (obj.optLong("interval", 0L) * 1000L).coerceAtLeast(0L),
            coverUrl = albumMid?.let { tencentAlbumCover(it) },
            albumId = albumIdValue.takeIf { it > 0L }
                ?: obj.optString("albumid").toLongOrNull()
                ?: 0L,
            albumAudioId = obj.optString("songid").takeIf { it.isNotBlank() }
                ?: obj.optString("id").takeIf { it.isNotBlank() }
        )
    }

    /**
     * 音源脚本（K×H 等）内置的 QQ 接口：按“歌名+歌手”关键词返回一首歌的
     * 直链、封面和详情页地址，这里只取歌曲信息，播放仍然交给自定义源脚本。
     */
    private suspend fun tencentAggregateFallback(keyword: String): CustomChannelSong? {
        val url = CustomChannelSearchConfig.TENCENT_AGGREGATE_BASE_URL +
            "?key=${CustomChannelSearchConfig.TENCENT_AGGREGATE_KEY}&msg=" +
            encode(keyword) + "&n=5"
        val raw = get(url) ?: return null
        return runCatching {
            val data = JSONObject(raw).optJSONObject("data") ?: return@runCatching null
            val songMid = TENCENT_SONG_MID_PATTERN.find(data.optString("html"))
                ?.groupValues
                ?.getOrNull(1)
                ?.takeIf { it.isNotBlank() }
                ?: return@runCatching null
            val name = data.optString("name").takeIf { it.isNotBlank() }
                ?: return@runCatching null
            LyraLogger.i(TAG, "tencent fallback hit: $keyword -> $name($songMid)")
            CustomChannelSong(
                platformId = songMid,
                name = name,
                artist = data.optString("songname"),
                album = "",
                durationMs = 0L,
                coverUrl = data.optString("picture").takeIf { it.isNotBlank() }
            )
        }.onFailure {
            LyraLogger.w(TAG, "QQ 兜底搜索解析失败: ${it.message}")
        }.getOrNull()
    }

    /**
     * QQ 音乐官方搜索接口现在匿名请求会被过滤成空结果，只有联想接口还返回歌曲，
     * 因此这里走 smartbox 联想词 + 单曲详情补全（封面、专辑、时长）。
     */
    private suspend fun tencentSuggestSongs(keyword: String): List<TencentSuggestion> {
        val url = "https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?key=" +
            encode(keyword) + "&format=json&inCharset=utf8&outCharset=utf-8"
        val raw = get(url, referer = "https://y.qq.com/", userAgent = CustomChannelSearchConfig.DESKTOP_USER_AGENT)
            ?: return emptyList()
        return runCatching {
            val list = JSONObject(raw)
                .optJSONObject("data")
                ?.optJSONObject("song")
                ?.optJSONArray("itemlist")
                ?: JSONArray()
            buildList(list.length()) {
                for (index in 0 until list.length()) {
                    val item = list.optJSONObject(index) ?: continue
                    val songMid = item.optString("mid").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                    add(
                        TencentSuggestion(
                            songMid = songMid,
                            songId = item.optString("id").takeIf { it.isNotBlank() },
                            name = name,
                            singer = item.optString("singer")
                        )
                    )
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "QQ 音乐联想解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    private suspend fun resolveTencentSuggestions(
        suggestions: List<TencentSuggestion>
    ): List<CustomChannelSong> = coroutineScope {
        suggestions.map { suggestion ->
            async {
                val detail = detailSemaphore.withPermit {
                    tencentSongDetail(suggestion.songMid)
                }
                CustomChannelSong(
                    platformId = suggestion.songMid,
                    name = detail?.name?.takeIf { it.isNotBlank() } ?: suggestion.name,
                    artist = detail?.singer?.takeIf { it.isNotBlank() } ?: suggestion.singer,
                    album = detail?.albumName.orEmpty(),
                    durationMs = detail?.durationMs ?: 0L,
                    coverUrl = detail?.albumMid?.let { tencentAlbumCover(it) },
                    albumId = detail?.albumId ?: 0L,
                    albumAudioId = suggestion.songId
                )
            }
        }.awaitAll()
    }

    private suspend fun tencentSongDetail(songMid: String): TencentDetail? {
        if (songMid.isBlank()) return null
        val payload = JSONObject()
            .put(
                "songinfo",
                JSONObject()
                    .put("method", "get_song_detail_yqq")
                    .put("module", "music.pf_song_detail_svr")
                    .put("param", JSONObject().put("song_mid", songMid))
            )
            .toString()
        val url = "https://u.y.qq.com/cgi-bin/musicu.fcg?format=json&data=" + encode(payload)
        val raw = get(url, referer = "https://y.qq.com/", userAgent = CustomChannelSearchConfig.DESKTOP_USER_AGENT)
            ?: return null
        return runCatching {
            val track = JSONObject(raw)
                .optJSONObject("songinfo")
                ?.optJSONObject("data")
                ?.optJSONObject("track_info")
                ?: return@runCatching null
            val album = track.optJSONObject("album")
            TencentDetail(
                name = track.optString("name"),
                singer = track.optJSONArray("singer")
                    ?.let { singers ->
                        (0 until singers.length()).mapNotNull { singerIndex ->
                            singers.optJSONObject(singerIndex)
                                ?.optString("name")
                                ?.takeIf { it.isNotBlank() }
                        }
                    }
                    .orEmpty()
                    .joinToString(" / "),
                albumName = album?.optString("name").orEmpty(),
                albumMid = album?.optString("mid")?.takeIf { it.isNotBlank() },
                albumId = album?.optString("id")?.toLongOrNull() ?: 0L,
                durationMs = (track.optLong("interval", 0L) * 1000L).coerceAtLeast(0L)
            )
        }.onFailure {
            LyraLogger.w(TAG, "QQ 音乐详情解析失败 mid=$songMid: ${it.message}")
        }.getOrNull()
    }

    private suspend fun searchTencentLegacy(keyword: String): List<CustomChannelSong> {
        val url = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?ct=24&qqmusic_ver=1298" +
            "&new_json=1&remoteplace=txt.yqq.song&searchid=0&t=0&aggr=1&cr=1&catZhida=1" +
            "&lossless=0&flag_qc=0&p=1&n=$SEARCH_PAGE_SIZE&w=" + encode(keyword) +
            "&g_tk=5381&loginUin=0&hostUin=0&format=json&inCharset=utf8" +
            "&outCharset=utf-8&notice=0&platform=yqq.json&needNewCode=0"
        val raw = get(url, referer = "https://y.qq.com/", userAgent = CustomChannelSearchConfig.DESKTOP_USER_AGENT)
            ?: return emptyList()
        return runCatching {
            val list = JSONObject(raw)
                .optJSONObject("data")
                ?.optJSONObject("song")
                ?.optJSONArray("list")
                ?: JSONArray()
            val songs = buildList(list.length()) {
                for (index in 0 until list.length()) {
                    val item = list.optJSONObject(index) ?: continue
                    val songMid = item.optString("mid")
                        .takeIf { it.isNotBlank() }
                        ?: item.optString("songmid").takeIf { it.isNotBlank() }
                        ?: continue
                    val name = item.optString("name")
                        .takeIf { it.isNotBlank() }
                        ?: item.optString("songname").takeIf { it.isNotBlank() }
                        ?: continue
                    val album = item.optJSONObject("album")
                    val albumMid = album?.optString("mid")
                        ?.takeIf { it.isNotBlank() }
                        ?: item.optString("albummid").takeIf { it.isNotBlank() }
                    val singers = item.optJSONArray("singer")
                        ?.let { array ->
                            (0 until array.length()).mapNotNull { singerIndex ->
                                array.optJSONObject(singerIndex)
                                    ?.optString("name")
                                    ?.takeIf { it.isNotBlank() }
                            }
                        }
                        .orEmpty()
                        .joinToString(" / ")
                    val intervalSeconds = item.optLong("interval", 0L)
                    add(
                        CustomChannelSong(
                            platformId = songMid,
                            name = name,
                            artist = singers,
                            album = album?.optString("name")
                                ?.takeIf { it.isNotBlank() }
                                ?: item.optString("albumname"),
                            durationMs = (intervalSeconds * 1000L).coerceAtLeast(0L),
                            coverUrl = albumMid?.let { tencentAlbumCover(it) },
                            albumId = album?.optLong("id", 0L)?.takeIf { it > 0L }
                                ?: item.optString("albumid").toLongOrNull()
                                ?: 0L,
                            albumAudioId = item.optString("id")
                                .takeIf { it.isNotBlank() }
                                ?: item.optString("songid").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
            LyraLogger.i(
                TAG,
                "tencent search $keyword -> ${songs.size} songs, " +
                    "with cover=${songs.count { !it.coverUrl.isNullOrBlank() }}"
            )
            songs
        }.onFailure {
            LyraLogger.w(TAG, "QQ 音乐搜索解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    /**
     * QQ 音乐榜单（热歌榜 / 新歌榜 / 巅峰榜）。
     *
     * 官方匿名搜索接口被过滤后，榜单接口仍稳定返回完整歌曲信息（封面、歌手、时长），
     * 用作首页“热门”等分区的 QQ 来源更合适。
     */
    suspend fun searchTencentTopList(topId: Int = 26): List<CustomChannelSong> {
        val url = "https://c.y.qq.com/v8/fcg-bin/fcg_v8_toplist_cp.fcg?g_tk=5381&uin=0" +
            "&format=json&inCharset=utf8&outCharset=utf-8&notice=0&platform=h5" +
            "&needNewCode=1&tpl=3&page=detail&type=top&topid=$topId"
        val raw = get(url, referer = "https://y.qq.com/", userAgent = CustomChannelSearchConfig.DESKTOP_USER_AGENT)
            ?: return emptyList()
        return runCatching {
            val list = JSONObject(raw).optJSONArray("songlist") ?: JSONArray()
            val songs = buildList(list.length()) {
                for (index in 0 until list.length()) {
                    val data = list.optJSONObject(index)?.optJSONObject("data") ?: continue
                    val song = tencentSongFromJson(data) ?: continue
                    add(song)
                }
            }
            LyraLogger.i(TAG, "tencent toplist $topId -> ${songs.size} songs, cover=${songs.count { !it.coverUrl.isNullOrBlank() }}")
            songs
        }.onFailure {
            LyraLogger.w(TAG, "QQ 榜单解析失败 topid=$topId: ${it.message}")
        }.getOrDefault(emptyList())
    }

    // ==================== 歌手 ====================

    suspend fun searchKugouArtists(keyword: String): List<CustomChannelArtist> {
        val url = "http://mobilecdn.kugou.com/api/v3/search/singer?format=json&keyword=" +
            encode(keyword) + "&page=1&pagesize=$SEARCH_PAGE_SIZE"
        val raw = get(url) ?: return emptyList()
        return runCatching {
            val array = JSONObject(raw).optJSONArray("data") ?: JSONArray()
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val name = item.optString("singername").takeIf { it.isNotBlank() } ?: continue
                    add(
                        CustomChannelArtist(
                            id = item.optString("singerid").takeIf { it.isNotBlank() } ?: name,
                            name = name,
                            coverUrl = item.optString("imgurl").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "酷狗歌手搜索解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    suspend fun searchMiguArtists(keyword: String): List<CustomChannelArtist> {
        val url = miguSearchUrl(keyword, song = false, artist = true, playlist = false)
        val raw = get(url, referer = MIGU_REFERER) ?: return emptyList()
        return runCatching {
            val array = JSONObject(raw).optJSONObject("singerResultData")?.optJSONArray("result")
                ?: JSONArray()
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                    add(
                        CustomChannelArtist(
                            id = item.optString("id").takeIf { it.isNotBlank() } ?: name,
                            name = name,
                            coverUrl = item.optJSONArray("imgItems")
                                ?.let { images ->
                                    (0 until images.length()).mapNotNull { imageIndex ->
                                        images.optJSONObject(imageIndex)
                                            ?.optString("img")
                                            ?.takeIf { it.isNotBlank() }
                                    }
                                }
                                ?.lastOrNull()
                        )
                    )
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "咪咕歌手搜索解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    suspend fun searchTencentArtists(keyword: String): List<CustomChannelArtist> {
        return tencentSuggestSingers(keyword).map { singer ->
            CustomChannelArtist(
                id = singer.singerMid,
                name = singer.name,
                coverUrl = singer.picUrl
            )
        }
    }

    // ==================== 歌单 ====================

    suspend fun searchKugouPlaylists(keyword: String): List<CustomChannelPlaylist> {
        val url = "http://mobilecdn.kugou.com/api/v3/search/special?format=json&keyword=" +
            encode(keyword) + "&page=1&pagesize=$SEARCH_PAGE_SIZE"
        val raw = get(url) ?: return emptyList()
        return runCatching {
            val info = JSONObject(raw).optJSONObject("data")?.optJSONArray("info") ?: JSONArray()
            buildList(info.length()) {
                for (index in 0 until info.length()) {
                    val item = info.optJSONObject(index) ?: continue
                    val id = item.optString("specialid").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("specialname").takeIf { it.isNotBlank() } ?: continue
                    val cover = item.optJSONObject("trans_param")
                        ?.optJSONObject("skin")
                        ?.optString("thumbnail")
                        ?.takeIf { it.isNotBlank() }
                        ?: item.optString("imgurl")
                            .takeIf { it.isNotBlank() }
                            ?.replace("{size}", KUGOU_COVER_SIZE)
                    add(
                        CustomChannelPlaylist(
                            id = id,
                            name = name,
                            coverUrl = cover,
                            songCount = item.optInt("songcount", 0),
                            creator = item.optString("nickname").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "酷狗歌单搜索解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    suspend fun searchMiguPlaylists(keyword: String): List<CustomChannelPlaylist> {
        val url = miguSearchUrl(keyword, song = false, artist = false, playlist = true)
        val raw = get(url, referer = MIGU_REFERER) ?: return emptyList()
        return runCatching {
            val array = JSONObject(raw).optJSONObject("songListResultData")?.optJSONArray("result")
                ?: JSONArray()
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                    add(
                        CustomChannelPlaylist(
                            id = id,
                            name = name,
                            coverUrl = item.optString("musicListPicUrl").takeIf { it.isNotBlank() },
                            songCount = item.optString("musicNum").toIntOrNull() ?: 0,
                            creator = item.optString("userId").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "咪咕歌单搜索解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    /**
     * QQ 匿名接口没有歌单关键词搜索，这里返回歌单广场的热门歌单。
     */
    suspend fun searchTencentPlaylists(): List<CustomChannelPlaylist> {
        val url = "https://c.y.qq.com/splcloud/fcgi-bin/fcg_get_diss_by_tag.fcg?picmid=1&rnd=0.1" +
            "&g_tk=5381&loginUin=0&hostUin=0&format=json&inCharset=utf8&outCharset=utf-8" +
            "&notice=0&platform=yqq.json&needNewCode=0&categoryId=10000000&sortId=5&sin=0&ein=29"
        val raw = get(url, referer = "https://y.qq.com/", userAgent = CustomChannelSearchConfig.DESKTOP_USER_AGENT)
            ?: return emptyList()
        return runCatching {
            val array = JSONObject(raw).optJSONObject("data")?.optJSONArray("list") ?: JSONArray()
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("dissid").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("dissname").takeIf { it.isNotBlank() } ?: continue
                    add(
                        CustomChannelPlaylist(
                            id = id,
                            name = name,
                            coverUrl = item.optString("imgurl").takeIf { it.isNotBlank() },
                            songCount = item.optInt("songnum", 0),
                            creator = item.optString("nickname").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "QQ 热门歌单解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    // ==================== 歌单内歌曲 ====================

    suspend fun fetchKugouPlaylistSongs(specialId: String): List<CustomChannelSong> {
        if (specialId.isBlank()) return emptyList()
        val url = "http://mobilecdn.kugou.com/api/v3/special/song?format=json&specialid=" +
            encode(specialId) + "&page=1&pagesize=$PLAYLIST_SONGS_LIMIT"
        val raw = get(url) ?: return emptyList()
        return runCatching {
            val info = JSONObject(raw).optJSONObject("data")?.optJSONArray("info") ?: JSONArray()
            buildList(info.length()) {
                for (index in 0 until info.length()) {
                    val item = info.optJSONObject(index) ?: continue
                    val hash = item.optString("hash").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("songname").takeIf { it.isNotBlank() } ?: continue
                    val albumId = item.optString("album_id").toLongOrNull() ?: 0L
                    add(
                        CustomChannelSong(
                            platformId = hash,
                            name = name,
                            artist = item.optString("singername"),
                            album = item.optString("album_name"),
                            durationMs = item.optLong("duration", 0L) * 1000L,
                            coverUrl = item.optJSONObject("trans_param")
                                ?.optString("union_cover")
                                ?.takeIf { it.isNotBlank() }
                                ?.replace("{size}", KUGOU_COVER_SIZE),
                            albumId = albumId,
                            albumAudioId = item.optLong("album_audio_id", 0L)
                                .takeIf { it > 0L }
                                ?.toString()
                        )
                    )
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "酷狗歌单歌曲解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    suspend fun fetchTencentPlaylistSongs(dissId: String): List<CustomChannelSong> {
        if (dissId.isBlank()) return emptyList()
        val payload = JSONObject()
            .put("comm", JSONObject().put("ct", "24").put("cv", "0"))
            .put(
                "req",
                JSONObject()
                    .put("module", "music.srfDissInfo.aiDissInfo")
                    .put("method", "uniform_get_Dissinfo")
                    .put(
                        "param",
                        JSONObject()
                            // QQ 这个接口要求 disstid 为数字，传字符串会返回 10006 错误
                            .put("disstid", dissId.toLongOrNull() ?: dissId)
                            .put("enc_host_uin", "")
                            .put("tag", 1)
                            .put("userinfo", 1)
                            .put("song_begin", 0)
                            .put("song_num", PLAYLIST_SONGS_LIMIT)
                    )
            )
            .toString()
        val url = "https://u.y.qq.com/cgi-bin/musicu.fcg?format=json&data=" + encode(payload)
        val raw = get(url, referer = "https://y.qq.com/", userAgent = CustomChannelSearchConfig.DESKTOP_USER_AGENT)
            ?: return emptyList()
        return runCatching {
            val array = JSONObject(raw)
                .optJSONObject("req")
                ?.optJSONObject("data")
                ?.optJSONArray("songlist")
                ?: JSONArray()
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val song = array.optJSONObject(index)?.let(::tencentSongFromJson) ?: continue
                    add(song)
                }
            }
        }.onFailure {
            LyraLogger.w(TAG, "QQ 歌单歌曲解析失败: ${it.message}")
        }.getOrDefault(emptyList())
    }

    private fun miguSearchUrl(
        keyword: String,
        song: Boolean,
        artist: Boolean,
        playlist: Boolean
    ): String {
        val switch = JSONObject()
            .put("song", if (song) 1 else 0)
            .put("album", 0)
            .put("singer", if (artist) 1 else 0)
            .put("songlist", if (playlist) 1 else 0)
            .toString()
        return "https://app.c.nf.migu.cn/MIGUM2.0/v1.0/content/search_all.do?text=" +
            encode(keyword) + "&pageNo=1&pageSize=$SEARCH_PAGE_SIZE&isCopyright=1&sort=1" +
            "&searchSwitch=" + encode(switch)
    }

    private fun tencentAlbumCover(albumMid: String): String =
        "https://y.gtimg.cn/music/photo_new/T002R300x300M000$albumMid.jpg"

    private data class TencentSuggestion(
        val songMid: String,
        val songId: String?,
        val name: String,
        val singer: String
    )

    private data class TencentSingerSuggestion(
        val singerMid: String,
        val name: String,
        val picUrl: String? = null
    )

    private data class TencentDetail(
        val name: String,
        val singer: String,
        val albumName: String,
        val albumMid: String?,
        val albumId: Long,
        val durationMs: Long
    )

    private suspend fun get(
        url: String,
        referer: String? = null,
        userAgent: String = CustomChannelSearchConfig.MOBILE_USER_AGENT
    ): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val builder = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                referer?.let { builder.header("Referer", it) }
                okHttpClient.newCall(builder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        LyraLogger.w(TAG, "搜索请求失败 ${response.code}: $url")
                        null
                    } else {
                        response.body?.string()
                    }
                }
            }.onFailure {
                LyraLogger.w(TAG, "搜索请求异常: ${it.message}")
            }.getOrNull()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        private val digests = MessageDigest.getInstance("SHA-256")

        /** 用平台 id 生成稳定的本地歌曲 id */
        internal fun stableChannelSongId(value: String): Long {
            val hash = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            var result = 0L
            hash.take(8).forEach { byte -> result = (result shl 8) or (byte.toLong() and 0xff) }
            return if (result == 0L) 1L else -kotlin.math.abs(result)
        }
    }
}
