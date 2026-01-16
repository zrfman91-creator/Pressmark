// =======================================================
// file: app/src/main/java/com/zak/pressmark/feature/albumdetails/components/NotesSection.kt
// =======================================================
package com.zak.pressmark.feature.albumdetails.components


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AlbumDetailsNotesSection(
    notes: String?,
    modifier: Modifier = Modifier,
) {
    val value = notes?.trim().takeIf { !it.isNullOrBlank() } ?: "Notes functionality coming soon!"

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 8.dp),
            )
        }
    }
}
