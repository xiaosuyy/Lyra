package com.lyra.player.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import com.lyra.player.core.api.search.MusicPlatform
import com.lyra.player.data.sync.model.SyncCausalToken

// alternate 是旧 release(混淆构建)写进本地 JSON 的字段名, 只用于读取;
// 现在字段名已由 proguard 保留, 新数据一律用真实字段名写出
@Parcelize
data class SongItem(
    @field:SerializedName(value = "id", alternate = ["m"])
    val id: Long,
    @field:SerializedName(value = "name", alternate = ["n"])
    val name: String,
    @field:SerializedName(value = "artist", alternate = ["o"])
    val artist: String,
    @field:SerializedName(value = "album", alternate = ["p"])
    val album: String,
    @field:SerializedName(value = "albumId", alternate = ["q"])
    val albumId: Long,
    @field:SerializedName(value = "durationMs", alternate = ["r"])
    val durationMs: Long,
    @field:SerializedName(value = "coverUrl", alternate = ["s"])
    val coverUrl: String?,
    @field:SerializedName(value = "mediaUri", alternate = ["t"])
    val mediaUri: String? = null,
    @field:SerializedName(value = "matchedLyric", alternate = ["u"])
    val matchedLyric: String? = null,
    @field:SerializedName(value = "matchedTranslatedLyric", alternate = ["v"])
    val matchedTranslatedLyric: String? = null,
    @field:SerializedName(value = "matchedLyricSource", alternate = ["w"])
    val matchedLyricSource: MusicPlatform? = null,
    @field:SerializedName(value = "matchedSongId", alternate = ["x"])
    val matchedSongId: String? = null,
    @field:SerializedName(value = "userLyricOffsetMs", alternate = ["y"])
    val userLyricOffsetMs: Long = 0L,
    @field:SerializedName(value = "customCoverUrl", alternate = ["z"])
    val customCoverUrl: String? = null,
    @field:SerializedName(value = "customName", alternate = ["A"])
    val customName: String? = null,
    @field:SerializedName(value = "customArtist", alternate = ["B"])
    val customArtist: String? = null,
    @field:SerializedName(value = "originalName", alternate = ["C"])
    val originalName: String? = null,
    @field:SerializedName(value = "originalArtist", alternate = ["D"])
    val originalArtist: String? = null,
    @field:SerializedName(value = "originalCoverUrl", alternate = ["E"])
    val originalCoverUrl: String? = null,
    @field:SerializedName(value = "originalLyric", alternate = ["F"])
    val originalLyric: String? = null,
    @field:SerializedName(value = "originalTranslatedLyric", alternate = ["G"])
    val originalTranslatedLyric: String? = null,
    @field:SerializedName(value = "localFileName", alternate = ["H"])
    val localFileName: String? = null,
    @field:SerializedName(value = "localFilePath", alternate = ["I"])
    val localFilePath: String? = null,
    @field:SerializedName(value = "channelId", alternate = ["J"])
    val channelId: String? = null,
    @field:SerializedName(value = "audioId", alternate = ["K"])
    val audioId: String? = null,
    @field:SerializedName(value = "subAudioId", alternate = ["L"])
    val subAudioId: String? = null,
    @field:SerializedName(value = "playlistContextId", alternate = ["M"])
    val playlistContextId: String? = null,
    @field:SerializedName(value = "sourceStableKey", alternate = ["N"])
    val sourceStableKey: String? = null,
    @field:SerializedName(value = "streamUrl", alternate = ["O"])
    val streamUrl: String? = null,
    @field:SerializedName(value = "neteaseArtists", alternate = ["P"])
    val neteaseArtists: List<NeteaseArtistSummary>? = emptyList(),
    @field:SerializedName(value = "addedAt", alternate = ["Q"])
    val addedAt: Long = 0L,
    @field:SerializedName(value = "syncMembershipTokens", alternate = ["R"])
    val syncMembershipTokens: List<SyncCausalToken>? = emptyList()
) : Parcelable
