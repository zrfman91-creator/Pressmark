// FILE: app/src/main/java/com/zak/pressmark/feature/addalbum/state/DiscogsAutoFillUi.kt
package com.zak.pressmark.feature.addalbum.state

data class DiscogsAutofillUi(
    val albumId: Long,
    val details: AlbumAutofillDetails,
    val willFillLabels: List<String>,
    val discogsSubtitle: String = "",
)

data class AlbumAutofillDetails(    // Fill-missing-only candidate fields.
    val year: Int? = null,
    val catNo: String? = null,
    val label: String? = null,
    val format: String? = null,
    val rpm: Int? = null,
)
