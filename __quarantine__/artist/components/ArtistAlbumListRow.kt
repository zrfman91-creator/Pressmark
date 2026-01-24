// FILE: app/src/main/java/com/zak/pressmark/feature/artist/components/ArtistAlbumListRow.kt
package com.zak.pressmark.feature.artist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Artist-screen row:
 * - No click (prevents deep details -> artist loops)
 * - No per-row menus
 * - Compact, information-forward
 */
@Composable
fun ArtistAlbumListRow(
    album: AlbumEntity,
    modifier: Modifier = Modifier,
) {
    val meta = remember(album.releaseYear, album.label, album.catalogNo) {
        buildArtistMetaLine(
            year = album.releaseYear,
            label = album.label,
            catalogNo = album.catalogNo,
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun buildArtistMetaLine(
    year: Int?,
    label: String?,
    catalogNo: String?,
): String {
    val parts = buildList {
        year?.let { add(it.toString()) }
        label?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
        catalogNo?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    return parts.joinToString(" • ")
}
