@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.feature.resolveinbox.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.local.entity.ProviderSnapshotEntity

@Composable
fun ResolveInboxItemScreen(
    inboxItem: InboxItemEntity?,
    candidates: List<ProviderSnapshotEntity>,
    selectedCandidateId: String?,
    errorMessage: String?,
    onSelectCandidate: (String?) -> Unit,
    onCommit: () -> Unit,
    onAddDetails: (title: String?, artist: String?, label: String?, catalogNo: String?, format: String?) -> Unit,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddDetails by remember { mutableStateOf(false) }

    LaunchedEffect(candidates) {
        if (selectedCandidateId == null && candidates.isNotEmpty()) {
            onSelectCandidate(candidates.first().id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resolve Inbox Item") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = inboxItem?.extractedTitle ?: inboxItem?.rawTitle ?: "Untitled",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = inboxItem?.extractedArtist ?: inboxItem?.rawArtist ?: "Unknown artist",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Top candidates",
                style = MaterialTheme.typography.titleMedium,
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(candidates, key = { it.id }) { candidate ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row {
                            RadioButton(
                                selected = candidate.id == selectedCandidateId,
                                onClick = { onSelectCandidate(candidate.id) },
                            )
                            Column(modifier = Modifier.padding(top = 6.dp)) {
                                Text(candidate.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${candidate.artist} • ${candidate.label ?: ""} ${candidate.catalogNo ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                candidate.reasonsJson?.let { reasons ->
                                    val summary = summarizeReasons(reasons)
                                    if (summary.isNotBlank()) {
                                        Text(
                                            text = summary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                }
                                Text(
                                    text = "Confidence ${candidate.confidence}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCommit,
                enabled = selectedCandidateId != null,
            ) {
                Text("Confirm and commit")
            }

            TextButton(onClick = { showAddDetails = true }) {
                Text("None of these? Add details")
            }
        }
    }

    if (showAddDetails) {
        AddDetailsBottomSheet(
            title = inboxItem?.extractedTitle ?: inboxItem?.rawTitle,
            artist = inboxItem?.extractedArtist ?: inboxItem?.rawArtist,
            label = inboxItem?.extractedLabel,
            catalogNo = inboxItem?.extractedCatalogNo,
            onDismiss = { showAddDetails = false },
            onSave = { title, artist, label, catalogNo, format ->
                onAddDetails(title, artist, label, catalogNo, format)
                showAddDetails = false
            },
        )
    }

    errorMessage?.let { message ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissError,
            confirmButton = { TextButton(onClick = onDismissError) { Text("OK") } },
            title = { Text("Unable to commit") },
            text = { Text(message) },
        )
    }
}

private fun summarizeReasons(rawJson: String): String {
    return runCatching {
        val reasons = org.json.JSONObject(rawJson)
            .optJSONArray("reasons")
            ?.let { array ->
                (0 until array.length())
                    .mapNotNull { idx -> array.optString(idx).takeIf { it.isNotBlank() } }
            }
            .orEmpty()
        reasons.take(2).joinToString(" • ")
    }.getOrDefault("")
}
