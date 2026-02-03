@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.feature.workdetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * "About this album" = master-level (canonical) details only.
 * This section may show master artwork, but must never imply "owned" (that's handled by Your Pressing).
 */
@Composable
fun AboutThisAlbumSection(
    modifier: Modifier = Modifier,
    title: String,
    artist: String,
    masterYear: Int?,
    genres: List<String>,
    styles: List<String>,
    discogsMasterId: Long? = null,
    masterArtworkUri: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "About this album",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Master artwork (canonical)
            if (!masterArtworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = masterArtworkUri,
                    contentDescription = "Master album artwork",
                    modifier = Modifier
                        .size(128.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Never show "null" or empty junk — either show year or show nothing.
                masterYear
                    ?.takeIf { it > 0 }
                    ?.let { yearValue ->
                        Text(
                            text = yearValue.toString(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                // Keep Genres and Styles separate (as requested)
                AboutThisAlbumTags(
                    genres = genres,
                    styles = styles,
                )
            }
        }

        discogsMasterId?.let { id ->
            Text(
                text = "Discogs master: $id",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(2.dp))
    }
}

/**
 * Genres and Styles are intentionally separate (no consolidation).
 */
@Composable
fun AboutThisAlbumTags(
    genres: List<String>,
    styles: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val genresLine = genres
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
            .ifBlank { "—" }

        val stylesLine = styles
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
            .ifBlank { "—" }

        Text(
            text = "Genres: $genresLine",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Styles: $stylesLine",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
