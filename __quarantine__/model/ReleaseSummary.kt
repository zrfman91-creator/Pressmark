package com.zak.pressmark.data.model

data class ReleaseSummary(
    val releaseId: String,
    val title: String,
    val artistLine: String,
    val releaseYear: Int?,
    val artworkUri: String?,
    val catalogNo: String?,
    val barcode: String?,
    val label: String?,
    val country: String?,
    val format: String?,
    val releaseType: String?,
    val addedAt: Long,
)
