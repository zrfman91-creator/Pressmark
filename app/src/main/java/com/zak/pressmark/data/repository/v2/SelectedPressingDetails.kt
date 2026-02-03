// FILE: app/src/main/java/com/zak/pressmark/data/repository/v2/SelectedPressingDetails.kt
package com.zak.pressmark.data.repository.v2

/**
 * A lightweight, display-friendly projection of the user's currently selected pressing for a Work.
 * Source-of-truth is Variant(key="default") -> Pressing -> Release.
 */
data class SelectedPressingDetails(
    val label: String?,
    val catalogNo: String?,
    val country: String?,
    val year: Int?,
    val format: String?,
    val discogsReleaseId: Long?,
    val artworkUri: String?, // ✅ ADD THIS
)