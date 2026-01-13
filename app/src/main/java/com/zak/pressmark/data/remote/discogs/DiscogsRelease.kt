// path: app/src/main/java/com/zak/pressmark/data/remote/discogs/DiscogsRelease.kt
package com.zak.pressmark.data.remote.discogs

import com.google.gson.annotations.SerializedName

/**
 * Data models for the Discogs API /releases/{id} endpoint.
 * This represents all the useful fields you can get for a single album release.
 */
data class DiscogsRelease(
    // --- Core Album Info ---
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: Int?,

    // --- Artist & Label Info ---
    @SerializedName("artists") val artists: List<DiscogsArtist>?,
    @SerializedName("labels") val labels: List<DiscogsLabel>?,

    // --- Genre & Style Info ---
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("styles") val styles: List<String>?,

    // --- Tracklist & Artwork ---
    @SerializedName("tracklist") val tracklist: List<DiscogsTrack>?,
    @SerializedName("images") val images: List<DiscogsImage>?,

    // --- Detailed & Extra Info ---
    @SerializedName("notes") val notes: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("videos") val videos: List<DiscogsVideo>?,
    @SerializedName("master_id") val masterId: Long?,
    @SerializedName("formats") val formats: List<DiscogsFormat>?,
)
data class DiscogsFormat(val name: String)
// --- Sub-Objects ---

data class DiscogsArtist(
    @SerializedName("name") val name: String
)

data class DiscogsLabel(
    @SerializedName("name") val name: String,
    @SerializedName("catno") val catalogNumber: String
)

data class DiscogsTrack(
    @SerializedName("position") val position: String,
    @SerializedName("title") val title: String,
    @SerializedName("duration") val duration: String?
)

data class DiscogsImage(
    @SerializedName("type") val type: String, // "primary" or "secondary"
    @SerializedName("uri") val uri: String, // Full-size image URL
    @SerializedName("uri150") val uri150: String, // 150x150 thumbnail URL
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

data class DiscogsVideo(
    @SerializedName("uri") val uri: String, // e.g., a YouTube URL
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?
)

data class DiscogsSearchResponse(val results: List<DiscogsSearchResult> = emptyList(),
)

data class DiscogsSearchResult(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String?,
    @SerializedName("year") val year: String?, // Discogs sometimes sends year as a String
    @SerializedName("label") val label: List<String>?,
    @SerializedName("catno") val catno: String?,
    @SerializedName("thumb") val thumb: String?,
    @SerializedName("cover_image") val coverImage: String?,
)