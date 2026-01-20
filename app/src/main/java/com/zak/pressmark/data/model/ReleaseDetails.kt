package com.zak.pressmark.data.model

data class ReleaseDetails(
    val releaseId: String,
    val title: String,
    val artistLine: String,
    val releaseYear: Int?,
    val label: String?,
    val catalogNo: String?,
    val format: String?,
    val barcode: String?,
    val country: String?,
    val releaseType: String?,
    val notes: String?,
    val rating: Int?,
    val addedAt: Long,
    val lastPlayedAt: Long?,
    val artwork: ReleaseArtwork?,
    val credits: List<ReleaseCredit>,
)

data class ReleaseArtwork(
    val id: Long,
    val uri: String,
    val isPrimary: Boolean,
    val kind: String,
    val source: String,
    val width: Int?,
    val height: Int?,
)

data class ReleaseCredit(
    val artistId: Long,
    val artistName: String,
    val role: String,
    val position: Int,
    val displayHint: String?,
)
