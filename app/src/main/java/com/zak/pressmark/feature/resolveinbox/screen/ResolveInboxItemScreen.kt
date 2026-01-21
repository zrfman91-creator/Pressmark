package com.zak.pressmark.feature.resolveinbox.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.local.entity.ProviderSnapshotEntity

@Composable
fun ResolveInboxItemScreen(
    inboxItem: InboxItemEntity?,
    candidates: List<ProviderSnapshotEntity>,
    onCommit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resolve Inbox Item") },
                navigationIcon = { androidx.compose.material3.TextButton(onClick = onBack) { Text("Back") } },
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
                        Text(candidate.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${candidate.artist} • ${candidate.label ?: ""} ${candidate.catalogNo ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Confidence ${candidate.confidence}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = onCommit) {
                Text("Confirm and commit")
            }
        }
    }
}
