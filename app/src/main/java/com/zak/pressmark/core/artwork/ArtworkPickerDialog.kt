package com.zak.pressmark.core.artwork

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage

@Composable
fun ArtworkPickerDialog(
    artist: String,
    title: String,
    results: List<ArtworkCandidate>,
    onPick: (ArtworkCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            Column {
                Text(
                    text = "Results for '$title'",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = results,
                        key = { "${it.provider}:${it.providerItemId}" }
                    ) { candidate ->
                        ArtworkCandidateRow(
                            candidate = candidate,
                            onClick = { onPick(candidate) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkCandidateRow(
    candidate: ArtworkCandidate,
    onClick: () -> Unit,
) {
    val imageUrl = candidate.imageUrl ?: candidate.thumbUrl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Cover for ${candidate.displayTitle}",
            modifier = Modifier.size(56.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.displayTitle.ifBlank { "Unknown Title" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitle = candidate.subtitle
                ?: candidate.displayArtist
                ?: providerLabel(candidate.provider)

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

private fun providerLabel(provider: ArtworkProviderId): String =
    when (provider) {
        ArtworkProviderId.DISCOGS -> "Discogs"
        ArtworkProviderId.MUSICBRAINZ -> "MusicBrainz"
    }
