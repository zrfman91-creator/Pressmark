package com.zak.pressmark.feature.library.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zak.pressmark.feature.library.vm.LibraryItemUi
import com.zak.pressmark.feature.library.vm.LibraryListItem
import com.zak.pressmark.feature.library.vm.LibraryUiState

/**
 * Dumb UI screen. No ViewModels, no Flows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onOpenWork: (String) -> Unit,
    onAddManual: () -> Unit,
    onAddBarcode: () -> Unit,
    deleteTarget: LibraryItemUi?,
    onRequestDelete: (LibraryItemUi) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (LibraryItemUi) -> Unit,
) {
    var railMode by remember { mutableStateOf(RailMode.Idle) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(searchQuery, state.items) {
        filterLibraryItemsForSearch(
            items = state.items,
            query = searchQuery,
        )
    }
    val isSearching = searchQuery.isNotBlank()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Library") }) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (filteredItems.isEmpty()) {
                    Text(
                        text = if (isSearching) "No works match your search." else "No works yet. Add one to get started.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            items = filteredItems,
                            key = {
                                when (it) {
                                    is LibraryListItem.Header -> it.id
                                    is LibraryListItem.Row -> it.id
                                }
                            },
                            contentType = {
                                when (it) {
                                    is LibraryListItem.Header -> "header"
                                    is LibraryListItem.Row -> "row"
                                }
                            },
                        ) { listItem ->
                            when (listItem) {
                                is LibraryListItem.Header -> LibraryGroupHeader(
                                    title = listItem.title,
                                    count = listItem.count,
                                    isExpanded = listItem.isExpanded,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                is LibraryListItem.Row -> WorkRow(
                                    item = listItem.item,
                                    onClick = { onOpenWork(listItem.item.workId) },
                                    onDelete = { onRequestDelete(listItem.item) },
                                )
                            }
                        }

                        // Prevent last item from being hidden behind the action rail.
                        item { Spacer(modifier = Modifier.height(96.dp)) }
                    }
                }
            }
        }

        // --- The Action Rail ---
        LibraryActionRail(
            mode = railMode,
            onModeChange = { newMode ->
                railMode = newMode
                if (newMode == RailMode.Idle) {
                    searchQuery = ""
                }
            },
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            addIcon = Icons.Default.Add,
            searchIcon = Icons.Default.Search,
            clearIcon = Icons.Default.Clear,
            bulkIcon = Icons.Outlined.LibraryAdd,
            scanIcon = Icons.Outlined.PhotoCamera,
            addAlbumIcon = Icons.Default.Add,
            onAddAlbum = onAddManual,
            onScanBarcode = onAddBarcode,
            onBulkAdd = { /* TODO: Implement if needed */ },
            modifier = Modifier.fillMaxSize(),
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Remove from library?") },
            text = { Text("This will remove the work and any related entries.") },
            confirmButton = {
                Button(onClick = { onConfirmDelete(target) }) { Text("Delete") }
            },
            dismissButton = {
                Button(onClick = onDismissDelete) { Text("Cancel") }
            },
        )
    }
}

private fun filterLibraryItemsForSearch(
    items: List<LibraryListItem>,
    query: String,
): List<LibraryListItem> {
    val q = query.trim()
    if (q.isBlank()) return items

    val result = mutableListOf<LibraryListItem>()

    var pendingHeader: LibraryListItem.Header? = null
    var pendingRows = mutableListOf<LibraryListItem.Row>()

    fun flushGroup() {
        val header = pendingHeader
        if (header != null) {
            if (pendingRows.isNotEmpty()) {
                result.add(
                    header.copy(
                        // Search results: show only matching rows count and keep expanded.
                        count = pendingRows.size,
                        isExpanded = true,
                    )
                )
                result.addAll(pendingRows)
            }
            pendingHeader = null
            pendingRows = mutableListOf()
        }
    }

    items.forEach { listItem ->
        when (listItem) {
            is LibraryListItem.Header -> {
                flushGroup()
                pendingHeader = listItem
            }

            is LibraryListItem.Row -> {
                val item = listItem.item
                val matches =
                    item.title.contains(q, ignoreCase = true) ||
                            item.artistLine.contains(q, ignoreCase = true)

                if (!matches) return@forEach

                // If the list isn't grouped (no headers), emit rows directly.
                if (pendingHeader == null) {
                    result.add(listItem)
                } else {
                    pendingRows.add(listItem)
                }
            }
        }
    }

    flushGroup()
    return result
}

@Composable
private fun LibraryGroupHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
            Text(title)
        }
        Text("$count")
    }
}

@Composable
private fun WorkRow(
    item: LibraryItemUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!item.artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = item.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
            )
            Text(
                text = item.artistLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}
