package com.zak.pressmark.feature.addalbum.state

/**
 * Batch E
 * UI model for Discogs autofill consent.
 *
 * IMPORTANT: Feature-scoped (only used by Add Album flow right now).
 */
data class DiscogsAutofillUi(
    val albumId: Long,
    val details: AlbumAutofillDetails,
    val willFillLabels: List<String>,
    val discogsSubtitle: String = "",
)

/**
 * Fill-missing-only candidate fields.
 * Keep this minimal; add more later when you wire deeper Discogs parsing.
 */
data class AlbumAutofillDetails(
    val year: Int? = null,
    val catNo: String? = null,
    val label: String? = null,
    val format: String? = null,
    val rpm: Int? = null,
)
