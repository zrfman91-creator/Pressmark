package com.zak.pressmark.feature.catalogdetails.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.ui.elements.AlbumArtwork
import com.zak.pressmark.data.model.CatalogItemDetails
import com.zak.pressmark.data.model.CatalogPressingDetails
import com.zak.pressmark.feature.catalog.components.TopAppBar

@Composable
fun CatalogDetailsScreen(
    details: CatalogItemDetails?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = details?.displayTitle ?: "Catalog",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (details == null) {
                    Text(
                        text = "Loading catalog item…",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    return@Column
                }

                Text(
                    text = "Master info",
                    style = MaterialTheme.typography.titleMedium,
                )
                MasterInfoSection(details)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PressingsSection(details.pressings)
            }
        }
    }
}

@Composable
private fun MasterInfoSection(details: CatalogItemDetails) {
    val master = details.master
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtwork(
            artworkUrl = details.primaryArtworkUri ?: master?.artworkUri,
            contentDescription = "${details.displayArtistLine} — ${details.displayTitle}",
            size = 140.dp,
            cornerRadius = 12.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = details.displayTitle,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = details.displayArtistLine,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val year = details.releaseYear ?: master?.year
            if (year != null) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            master?.let {
                val genres = listOfNotNull(it.genres, it.styles)
                    .joinToString(" · ")
                    .takeIf { value -> value.isNotBlank() }
                if (!genres.isNullOrBlank()) {
                    Text(
                        text = genres,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = details.state.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    Button(
        onClick = { /* TODO: refine pressing flow */ },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text("Refine pressing")
    }
}

@Composable
private fun PressingsSection(pressings: List<CatalogPressingDetails>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Owned Pressings / Variants",
            style = MaterialTheme.typography.titleMedium,
        )
        if (pressings.isEmpty()) {
            Text(
                text = "No pressings confirmed yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        pressings.forEach { pressing ->
            PressingCard(pressing)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun PressingCard(pressing: CatalogPressingDetails) {
    val summary = pressing.summary
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = summary.title ?: "Pressing",
            style = MaterialTheme.typography.titleSmall,
        )
        val meta = listOfNotNull(
            summary.label?.takeIf { it.isNotBlank() },
            summary.catalogNo?.takeIf { it.isNotBlank() },
            summary.country?.takeIf { it.isNotBlank() },
            summary.releaseYear?.toString(),
        )
        if (meta.isNotEmpty()) {
            Text(
                text = meta.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (pressing.variants.isNotEmpty()) {
            Text(
                text = "Variants:",
                style = MaterialTheme.typography.labelMedium,
            )
            pressing.variants.forEach { variant ->
                Text(
                    text = "• ${variant.variantKey}${variant.notes?.let { " — $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
