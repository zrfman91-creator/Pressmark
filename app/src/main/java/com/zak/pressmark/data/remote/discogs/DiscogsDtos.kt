package com.zak.pressmark.data.remote.discogs

import com.google.gson.annotations.SerializedName

data class DiscogsSearchResponse(
    @SerializedName("results") val results: List<DiscogsSearchResultDto> = emptyList(),
)

data class DiscogsSearchResultDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("thumb") val thumb: String? = null,
    @SerializedName("cover_image") val coverImage: String? = null,
    @SerializedName("genre") val genre: List<String>? = null,
    @SerializedName("style") val style: List<String>? = null,
)

data class DiscogsMasterResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("genres") val genres: List<String>? = null,
    @SerializedName("styles") val styles: List<String>? = null,
    @SerializedName("tracklist") val tracklist: List<DiscogsTrackDto>? = null,
    @SerializedName("images") val images: List<DiscogsImageDto>? = null,
)

data class DiscogsTrackDto(
    @SerializedName("position") val position: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("duration") val duration: String? = null,
)

data class DiscogsImageDto(
    @SerializedName("uri") val uri: String? = null,
    @SerializedName("uri150") val uri150: String? = null,
    @SerializedName("type") val type: String? = null,
)
