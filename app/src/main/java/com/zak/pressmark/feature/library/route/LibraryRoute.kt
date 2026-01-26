// FILE: app/src/main/java/com/zak/pressmark/feature/library/route/LibraryRoute.kt
package com.zak.pressmark.feature.library.route

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import coil3.compose.AsyncImage
import com.zak.pressmark.data.prefs.LibraryGroupKey
import com.zak.pressmark.data.prefs.LibrarySortKey
import com.zak.pressmark.data.prefs.LibrarySortSpec
import com.zak.pressmark.data.prefs.SortDirection
import com.zak.pressmark.feature.library.vm.LibraryItemUi
import com.zak.pressmark.feature.library.vm.LibraryListItem
import com.zak.pressmark.feature.library.vm.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryRoute(
    vm: LibraryViewModel,
    onOpenWork: (String) -> Unit,
    onAddManual: () -> Unit,
    onAddBarcode: () -> Unit,
) {
    val state by vm.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<LibraryItemUi?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onAddManual,
                    modifier = Modifier.weight(1f),
                ) { Text("Add manually") }

                Button(
                    onClick = onAddBarcode,
                    modifier = Modifier.weight(1f),
                ) { Text("Add barcode") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LibraryControls(
                sortSpec = state.sortSpec,
                groupKey = state.groupKey,
                onSortChanged = vm::updateSort,
                onGroupChanged = vm::updateGroup,
                onExpandAll = { groupIds ->
                    vm.expandAll(groupIds)
                },
                onCollapseAll = { groupIds ->
                    vm.collapseAll(groupIds)
                },
                listItems = state.listItems,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.listItems.isEmpty()) {
                Text("No works yet. Add one to get started.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = state.listItems,
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
                                onToggle = { vm.toggleGroupExpanded(listItem.id, listItem.isExpanded) },
                            )
                            is LibraryListItem.Row -> WorkRow(
                                item = listItem.item,
                                onClick = { onOpenWork(listItem.item.workId) },
                                onDelete = { deleteTarget = listItem.item },
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove from library?") },
            text = { Text("This will remove the work and any related entries.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteWork(target.workId)
                        deleteTarget = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun LibraryControls(
    sortSpec: LibrarySortSpec,
    groupKey: LibraryGroupKey,
    onSortChanged: (LibrarySortSpec) -> Unit,
    onGroupChanged: (LibraryGroupKey) -> Unit,
    onExpandAll: (List<String>) -> Unit,
    onCollapseAll: (List<String>) -> Unit,
    listItems: List<LibraryListItem>,
) {
    val groupIds = remember(listItems) {
        listItems.filterIsInstance<LibraryListItem.Header>().map { it.id }
    }
    val hasCollapsed = remember(listItems) {
        listItems.filterIsInstance<LibraryListItem.Header>().any { !it.isExpanded }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SortMenu(
                current = sortSpec,
                onChanged = onSortChanged,
                modifier = Modifier.weight(1f),
            )
            GroupMenu(
                current = groupKey,
                onChanged = onGroupChanged,
                modifier = Modifier.weight(1f),
            )
        }
        if (groupKey != LibraryGroupKey.NONE && groupIds.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        if (hasCollapsed) {
                            onExpandAll(groupIds)
                        } else {
                            onCollapseAll(groupIds)
                        }
                    },
                ) {
                    Text(if (hasCollapsed) "Expand all" else "Collapse all")
                }
            }
        }
    }
}

@Composable
private fun SortMenu(
    current: LibrarySortSpec,
    onChanged: (LibrarySortSpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(sortLabel(current))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            sortOptions().forEach { option ->
                DropdownMenuItem(
                    text = { Text(sortLabel(option)) },
                    onClick = {
                        expanded = false
                        onChanged(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun GroupMenu(
    current: LibraryGroupKey,
    onChanged: (LibraryGroupKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(groupLabel(current))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LibraryGroupKey.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(groupLabel(option)) },
                    onClick = {
                        expanded = false
                        onChanged(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun LibraryGroupHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
            Text(title)
        }
        Text("$count")
    }
}

private fun sortOptions(): List<LibrarySortSpec> {
    return listOf(
        LibrarySortSpec(LibrarySortKey.TITLE, SortDirection.ASC),
        LibrarySortSpec(LibrarySortKey.TITLE, SortDirection.DESC),
        LibrarySortSpec(LibrarySortKey.ARTIST, SortDirection.ASC),
        LibrarySortSpec(LibrarySortKey.ARTIST, SortDirection.DESC),
        LibrarySortSpec(LibrarySortKey.RECENTLY_ADDED, SortDirection.DESC),
        LibrarySortSpec(LibrarySortKey.YEAR, SortDirection.ASC),
        LibrarySortSpec(LibrarySortKey.YEAR, SortDirection.DESC),
    )
}

private fun sortLabel(spec: LibrarySortSpec): String {
    return when (spec.key) {
        LibrarySortKey.TITLE -> "Title ${textDirectionLabel(spec.direction)}"
        LibrarySortKey.ARTIST -> "Artist ${textDirectionLabel(spec.direction)}"
        LibrarySortKey.RECENTLY_ADDED -> "Recently added"
        LibrarySortKey.YEAR -> "Year ${yearDirectionLabel(spec.direction)}"
    }
}

private fun textDirectionLabel(direction: SortDirection): String =
    if (direction == SortDirection.ASC) "A–Z" else "Z–A"

private fun yearDirectionLabel(direction: SortDirection): String =
    if (direction == SortDirection.ASC) "Asc" else "Desc"

private fun groupLabel(key: LibraryGroupKey): String {
    return when (key) {
        LibraryGroupKey.NONE -> "No grouping"
        LibraryGroupKey.ARTIST -> "Artist"
        LibraryGroupKey.GENRE -> "Genre"
        LibraryGroupKey.STYLE -> "Style"
        LibraryGroupKey.DECADE -> "Decade"
        LibraryGroupKey.YEAR -> "Year"
    }
}

@Composable
private fun WorkRow(
    item: LibraryItemUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(item.title)
                Text(item.artistLine)
                item.year?.let { Text(it.toString()) }
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
}
