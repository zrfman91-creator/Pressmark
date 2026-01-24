package com.zak.pressmark.data.model

data class CatalogItemSummary(
    val catalogItemId: String,
    val displayTitle: String,
    val displayArtistLine: String,
    val primaryArtworkUri: String?,
    val releaseYear: Int?,
    val addedAt: Long,
    val state: String,
)
