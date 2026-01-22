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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.local.entity.ProviderSnapshotEntity
import org.json.JSONObject

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
    var expandedReasonsId by remember { mutableStateOf<String?>(null) }

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
            val missingDetails = (inboxItem?.extractedTitle ?: inboxItem?.rawTitle).isNullOrBlank() ||
                (inboxItem?.extractedArtist ?: inboxItem?.rawArtist).isNullOrBlank()
            if (missingDetails) {
                TextButton(onClick = { showAddDetails = true }) {
                    Text("Add missing details")
                }
            }

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
                    val imageUrl = remember(candidate.rawJson) {
                        extractCandidateImageUrl(candidate.rawJson)
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = candidate.id == selectedCandidateId,
                                onClick = { onSelectCandidate(candidate.id) },
                            )
                            if (imageUrl != null) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Cover for ${candidate.title}",
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .height(56.dp)
                                        .width(56.dp),
                                )
                            }
                            Column(modifier = Modifier.padding(top = 6.dp)) {
                                Text(candidate.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${candidate.artist} • ${candidate.label ?: ""} ${candidate.catalogNo ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                candidate.reasonsJson?.let { reasons ->
                                    val reasonList = parseReasons(reasons)
                                    if (reasonList.isNotEmpty()) {
                                        val expanded = expandedReasonsId == candidate.id
                                        val display = if (expanded) reasonList else reasonList.take(3)
                                        Text(
                                            text = "Matched because ${display.joinToString(" • ") { reasonLabel(it) }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        TextButton(
                                            onClick = {
                                                expandedReasonsId = if (expanded) null else candidate.id
                                            },
                                        ) {
                                            Text(if (expanded) "Hide reasons" else "Show reasons")
                                        }
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

private fun parseReasons(rawJson: String): List<String> {
    return runCatching {
        if (rawJson.trim().startsWith("[")) {
            val array = org.json.JSONArray(rawJson)
            (0 until array.length())
                .mapNotNull { idx -> array.optString(idx).takeIf { it.isNotBlank() } }
        } else {
            org.json.JSONObject(rawJson)
                .optJSONArray("reasons")
                ?.let { array ->
                    (0 until array.length())
                        .mapNotNull { idx -> array.optString(idx).takeIf { it.isNotBlank() } }
                }
                .orEmpty()
        }
    }.getOrDefault(emptyList())
}

private fun reasonLabel(code: String): String {
    return when (code) {
        "LOW_SIGNAL" -> "Low signal"
        "MULTIPLE_CANDIDATES" -> "Multiple candidates"
        "MISSING_TITLE" -> "Missing title"
        "MISSING_ARTIST" -> "Missing artist"
        "WEAK_MATCH_TITLE" -> "Weak title match"
        "WEAK_MATCH_ARTIST" -> "Weak artist match"
        "NO_API_MATCH" -> "No API match"
        "BARCODE_VALID_CHECKSUM" -> "Valid checksum"
        "BARCODE_NORMALIZED" -> "Normalized barcode"
        "FORMAT_MATCH_VINYL" -> "Vinyl format match"
        "CATNO_MATCH" -> "Catalog no match"
        "LABEL_MATCH" -> "Label match"
        "TITLE_MATCH" -> "Title match"
        "ARTIST_MATCH" -> "Artist match"
        "RUNNER_UP_GAP_STRONG" -> "Clear top result"
        else -> code.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }
}

private fun extractCandidateImageUrl(rawJson: String): String? {
    return runCatching {
        val json = JSONObject(rawJson)
        val cover = json.optString("cover_image").trim()
        if (cover.isNotBlank()) return cover
        val thumb = json.optString("thumb").trim()
        thumb.takeIf { it.isNotBlank() }
    }.getOrNull()
}
