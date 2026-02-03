package com.zak.pressmark.feature.ingest.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zak.pressmark.feature.ingest.vm.BarcodeMasterCandidateUi
import com.zak.pressmark.feature.ingest.vm.DiscogsCandidateUi

@Composable
fun LookupResultsDialog(
    artistLine: String,
    releaseTitle: String,
    yearText: String? = null,
    imageUrl: String? = null,
    onDismiss: () -> Unit,
    onConfirmAdd: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Add this release?",
    confirmLabel: String = "Add",
    dismissLabel: String = "Cancel",
    helperText: String = "You can always refine variants afterward.",
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
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = releaseTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = buildAnnotatedString {
                                append(artistLine)
                                if (!yearText.isNullOrBlank()) {
                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.Normal,
                                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    ) {
                                        append(" \u00B7 ")
                                    }
                                    append(yearText)
                                }
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            shape = RoundedCornerShape(4.dp),
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Text(
                                text = dismissLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Button(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            onClick = onConfirmAdd,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(
                                text = confirmLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        text = helperText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    )
}

@Composable
fun LookupResultsDialog(
    candidate: BarcodeMasterCandidateUi,
    onDismiss: () -> Unit,
    onConfirmAdd: (BarcodeMasterCandidateUi) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Add this release?",
    confirmLabel: String = "Add",
    dismissLabel: String = "Cancel",
    helperText: String = "If this looks right, add it to your library. You can refine variants afterward.",
) {
    val thumb = candidate.thumbUrl ?: candidate.coverUrl
    LookupResultsDialog(
        artistLine = candidate.artistLine,
        releaseTitle = candidate.releaseTitle,
        yearText = candidate.year?.toString(),
        imageUrl = thumb,
        onDismiss = onDismiss,
        onConfirmAdd = { onConfirmAdd(candidate) },
        modifier = modifier,
        title = title,
        confirmLabel = confirmLabel,
        dismissLabel = dismissLabel,
        helperText = helperText,
    )
}

@Composable
fun LookupResultsDialog(
    candidate: DiscogsCandidateUi,
    onDismiss: () -> Unit,
    onConfirmAdd: (DiscogsCandidateUi) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Add this release?",
    confirmLabel: String = "Add",
    dismissLabel: String = "Cancel",
    helperText: String = "If this looks right, add it to your library. You can refine variants afterward.",
) {
    val (artistLine, releaseTitle) = splitArtistTitle(candidate.title)
    val thumb = candidate.coverUrl ?: candidate.thumbUrl
    LookupResultsDialog(
        artistLine = artistLine,
        releaseTitle = releaseTitle,
        yearText = candidate.year?.toString(),
        imageUrl = thumb,
        onDismiss = onDismiss,
        onConfirmAdd = { onConfirmAdd(candidate) },
        modifier = modifier,
        title = title,
        confirmLabel = confirmLabel,
        dismissLabel = dismissLabel,
        helperText = helperText,
    )
}

private fun splitArtistTitle(text: String): Pair<String, String> {
    val parts = text.split(" - ", limit = 2)
    return if (parts.size == 2) {
        parts[0].trim().ifBlank { "Unknown Artist" } to parts[1].trim().ifBlank { text.trim() }
    } else {
        "Unknown Artist" to text.trim()
    }
}
