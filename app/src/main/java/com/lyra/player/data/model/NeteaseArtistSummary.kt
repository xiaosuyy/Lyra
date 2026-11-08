package com.lyra.player.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class NeteaseArtistSummary(
    @field:SerializedName(value = "id", alternate = ["m"])
    val id: Long,
    @field:SerializedName(value = "name", alternate = ["n"])
    val name: String
) : Parcelable
