package com.zak.pressmark.data.remote.discogs

import com.google.gson.annotations.SerializedName

data class DiscogsSearchResponse(
    @SerializedName("results") val results: List<DiscogsSearchResultDto> = emptyList(),
)

data class DiscogsSearchResultDto(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String? = null, // "release" | "master" | etc.
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("thumb") val thumb: String? = null,
    @SerializedName("cover_image") val coverImage: String? = null,
    @SerializedName("master_id") val masterId: Long? = null,
    @SerializedName("genre") val genre: List<String>? = null,
    @SerializedName("style") val style: List<String>? = null,
)
