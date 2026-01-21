@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.feature.inbox.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.feature.inbox.vm.InboxFilter

@Composable
fun InboxScreen(
    items: List<InboxItemEntity>,
    selectedFilter: InboxFilter,
    onFilterChange: (InboxFilter) -> Unit,
    onResolveItem: (InboxItemEntity) -> Unit,
    onRetryFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbox") },
                actions = {
                    TextButton(onClick = onRetryFailed) { Text("Retry failed") }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                InboxFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filterLabel(filter)) },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    InboxRow(item = item, onClick = { onResolveItem(item) })
                }
            }
        }
    }
}

@Composable
private fun InboxRow(
    item: InboxItemEntity,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = item.extractedTitle ?: item.rawTitle ?: "Untitled",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = item.extractedArtist ?: item.rawArtist ?: "Unknown artist",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${item.sourceType} • ${item.lookupStatus}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun filterLabel(filter: InboxFilter): String {
    return when (filter) {
        InboxFilter.ALL -> "All"
        InboxFilter.NEEDS_PICK -> "Needs pick"
        InboxFilter.FAILED -> "Failed"
        InboxFilter.DRAFTS -> "Drafts"
    }
}
