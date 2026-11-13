package com.lyra.player.core.api.qqmusic

import com.lyra.player.core.api.search.CustomChannelSong
import com.lyra.player.core.api.search.tencentSongFromJson
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.data.auth.qqmusic.QQMusicSession
import com.lyra.player.util.network.awaitResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** 「我喜欢」在 QQ 音乐里的固定目录 id。 */
internal const val QQ_LIKED_DIRECTORY_ID = 201

internal data class QQMusicLikedPage(
    val songs: List<CustomChannelSong>,
    val total: Int
)

/** QQ 音乐账号下的歌单（导入到 Lyra 用）。 */
internal data class QQMusicRemotePlaylist(
    val dissId: Long,
    val name: String,
    val coverUrl: String?,
    val songCount: Int
)

internal data class QQMusicFavoriteWriteResult(
    val added: Int,
    val failed: Int
)

/**
 * QQ 音乐「我喜欢」读写。
 *
 * 读：`music.srfDissInfo.DissInfo/CgiGetDiss` + dirid=201。
 * 写：`music.musicasset.PlaylistDetailWrite`（AddSonglist / DelSonglist），
 * 失败时退回 Web 版收藏接口，和 MeloX 的处理保持一致。
 */
internal class QQMusicFavoriteClient(
    private val httpClient: OkHttpClient = AppContainer.sharedOkHttpClient
) {

    suspend fun fetchLikedSongs(
        session: QQMusicSession,
        songBegin: Int = 0,
        songNum: Int = LIKED_PAGE_SIZE
    ): QQMusicLikedPage = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) throw IOException("请先登录 QQ音乐账号")
        val param = JSONObject()
            .put("dirid", QQ_LIKED_DIRECTORY_ID)
            .put("disstid", 0)
            .put("tag", true)
            .put("song_begin", songBegin.coerceAtLeast(0))
            .put("song_num", songNum.coerceIn(1, 300))
            .put("userinfo", false)
            .put("orderlist", false)
            .put("onlysonglist", true)
        val data = runCatching {
            musicu(
                session = session,
                module = "music.srfDissInfo.DissInfo",
                method = "CgiGetDiss",
                param = param,
                commStyle = MusicuCommStyle.WEB
            )
        }.getOrElse { webError ->
            runCatching {
                musicu(
                    session = session,
                    module = "music.srfDissInfo.DissInfo",
                    method = "CgiGetDiss",
                    param = param,
                    commStyle = MusicuCommStyle.ANDROID
                )
            }.getOrElse { throw webError }
        }
        val songs = data.optJSONArray("songlist")?.let(::parseSongs).orEmpty()
        val total = data.optInt("total_song_num", songs.size)
        LyraLogger.d(
            "LYRA-QQFav",
            "liked page begin=$songBegin num=$songNum got=${songs.size} total=$total"
        )
        QQMusicLikedPage(songs = songs, total = total)
    }

    /** 当前账号创建/收藏的歌单列表。 */
    suspend fun fetchUserPlaylists(session: QQMusicSession): List<QQMusicRemotePlaylist> =
        withContext(Dispatchers.IO) {
            if (!session.isLoggedIn) throw IOException("请先登录 QQ音乐账号")
            val param = JSONObject().put("uin", session.uin)
            val data = runCatching {
                musicu(
                    session = session,
                    module = "music.musicasset.PlaylistBaseRead",
                    method = "GetPlaylistByUin",
                    param = param,
                    commStyle = MusicuCommStyle.ANDROID
                )
            }.getOrElse { androidError ->
                runCatching {
                    musicu(
                        session = session,
                        module = "music.musicasset.PlaylistBaseRead",
                        method = "GetPlaylistByUin",
                        param = param,
                        commStyle = MusicuCommStyle.WEB
                    )
                }.getOrElse { throw androidError }
            }
            val array = findArray(data, "v_playlist", "playlist", "list")
                ?: return@withContext emptyList()
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val basic = item.optJSONObject("basic") ?: item
                    val dissId = firstLong(basic, "tid", "dissid", "disstid", "id")
                        .takeIf { it > 0L } ?: continue
                    val name = firstString(basic, "dissname", "title", "name", "dirName")
                        .takeIf { it.isNotBlank() } ?: continue
                    val cover = firstString(basic, "logo", "picurl", "picUrl", "coverUrl")
                        .ifBlank {
                            basic.optJSONObject("cover")?.optString("default_url").orEmpty()
                        }
                        .takeIf { it.isNotBlank() }
                        ?.replaceFirst("http://", "https://")
                    val songCount = firstInt(basic, "songnum", "songNum", "song_cnt")
                        ?.coerceAtLeast(0) ?: 0
                    add(QQMusicRemotePlaylist(dissId, name, cover, songCount))
                }
            }
        }

    /** 批量收藏；只处理能解析出数字 songId 的 mid。 */
    suspend fun addFavorites(
        session: QQMusicSession,
        songMids: List<String>
    ): QQMusicFavoriteWriteResult = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) throw IOException("请先登录 QQ音乐账号")
        val mids = songMids.map(String::trim).filter(String::isNotBlank).distinct()
        if (mids.isEmpty()) return@withContext QQMusicFavoriteWriteResult(added = 0, failed = 0)

        var added = 0
        var failed = 0
        mids.chunked(WRITE_BATCH_SIZE).forEach { chunk ->
            val refs = runCatching { resolveWriteRefs(chunk, session) }
                .onFailure { LyraLogger.w("LYRA-QQFav", "resolve write refs failed: ${it.message}") }
                .getOrDefault(emptyMap())
            val missing = chunk.filter { refs[it] == null }
            failed += missing.size
            if (refs.isEmpty()) return@forEach

            val primary = runCatching {
                musicu(
                    session = session,
                    module = "music.musicasset.PlaylistDetailWrite",
                    method = "AddSonglist",
                    param = buildWriteParam(refs.values.toList()),
                    commStyle = MusicuCommStyle.ANDROID,
                    checkRetCode = true
                )
            }
            if (primary.isSuccess) {
                added += refs.size
                return@forEach
            }

            // 部分账号不接受 Android 写接口，退回 Web 收藏（逐首）。
            var chunkAdded = 0
            refs.forEach { (mid, ref) ->
                runCatching { legacyAddFavorite(session, mid, ref) }
                    .onSuccess { chunkAdded++ }
                    .onFailure { LyraLogger.w("LYRA-QQFav", "legacy favorite $mid failed: ${it.message}") }
            }
            added += chunkAdded
            failed += refs.size - chunkAdded
        }
        QQMusicFavoriteWriteResult(added = added, failed = failed)
    }

    private fun parseSongs(array: JSONArray): List<CustomChannelSong> = buildList(array.length()) {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            // 「我喜欢」列表里可能是 { track: {...} } 包装。
            val song = tencentSongFromJson(item.optJSONObject("track") ?: item) ?: continue
            add(song)
        }
    }

    private suspend fun resolveWriteRefs(
        mids: List<String>,
        session: QQMusicSession
    ): Map<String, SongWriteRef> {
        val result = LinkedHashMap<String, SongWriteRef>()
        runCatching {
            val data = musicu(
                session = session,
                module = "music.trackInfo.UniformRuleCtrl",
                method = "CgiGetTrackInfo",
                param = JSONObject()
                    .put("ctx", 0)
                    .put("client", 1)
                    .put("types", JSONArray().apply { mids.forEach { put(0) } })
                    .put("modify_stamp", JSONArray().apply { mids.forEach { put(0) } })
                    .put("mids", JSONArray(mids)),
                commStyle = MusicuCommStyle.ANDROID
            )
            findArray(data, "tracks", "track_list", "songlist")?.let { tracks ->
                for (index in 0 until tracks.length()) {
                    val item = tracks.optJSONObject(index) ?: continue
                    val mid = firstString(item, "mid", "songmid", "songMid")
                    if (mid.isBlank()) continue
                    val id = firstLong(item, "id", "songid", "songId")
                    if (id <= 0L) continue
                    result[mid] = SongWriteRef(id, firstInt(item, "type", "songtype", "songType") ?: 0)
                }
            }
        }.onFailure {
            LyraLogger.w("LYRA-QQFav", "CgiGetTrackInfo failed: ${it.message}")
        }
        mids.filterNot(result::containsKey).forEach { mid ->
            runCatching { resolveWebSongRef(session, mid) }
                .getOrNull()
                ?.let { result[mid] = it }
        }
        return result
    }

    private suspend fun resolveWebSongRef(session: QQMusicSession, songMid: String): SongWriteRef? {
        val url = "https://c.y.qq.com/v8/fcg-bin/fcg_play_single_song.fcg".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("songmid", songMid)
            ?.addQueryParameter("tpl", "yqq_song_detail")
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("g_tk", hash33(session.musicKey).toString())
            ?.addQueryParameter("loginUin", session.uin)
            ?.addQueryParameter("hostUin", "0")
            ?.addQueryParameter("outCharset", "utf8")
            ?.addQueryParameter("notice", "0")
            ?.addQueryParameter("platform", "yqq")
            ?.addQueryParameter("needNewCode", "0")
            ?.build()
            ?: return null
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DESKTOP_USER_AGENT)
            .header("Referer", "https://y.qq.com/")
            .apply { if (session.cookie.isNotBlank()) header("Cookie", session.cookie) }
            .build()
        val body = runCatching {
            httpClient.newCall(request).awaitResponse { if (it.isSuccessful) it.body.string() else null }
        }.getOrNull() ?: return null
        val item = runCatching {
            JSONObject(body).optJSONArray("data")?.optJSONObject(0)
        }.getOrNull() ?: return null
        val id = firstLong(item, "id", "songid", "songId")
        if (id <= 0L) return null
        return SongWriteRef(id, firstInt(item, "type", "songtype", "songType") ?: 0)
    }

    private fun buildWriteParam(songs: List<SongWriteRef>): JSONObject = JSONObject()
        .put("dirId", QQ_LIKED_DIRECTORY_ID)
        .put("tid", 0)
        .put("bFmtUtf8", true)
        .put(
            "v_songInfo",
            JSONArray().apply {
                songs.forEach { song ->
                    put(
                        JSONObject()
                            .put("songId", song.songId)
                            .put("songType", song.songType)
                    )
                }
            }
        )

    private suspend fun legacyAddFavorite(session: QQMusicSession, songMid: String, song: SongWriteRef) {
        val url = "https://c.y.qq.com/splcloud/fcgi-bin/fcg_music_add2songdir.fcg".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("g_tk", hash33(session.musicKey).toString())
            ?.addQueryParameter("midlist", songMid)
            ?.addQueryParameter("typelist", "13")
            ?.addQueryParameter("dirid", QQ_LIKED_DIRECTORY_ID.toString())
            ?.addQueryParameter("addtype", "")
            ?.addQueryParameter("sender", "4")
            ?.addQueryParameter("formsender", "4")
            ?.addQueryParameter("r2", "0")
            ?.addQueryParameter("r3", "1")
            ?.addQueryParameter("utf8", "1")
            ?.build()
            ?: throw IOException("QQ音乐收藏地址无效")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DESKTOP_USER_AGENT)
            .header("Referer", "https://y.qq.com/n/yqq/playlist")
            .apply { if (session.cookie.isNotBlank()) header("Cookie", session.cookie) }
            .build()
        val body = httpClient.newCall(request).awaitResponse { response ->
            if (!response.isSuccessful) throw IOException("QQ音乐 Web 收藏请求失败：HTTP ${response.code}")
            response.body.string()
        }
        val json = runCatching { JSONObject(stripJsonp(body)) }
            .getOrElse { throw IOException("QQ音乐 Web 收藏响应无法解析", it) }
        val code = firstInt(json, "code") ?: -1
        if (code != 0) throw IOException(json.optString("msg").ifBlank { "QQ音乐 Web 收藏错误码 $code" })
    }

    private suspend fun musicu(
        session: QQMusicSession,
        module: String,
        method: String,
        param: JSONObject,
        commStyle: MusicuCommStyle,
        checkRetCode: Boolean = false
    ): JSONObject = withContext(Dispatchers.IO) {
        val comm = JSONObject()
        when (commStyle) {
            MusicuCommStyle.WEB -> {
                comm.put("ct", 24)
                    .put("cv", 4_747_474)
                    .put("format", "json")
                    .put("inCharset", "utf-8")
                    .put("outCharset", "utf-8")
                    .put("notice", 0)
                    .put("platform", "yqq.json")
                    .put("need_new_code", 1)
                    .put("uin", session.uin.ifBlank { "0" })
                    .put("g_tk", hash33(session.musicKey))
                    .put("g_tk_new_20200303", hash33(session.musicKey))
            }
            MusicuCommStyle.ANDROID -> {
                comm.put("ct", ANDROID_CLIENT_TYPE)
                    .put("cv", ANDROID_CLIENT_VERSION)
                    .put("v", ANDROID_CLIENT_VERSION)
                    .put("chid", "10003505")
                    .put("qq", session.uin)
                    .put("authst", session.musicKey)
                    .put("tmeAppID", "qqmusic")
                    .put("tmeLoginType", if (session.musicKey.startsWith("W_X")) 1 else 2)
            }
        }
        val payload = JSONObject()
            .put("comm", comm)
            .put(
                "req_0",
                JSONObject()
                    .put("module", module)
                    .put("method", method)
                    .put("param", param)
            )
        val usePost = commStyle == MusicuCommStyle.ANDROID
        val request = if (usePost) {
            Request.Builder()
                .url("https://u.y.qq.com/cgi-bin/musicu.fcg")
                .header("User-Agent", "QQMusic $ANDROID_CLIENT_VERSION(android 15)")
                .apply { if (session.cookie.isNotBlank()) header("Cookie", session.cookie) }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        } else {
            val url = "https://u.y.qq.com/cgi-bin/musicu.fcg".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("format", "json")
                ?.addQueryParameter("data", payload.toString())
                ?.build()
                ?: throw IOException("QQ音乐请求地址无效")
            Request.Builder()
                .url(url)
                .header("Referer", "https://y.qq.com/")
                .apply { if (session.cookie.isNotBlank()) header("Cookie", session.cookie) }
                .build()
        }
        val raw = httpClient.newCall(request).awaitResponse { response ->
            if (!response.isSuccessful) throw IOException("QQ音乐请求失败：HTTP ${response.code}")
            response.body.string()
        }
        val root = runCatching { JSONObject(raw) }
            .getOrElse { throw IOException("QQ音乐返回无法解析的数据", it) }
        val req = root.optJSONObject("req_0")
            ?: throw IOException("QQ音乐响应缺少 req_0")
        val code = req.optInt("code", 0)
        if (code != 0) {
            throw IOException(req.optString("message").ifBlank { req.optString("msg") }.ifBlank { "QQ音乐错误码 $code" })
        }
        val data = req.optJSONObject("data") ?: JSONObject()
        if (checkRetCode) {
            val retCode = findInt(data, "retCode", "retcode") ?: 0
            if (retCode != 0) throw IOException("QQ音乐写入返回 $retCode")
        }
        data
    }

    private fun stripJsonp(value: String): String {
        val trimmed = value.trim()
        if (trimmed.startsWith('{')) return trimmed
        val first = trimmed.indexOf('{')
        val last = trimmed.lastIndexOf('}')
        if (first >= 0 && last > first) return trimmed.substring(first, last + 1)
        throw IOException("QQ音乐返回无法解析的数据")
    }

    private fun findArray(value: Any?, vararg keys: String): JSONArray? = when (value) {
        is JSONObject -> {
            keys.asSequence().mapNotNull(value::optJSONArray).firstOrNull()
                ?: value.keys().asSequence()
                    .mapNotNull { key -> findArray(value.opt(key), *keys) }
                    .firstOrNull()
        }
        is JSONArray -> (0 until value.length()).asSequence()
            .mapNotNull { index -> findArray(value.opt(index), *keys) }
            .firstOrNull()
        else -> null
    }

    private fun findInt(value: Any?, vararg keys: String): Int? = when (value) {
        is JSONObject -> {
            for (key in keys) {
                when (val raw = value.opt(key)) {
                    is Number -> return raw.toInt()
                    is String -> raw.toIntOrNull()?.let { return it }
                }
            }
            value.keys().asSequence()
                .mapNotNull { key -> findInt(value.opt(key), *keys) }
                .firstOrNull()
        }
        is JSONArray -> (0 until value.length()).asSequence()
            .mapNotNull { index -> findInt(value.opt(index), *keys) }
            .firstOrNull()
        else -> null
    }

    private fun firstString(value: JSONObject, vararg keys: String): String =
        keys.asSequence().map(value::optString).firstOrNull(String::isNotBlank).orEmpty()

    private fun firstLong(value: JSONObject, vararg keys: String): Long =
        keys.asSequence().mapNotNull { key ->
            when (val raw = value.opt(key)) {
                is Number -> raw.toLong()
                is String -> raw.toLongOrNull()
                else -> null
            }
        }.firstOrNull() ?: -1L

    private fun firstInt(value: JSONObject, vararg keys: String): Int? =
        keys.asSequence().mapNotNull { key ->
            when (val raw = value.opt(key)) {
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull()
                else -> null
            }
        }.firstOrNull()

    private fun hash33(value: String): Long {
        var hash = 5381L
        value.forEach { char ->
            hash += (hash shl 5) + char.code
            hash = hash and 0xFFFF_FFFFL
        }
        return hash and 0x7FFF_FFFFL
    }

    private data class SongWriteRef(val songId: Long, val songType: Int)

    private enum class MusicuCommStyle { WEB, ANDROID }

    internal companion object {
        const val LIKED_PAGE_SIZE = 200
        private const val WRITE_BATCH_SIZE = 50
        private const val ANDROID_CLIENT_TYPE = 11
        private const val ANDROID_CLIENT_VERSION = 14_090_008
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
