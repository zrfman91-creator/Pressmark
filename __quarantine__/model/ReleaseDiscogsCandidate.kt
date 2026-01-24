package com.zak.pressmark.data.model

data class ReleaseDiscogsCandidate(
    val discogsReleaseId: Long,
    val title: String,
    val artist: String,
    val year: Int?,
    val label: String?,
    val catalogNo: String?,
    val format: String?,
    val barcode: String?,
    val coverUrl: String?,
    val thumbUrl: String?,
    val country: String?,
    val releaseType: String?,
    val notes: String?,
    val genres: List<String>,
    val styles: List<String>,
    val confidenceScore: Int,
    val isVinyl: Boolean,
)
