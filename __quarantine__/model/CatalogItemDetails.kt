package com.zak.pressmark.data.model

data class CatalogItemDetails(
    val catalogItemId: String,
    val displayTitle: String,
    val displayArtistLine: String,
    val primaryArtworkUri: String?,
    val releaseYear: Int?,
    val state: String,
    val master: MasterIdentitySummary?,
    val pressings: List<CatalogPressingDetails>,
)

data class MasterIdentitySummary(
    val provider: String,
    val masterId: String?,
    val title: String,
    val artistLine: String,
    val year: Int?,
    val genres: String?,
    val styles: String?,
    val artworkUri: String?,
)

data class CatalogPressingSummary(
    val pressingId: String,
    val catalogItemId: String,
    val releaseId: String?,
    val evidenceScore: Int?,
    val title: String?,
    val label: String?,
    val catalogNo: String?,
    val country: String?,
    val releaseYear: Int?,
)

data class CatalogVariantSummary(
    val variantId: String,
    val pressingId: String,
    val variantKey: String,
    val notes: String?,
)

data class CatalogPressingDetails(
    val summary: CatalogPressingSummary,
    val variants: List<CatalogVariantSummary>,
)
