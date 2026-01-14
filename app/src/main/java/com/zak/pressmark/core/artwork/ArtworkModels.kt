package com.zak.pressmark.core.artwork

data class ArtworkQuery(
    val albumId: String,
    val artist: String,
    val title: String,
    val year: Int? = null,
    val catalogNo: String? = null,
)

enum class ArtworkProviderId {
    DISCOGS,
    MUSICBRAINZ,
}

data class ArtworkCandidate(
    val provider: ArtworkProviderId,
    val providerItemId: String,
    val imageUrl: String?,
    val thumbUrl: String? = null,

    val displayTitle: String,
    val displayArtist: String? = null,

    val subtitle: String? = null, // e.g., "1984 • Epic"
    val confidence: Int = 0,      // reserved for later scoring/verification
    val reasons: List<String> = emptyList(),
)
