// FILE: app/src/main/java/com/zak/pressmark/feature/albumdetails/components/AlbumDetailsMetaRow.kt
package com.zak.pressmark.feature.albumdetails.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun AlbumDetailsMetaRow(
    year: Int?,
    label: String?,
    catalogNo: String?,
    modifier: Modifier = Modifier,
) {
    val meta = buildString {
        if (year != null) append(year)

        val safeLabel = label?.trim().takeIf { !it.isNullOrBlank() }
        if (safeLabel != null) {
            if (isNotEmpty()) append(" • ")
            append(safeLabel)
        }

        val safeCat = catalogNo?.trim().takeIf { !it.isNullOrBlank() }
        if (safeCat != null) {
            if (isNotEmpty()) append(" • ")
            append(safeCat)
        }
    }.ifBlank { "—" }

    Text(
        modifier = modifier.fillMaxWidth(),
        text = meta,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
