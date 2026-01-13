package com.zak.pressmark.feature.albumlist.coversearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.zak.pressmark.data.remote.discogs.DiscogsSearchResult

/**
 * A dialog that displays a list of potential matches from Discogs
 * for the user to select a cover image.
 */
@Composable
fun DiscogsCoverSearchDialog(
    artist: String,
    title: String,
    results: List<DiscogsSearchResult>,
    onPick: (DiscogsSearchResult) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp) // Constrain the height of the dialog
        ) {
            Column {
                Text(
                    text = "Results for '$title'",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(results, key = { it.id }) { result ->
                        // *** THIS IS THE KEY PART ***
                        // Each item in the list is a row with artwork and text
                        SearchResultRow(
                            result = result,
                            onClick = { onPick(result) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: DiscogsSearchResult,
    onClick: () -> Unit,
) {
    // This helper property correctly finds the best available image URL.
    val imageUrl = result.coverImage ?: result.thumb

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use AsyncImage to load the artwork from the URL
        AsyncImage(
            model = imageUrl,
            contentDescription = "Cover for ${result.title}",
            modifier = Modifier.size(56.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title ?: "Unknown Title",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Display year and label if available for better context
            val subtitle = listOfNotNull(result.year, result.label?.firstOrNull())
                .joinToString(" • ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
