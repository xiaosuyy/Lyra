package com.lyra.player.core.player.resolver

import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.data.model.SongItem
import java.io.InputStream
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "LYRA-PlayerManager"
private const val IDENTITY_PREFIX_BYTES = 24 * 1024
private const val IDENTITY_VERIFY_TIMEOUT_MS = 1_200L
private const val MAX_ID3_TEXT_BYTES = 4 * 1024

private val identityNonTextRegex = Regex("[^\\p{L}\\p{N}]+")
private val identityArtistSplitRegex =
    Regex("[/、,，&＆;；]|\\b(?:feat|ft|with)\\b\\.?", RegexOption.IGNORE_CASE)

/**
 * 聚合接口偶尔会按歌名搜到另一首歌并直接返回直链，元数据对得上、录音却不对。
 * 播放前抽查文件头部的 ID3 标签，与目标歌曲对不上就丢弃；
 * 没有标签或网络失败时放行，不阻断正常播放。
 */
internal suspend fun verifyRemoteAudioIdentity(
    client: OkHttpClient,
    url: String,
    song: SongItem
): Boolean = withContext(Dispatchers.IO) {
    val tags = withTimeoutOrNull(IDENTITY_VERIFY_TIMEOUT_MS) {
        runCatching { fetchLeadingId3(client, url) }.getOrElse { error ->
            LyraLogger.d(TAG, "音频标签校验跳过: ${error.message}")
            null
        }
    } ?: return@withContext true
    audioIdentityMatches(song, tags.first, tags.second)
}

internal fun audioIdentityMatches(song: SongItem, tagTitle: String?, tagArtist: String?): Boolean {
    if (tagTitle == null && tagArtist == null) return true
    val targetTitle = compactIdentityText(song.originalName ?: song.name)
    val targetArtist = compactIdentityText(song.originalArtist ?: song.artist)
    val titleMatches = tagTitle == null ||
        (targetTitle.isNotEmpty() && compactIdentityText(tagTitle) == targetTitle)
    val tagArtists = tagArtist?.let(::identityArtistTokens).orEmpty()
    val targetArtists = identityArtistTokens(song.originalArtist ?: song.artist)
    val artistMatches = tagArtist == null ||
        tagArtists.isEmpty() ||
        targetArtist.isEmpty() ||
        tagArtists.any { it in targetArtists }
    if (!titleMatches || !artistMatches) {
        LyraLogger.w(
            TAG,
            "音频标签与歌曲不符，丢弃直链: song=${song.name}, tagTitle=$tagTitle, tagArtist=$tagArtist"
        )
        return false
    }
    return true
}

private fun fetchLeadingId3(client: OkHttpClient, url: String): Pair<String?, String?>? {
    val request = Request.Builder()
        .url(url)
        .header("Range", "bytes=0-${IDENTITY_PREFIX_BYTES - 1}")
        .build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val bytes = response.body.byteStream().use { it.readPrefix(IDENTITY_PREFIX_BYTES) }
        return parseId3Text(bytes)
    }
}

private fun InputStream.readPrefix(limit: Int): ByteArray {
    val buffer = ByteArray(limit)
    var offset = 0
    while (offset < limit) {
        val read = read(buffer, offset, limit - offset)
        if (read <= 0) break
        offset += read
    }
    return if (offset == limit) buffer else buffer.copyOf(offset)
}

internal fun parseId3Text(bytes: ByteArray): Pair<String?, String?>? {
    if (bytes.size < 10 ||
        bytes[0] != 'I'.code.toByte() ||
        bytes[1] != 'D'.code.toByte() ||
        bytes[2] != '3'.code.toByte()
    ) {
        return null
    }
    val major = bytes[3].toInt() and 0xFF
    val flags = bytes[5].toInt() and 0xFF
    if (major !in 2..4 || flags and 0x80 != 0) return null
    val tagSize = syncSafeInt(bytes, 6)
    val tagEnd = minOf(bytes.size, 10 + tagSize)
    var cursor = 10
    if (flags and 0x40 != 0) {
        if (major >= 4) {
            cursor += syncSafeInt(bytes, 10)
        } else {
            cursor += 4 + bigEndianInt(bytes, 10)
        }
    }
    val frameHeaderSize = if (major == 2) 6 else 10
    val frameIdSize = if (major == 2) 3 else 4
    val titleId = if (major == 2) "TT2" else "TIT2"
    val artistId = if (major == 2) "TP1" else "TPE1"
    var title: String? = null
    var artist: String? = null
    while (cursor + frameHeaderSize <= tagEnd) {
        val id = String(bytes, cursor, frameIdSize)
        if (id[0] == '\u0000') break
        val frameSize = if (major == 2) {
            ((bytes[cursor + 3].toInt() and 0xFF) shl 16) or
                ((bytes[cursor + 4].toInt() and 0xFF) shl 8) or
                (bytes[cursor + 5].toInt() and 0xFF)
        } else if (major >= 4) {
            syncSafeInt(bytes, cursor + 4)
        } else {
            bigEndianInt(bytes, cursor + 4)
        }
        val dataStart = cursor + frameHeaderSize
        if (frameSize <= 0 || dataStart + frameSize > bytes.size) break
        when (id) {
            titleId -> title = decodeId3Text(bytes, dataStart, frameSize)
            artistId -> artist = decodeId3Text(bytes, dataStart, frameSize)
        }
        if (title != null && artist != null) break
        cursor = dataStart + frameSize
    }
    return if (title == null && artist == null) null else title to artist
}

private fun decodeId3Text(bytes: ByteArray, start: Int, size: Int): String? {
    if (size <= 1) return null
    val encoding = bytes[start].toInt() and 0xFF
    val textSize = minOf(size - 1, MAX_ID3_TEXT_BYTES)
    val payload = bytes.copyOfRange(start + 1, start + 1 + textSize)
    val charset = when (encoding) {
        0 -> Charsets.ISO_8859_1
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        3 -> Charsets.UTF_8
        else -> return null
    }
    return String(payload, charset)
        .trim('\u0000', '\uFEFF', ' ')
        .takeIf { it.isNotBlank() }
}

private fun syncSafeInt(bytes: ByteArray, offset: Int): Int {
    if (offset + 4 > bytes.size) return 0
    return ((bytes[offset].toInt() and 0x7F) shl 21) or
        ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
        ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
        (bytes[offset + 3].toInt() and 0x7F)
}

private fun bigEndianInt(bytes: ByteArray, offset: Int): Int {
    if (offset + 4 > bytes.size) return 0
    return ((bytes[offset].toInt() and 0xFF) shl 24) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
        (bytes[offset + 3].toInt() and 0xFF)
}

private fun compactIdentityText(value: String): String {
    return identityNonTextRegex.replace(value.lowercase(), "")
}

private fun identityArtistTokens(value: String): List<String> {
    return value.split(identityArtistSplitRegex)
        .map(::compactIdentityText)
        .filter { it.isNotBlank() }
}
