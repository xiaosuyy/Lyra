package com.lyra.player.core.api.qqmusic

import com.lyra.player.core.api.search.CustomChannelSearchApi
import com.lyra.player.core.api.search.CustomChannelSong
import com.lyra.player.core.api.search.tencentSongFromJson
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.data.auth.qqmusic.QQMusicSession
import com.lyra.player.util.network.awaitResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** QQ 音乐推荐歌单（首页卡片用）。 */
data class QQMusicHomePlaylist(
    val id: Long,
    val name: String,
    val coverUrl: String?,
    val trackCount: Int,
    val playCount: Long
)

/** QQ 音乐排行榜（首页排行榜卡片用）。 */
data class QQMusicToplist(
    val id: Long,
    val name: String,
    val coverUrl: String?,
    val trackCount: Int,
    val listenCount: Long
)

/**
 * QQ 音乐首页推荐数据：雷达、私人漫游、新歌首发、推荐歌单。
 *
 * 登录态（Cookie + qm_keyst）用来让推荐按账号口味生成；
 * 雷达与漫游匿名也可用，但只有登录后才会个性化。
 */
internal class QQMusicHomeClient(
    private val httpClient: OkHttpClient = AppContainer.sharedOkHttpClient
) {

    suspend fun getRadarSongs(
        session: QQMusicSession,
        page: Int = 1,
        mode: Int = RADAR_MODE_DEFAULT
    ): List<CustomChannelSong> {
        val raw = musicu(
            session = session,
            module = "music.recommend.TrackRelationServer",
            method = "GetRadarSong",
            param = JSONObject()
                .put("Page", page.coerceAtLeast(1))
                .put("Mode", mode.coerceIn(RADAR_MODE_FAMILIAR, RADAR_MODE_EXPLORE))
        )
        val data = raw.optJSONObject("req_0")?.optJSONObject("data")
        val array = data?.optJSONArray("VecSongs")
        return parseSongs(array) { item -> item.optJSONObject("Track") ?: item }
    }

    suspend fun getRadioTracks(
        session: QQMusicSession,
        pageSize: Int = 20
    ): List<CustomChannelSong> {
        val raw = musicu(
            session = session,
            module = "music.radioProxy.MbTrackRadioSvr",
            method = "get_radio_track",
            param = JSONObject().put("PageSize", pageSize.coerceAtLeast(1))
        )
        val array = raw.optJSONObject("req_0")?.optJSONObject("data")?.optJSONArray("tracks")
        return parseSongs(array)
    }

    suspend fun getNewSongs(limit: Int = 30): List<CustomChannelSong> {
        val raw = musicu(
            session = QQMusicSession(),
            module = "newsong.NewSongServer",
            method = "get_new_song_info",
            param = JSONObject().put("type", 1)
        )
        val array = raw.optJSONObject("req_0")?.optJSONObject("data")?.optJSONArray("songlist")
        return parseSongs(array).take(limit)
    }

    suspend fun getRecommendPlaylists(
        limit: Int = 12,
        from: Int = 0
    ): List<QQMusicHomePlaylist> {
        val raw = musicu(
            session = QQMusicSession(),
            module = "music.playlist.PlaylistSquare",
            method = "GetRecommendFeed",
            param = JSONObject()
                .put("From", from.coerceAtLeast(0))
                .put("Size", limit.coerceIn(1, 30))
        )
        val list = runCatching {
            raw.optJSONObject("req_0")
                ?.optJSONObject("data")
                ?.optJSONArray("List")
                ?: JSONArray()
        }.getOrDefault(JSONArray())
        return buildList(list.length()) {
            for (index in 0 until list.length()) {
                val basic = list.optJSONObject(index)
                    ?.optJSONObject("Playlist")
                    ?.optJSONObject("basic")
                    ?: continue
                val id = basic.optLong("tid", 0L)
                val title = basic.optString("title").trim()
                if (id <= 0L || title.isBlank()) continue
                add(
                    QQMusicHomePlaylist(
                        id = id,
                        name = title,
                        coverUrl = basic.optJSONObject("cover")
                            ?.optString("medium_url")
                            ?.takeIf { it.isNotBlank() },
                        trackCount = basic.optInt("song_cnt", 0),
                        playCount = basic.optLong("play_cnt", 0L)
                    )
                )
            }
        }
    }

    /** 排行榜列表（匿名可用，用于首页排行榜卡片）。 */
    suspend fun getToplists(limit: Int = 8): List<QQMusicToplist> {
        val raw = musicu(
            session = QQMusicSession(),
            module = "music.musicToplist.Toplist",
            method = "GetAll",
            param = JSONObject()
                .put("offset", 0)
                .put("num", 40)
                .put("tag", 0)
        )
        val groups = raw.optJSONObject("req_0")
            ?.optJSONObject("data")
            ?.optJSONArray("group")
            ?: return emptyList()
        val seen = LinkedHashMap<Long, QQMusicToplist>()
        for (groupIndex in 0 until groups.length()) {
            val list = groups.optJSONObject(groupIndex)?.optJSONArray("toplist") ?: continue
            for (index in 0 until list.length()) {
                val item = list.optJSONObject(index) ?: continue
                val id = item.optLong("topId", 0L)
                val title = item.optString("title").trim()
                if (id <= 0L || title.isBlank()) continue
                if (title.contains("MV") || title.contains("视频")) continue
                seen.putIfAbsent(
                    id,
                    QQMusicToplist(
                        id = id,
                        name = title,
                        coverUrl = item.optString("frontPicUrl")
                            .ifBlank { item.optString("headPicUrl") }
                            .takeIf { it.isNotBlank() },
                        trackCount = item.optInt("totalNum", 0),
                        listenCount = item.optLong("listenNum", 0L)
                    )
                )
                if (seen.size >= limit) return seen.values.toList()
            }
        }
        return seen.values.toList()
    }

    /** 单个排行榜歌曲（匿名可用）。 */
    suspend fun getToplistSongs(topId: Long, limit: Int = 60): List<CustomChannelSong> {
        if (topId <= 0L) return emptyList()
        val raw = musicu(
            session = QQMusicSession(),
            module = "music.musicToplist.Toplist",
            method = "GetDetail",
            param = JSONObject()
                .put("topId", topId)
                .put("offset", 0)
                .put("num", limit.coerceIn(1, 100))
        )
        val array = raw.optJSONObject("req_0")
            ?.optJSONObject("data")
            ?.optJSONArray("songInfoList")
        return parseSongs(array).take(limit)
    }

    /** 每日 30 首：从账号推荐流里取「每日」卡片对应的歌单并拉歌（需登录）。 */
    suspend fun getDailySongs(session: QQMusicSession, limit: Int = 30): List<CustomChannelSong> {
        if (!session.isLoggedIn) return emptyList()
        val card = recommendFeedCards(session).firstOrNull {
            it.optInt("subtype") == DAILY_SUBTYPE ||
                it.optString("title").contains("每日")
        } ?: return emptyList()
        val playlistId = numericId(card)
        if (playlistId <= 0L) return emptyList()
        return getPlaylistSongs(playlistId.toString(), limit)
    }

    /** 为你精选（AI 歌单）：账号推荐流里的歌单卡片（需登录）。 */
    suspend fun getForYouPlaylists(
        session: QQMusicSession,
        limit: Int = 10
    ): List<QQMusicHomePlaylist> {
        if (!session.isLoggedIn) return emptyList()
        val cards = recommendFeedCards(session)
        val candidates = cards.filter { card ->
            val id = numericId(card)
            card.optString("title").isNotBlank() &&
                id > 0L &&
                card.optInt("subtype") != DAILY_SUBTYPE
        }
        val playlists = candidates.filter { card ->
            card.optInt("type") == PLAYLIST_CARD_TYPE ||
                card.optInt("style") in FOR_YOU_STYLES
        }
        val picked = playlists.ifEmpty { candidates }
        return picked.mapNotNull { card ->
            val id = numericId(card)
            val title = card.optString("title").trim()
            if (id <= 0L || title.isBlank()) return@mapNotNull null
            val miscellany = card.optJSONObject("miscellany")
            QQMusicHomePlaylist(
                id = id,
                name = title,
                coverUrl = card.optString("cover")
                    .ifBlank { miscellany?.optString("foryou_headurl").orEmpty() }
                    .takeIf { it.isNotBlank() },
                trackCount = miscellany?.optInt("song_cnt", 0) ?: 0,
                playCount = card.optLong("play_cnt", 0L)
            )
        }.take(limit)
    }

    /** 热歌榜（榜单接口匿名可用）。 */
    suspend fun getHotRank(limit: Int = 30): List<CustomChannelSong> =
        CustomChannelSearchApi(httpClient).searchTencentTopList(TENCENT_HOT_TOPLIST_ID).take(limit)

    /** 歌单详情歌曲（QQ 歌单 tid）。 */
    suspend fun getPlaylistSongs(playlistId: String, limit: Int = 50): List<CustomChannelSong> =
        CustomChannelSearchApi(httpClient).fetchTencentPlaylistSongs(playlistId).take(limit)

    /** 账号推荐信息流：每日 30 首与为你精选都从这里取卡片。 */
    private suspend fun recommendFeedCards(session: QQMusicSession): List<JSONObject> {
        val param = JSONObject()
            .put("direction", 0)
            .put("page", 1)
            .put("v_cache", JSONArray())
            .put("v_uniq", JSONArray())
            .put("s_num", 0)
            .put("client_time", System.currentTimeMillis() / 1000L)
            .put(
                "ext",
                JSONObject()
                    .put("has_login", "1")
                    .put("is_20_style", "1")
                    .put("first_req_time", "1")
                    .put("client_time_zone", "Asia/Shanghai")
            )
        val raw = musicu(
            session = session,
            module = "music.recommend.RecommendFeed",
            method = "get_recommend_feed",
            param = param
        )
        val shelves = raw.optJSONObject("req_0")
            ?.optJSONObject("data")
            ?.optJSONArray("v_shelf")
            ?: return emptyList()
        val cards = ArrayList<JSONObject>()
        for (shelfIndex in 0 until shelves.length()) {
            val niches = shelves.optJSONObject(shelfIndex)?.optJSONArray("v_niche") ?: continue
            for (nicheIndex in 0 until niches.length()) {
                val nicheCards = niches.optJSONObject(nicheIndex)?.optJSONArray("v_card") ?: continue
                for (cardIndex in 0 until nicheCards.length()) {
                    nicheCards.optJSONObject(cardIndex)?.let(cards::add)
                }
            }
        }
        return cards
    }

    private fun numericId(card: JSONObject): Long = when (val raw = card.opt("id")) {
        is Number -> raw.toLong()
        is String -> raw.trim().toLongOrNull() ?: 0L
        else -> 0L
    }

    private suspend fun musicu(
        session: QQMusicSession,
        module: String,
        method: String,
        param: JSONObject
    ): JSONObject = withContext(Dispatchers.IO) {
        val loggedIn = session.cookie.isNotBlank()
        val comm = JSONObject()
            .put("ct", 24)
            .put("cv", 4747474)
            .put("format", "json")
            .put("inCharset", "utf-8")
            .put("outCharset", "utf-8")
            .put("notice", 0)
            .put("platform", "yqq.json")
            .put("need_new_code", 1)
            .put("uin", session.uin.ifBlank { "0" })
            .put("g_tk", hash33(session.musicKey))
            .put("g_tk_new_20200303", hash33(session.musicKey))
        val payload = JSONObject()
            .put("comm", comm)
            .put(
                "req_0",
                JSONObject()
                    .put("module", module)
                    .put("method", method)
                    .put("param", param)
            )
        val url = "https://u.y.qq.com/cgi-bin/musicu.fcg".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("data", payload.toString())
            ?.build()
            ?: throw IOException("QQ音乐推荐请求地址无效")
        val request = Request.Builder()
            .url(url)
            .header("Referer", "https://y.qq.com/")
            .apply { if (loggedIn) header("Cookie", session.cookie) }
            .build()
        val raw = httpClient.newCall(request).awaitResponse { response ->
            if (!response.isSuccessful) {
                throw IOException("QQ音乐推荐请求失败: ${response.code}")
            }
            response.body.string()
        }
        val json = JSONObject(raw)
        LyraLogger.d(
            "LYRA-QQHome",
            "musicu $module/$method reqCode=${json.optJSONObject("req_0")?.optInt("code")} len=${raw.length}"
        )
        json
    }

    private fun parseSongs(
        array: JSONArray?,
        unwrap: (JSONObject) -> JSONObject? = { it }
    ): List<CustomChannelSong> {
        if (array == null) return emptyList()
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val song = unwrap(item)?.let(::tencentSongFromJson) ?: continue
                add(song)
            }
        }
    }

    private fun hash33(value: String, seed: Long = 5381L): Long {
        var hash = seed
        value.forEach { char ->
            hash += (hash shl 5) + char.code
            hash = hash and 0xFFFF_FFFFL
        }
        return hash and 0x7FFF_FFFFL
    }

    private companion object {
        const val TENCENT_HOT_TOPLIST_ID = 26
        const val DAILY_SUBTYPE = 510
        const val PLAYLIST_CARD_TYPE = 200
        val FOR_YOU_STYLES = setOf(202, 302)

        const val RADAR_MODE_FAMILIAR = 1
        const val RADAR_MODE_DEFAULT = 2
        const val RADAR_MODE_EXPLORE = 3
    }
}
