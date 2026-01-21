// FILE: app/src/main/java/com/zak/pressmark/feature/albumdetails/components/AlbumDetailsInfoSection.kt
package com.zak.pressmark.feature.albumdetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AlbumDetailsInfoSection(
    year: Int?,
    label: String?,
    catalogNo: String?,
    format: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Column(
            // Using a more standard, consistent padding for the content.
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Conditionally render rows if data is present to avoid empty sections
            // This is an alternative if you prefer not to show the row at all vs. showing "—"
            year?.toString()?.let {
                AlbumDetailsInfoRow(label = "Year", value = it)
            }
            label?.let {
                AlbumDetailsInfoRow(label = "Label", value = it)
            }
            catalogNo?.let {
                AlbumDetailsInfoRow(label = "Catalog #", value = it)
            }
            format?.let {
                AlbumDetailsInfoRow(label = "Format", value = it)
            }
        }
    }
}

@Composable
private fun AlbumDetailsInfoRow(
    label: String,
    value: String, // Value is now non-nullable as we check before calling
    modifier: Modifier = Modifier,
) {
    // Simplified the logic in the row as the check is now done at the call site.
    val displayValue = value.trim().takeIf { it.isNotBlank() } ?: "—"

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
