@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.feature.artworkpicker.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.zak.pressmark.feature.artworkpicker.vm.DiscogsPressingCandidateUi

@Composable
fun DiscogsPressingPickerDialog(
    artist: String,
    title: String,
    candidates: List<DiscogsPressingCandidateUi>,
    selectedId: Long?,
    onSelectCandidate: (Long?) -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onTakePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selected = candidates.firstOrNull { it.candidate.discogsReleaseId == selectedId }
        ?: candidates.firstOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Select a pressing",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Match '$title' by $artist. Discogs only fills fields you left blank.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider()

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = selected?.candidate?.title ?: "Select a pressing",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Pressing candidates") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        candidates.forEach { item ->
                            val candidate = item.candidate
                            DropdownPressingRow(
                                candidateTitle = candidate.title,
                                summaryLine = summaryLine(candidate),
                                confidence = candidate.confidenceScore,
                                onClick = {
                                    expanded = false
                                    onSelectCandidate(candidate.discogsReleaseId)
                                },
                            )
                        }
                    }
                }

                selected?.let { selection ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val imageUrl = selection.candidate.coverUrl ?: selection.candidate.thumbUrl
                        if (!imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Discogs cover preview",
                                modifier = Modifier.size(80.dp),
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = summaryLine(selection.candidate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Confidence ${selection.candidate.confidenceScore}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Will fill",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (selection.fillLabels.isEmpty()) {
                            Text(
                                text = "No additional fields will be filled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            selection.fillLabels.forEach { label ->
                                Text(
                                    text = "• $label",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onTakePhoto,
                    ) {
                        Text("Take Photo")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onSkip,
                    ) {
                        Text("Keep my entry")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        enabled = selected != null,
                    ) {
                        Text("Use this pressing")
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownPressingRow(
    candidateTitle: String,
    summaryLine: String,
    confidence: Int,
    onClick: () -> Unit,
) {
    androidx.compose.material3.DropdownMenuItem(
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = candidateTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summaryLine.isNotBlank()) {
                    Text(
                        text = summaryLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "Confidence $confidence",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        onClick = onClick,
    )
}

private fun summaryLine(candidate: com.zak.pressmark.data.model.ReleaseDiscogsCandidate): String =
    buildList {
        if (candidate.isVinyl) add("Vinyl")
        candidate.label?.takeIf { it.isNotBlank() }?.let { add(it) }
        candidate.catalogNo?.takeIf { it.isNotBlank() }?.let { add(it) }
        candidate.year?.let { add(it.toString()) }
    }.joinToString(" • ")
