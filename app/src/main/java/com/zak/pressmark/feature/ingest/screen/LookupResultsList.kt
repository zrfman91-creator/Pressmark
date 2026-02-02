// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/screen/DiscogsResultsListDialog.kt
package com.zak.pressmark.feature.ingest.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zak.pressmark.feature.ingest.vm.DiscogsCandidateUi

@Composable
fun DiscogsResultsListDialog(
    candidates: List<DiscogsCandidateUi>,
    onSelect: (DiscogsCandidateUi) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Choose a match",
    dismissLabel: String = "Cancel",
) {
    AlertDialog(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                candidates.forEach { item ->
                    DiscogsCandidateRow(
                        item = item,
                        onClick = { onSelect(item) },
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) { Text(dismissLabel) }
        },
    )
}

@Composable
private fun DiscogsCandidateRow(
    item: DiscogsCandidateUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val thumb = item.thumbUrl ?: item.coverUrl
            if (!thumb.isNullOrBlank()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Spacer(modifier = Modifier.size(44.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitleParts = buildList {
                    item.subtitle?.takeIf { it.isNotBlank() }?.let { add(it) }
                    item.year?.let { add(it.toString()) }
                }
                if (subtitleParts.isNotEmpty()) {
                    Text(
                        text = subtitleParts.joinToString(" \u00B7 "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
