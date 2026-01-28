package com.zak.pressmark.feature.library.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zak.pressmark.data.prefs.LibraryGroupKey
import com.zak.pressmark.data.prefs.LibrarySortKey
import com.zak.pressmark.data.prefs.LibrarySortSpec
import com.zak.pressmark.data.prefs.SortDirection
import com.zak.pressmark.feature.library.ui.LibraryActionRow
import com.zak.pressmark.feature.library.vm.LibraryItemUi
import com.zak.pressmark.feature.library.vm.LibraryListItem
import com.zak.pressmark.feature.library.vm.LibraryUiState

/**
 * Content layer for the Library screen.
 *
 * Responsibilities:
 * - Renders the controls row + main list/empty state.
 * - Owns list shape (headers/rows) and search filtering behavior.
 *
 * Non-responsibilities:
 * - IME / navigation bar insets.
 * - Floating overlays (search bar, dialogs, sheets).
 */
@Composable
fun LibraryContent(
    state: LibraryUiState,
    searchQuery: String,
    onOpenWork: (String) -> Unit,
    onSortChanged: (LibrarySortSpec) -> Unit,
    onGroupChanged: (LibraryGroupKey) -> Unit,
    onToggleGroup: (groupId: String, isExpanded: Boolean) -> Unit,
    onRequestDelete: (LibraryItemUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredItems = remember(searchQuery, state.items) {
        filterLibraryItemsForSearch(state.items, searchQuery)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LibraryLayoutTokens.HorizontalGutter),
        verticalArrangement = Arrangement.spacedBy(LibraryLayoutTokens.ContentVerticalSpacing),
    ) {
        LibraryControlsRow(
            sortSpec = state.sortSpec,
            groupKey = state.groupKey,
            onSortChanged = onSortChanged,
            onGroupChanged = onGroupChanged,
        )

        if (filteredItems.isEmpty()) {
            EmptyState(isSearching = searchQuery.isNotBlank())
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(LibraryLayoutTokens.ListItemSpacing),
            ) {
                items(
                    items = filteredItems,
                    key = {
                        when (it) {
                            is LibraryListItem.Header -> it.id
                            is LibraryListItem.Row -> it.id
                        }
                    },
                ) { listItem ->
                    when (listItem) {
                        is LibraryListItem.Header -> LibraryGroupHeader(
                            title = listItem.title,
                            count = listItem.count,
                            isExpanded = listItem.isExpanded,
                            level = listItem.level,
                            modifier = Modifier.fillMaxWidth(),
                            onToggle = {
                                if (searchQuery.isBlank()) {
                                    onToggleGroup(listItem.id, listItem.isExpanded)
                                }
                            },
                        )

                        is LibraryListItem.Row -> WorkRow(
                            item = listItem.item,
                            level = listItem.level,
                            onClick = { onOpenWork(listItem.item.workId) },
                            onDelete = { onRequestDelete(listItem.item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryControlsRow(
    sortSpec: LibrarySortSpec,
    groupKey: LibraryGroupKey,
    onSortChanged: (LibrarySortSpec) -> Unit,
    onGroupChanged: (LibraryGroupKey) -> Unit,
) {
    LibraryActionRow(
        sort = sortSpec,
        group = groupKey,
        sortOptions = sortOptions(),
        groupOptions = LibraryGroupKey.entries,
        sortLabel = ::sortLabel,
        groupLabel = ::groupLabel,
        onSortSelected = onSortChanged,
        onGroupSelected = onGroupChanged,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmptyState(isSearching: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (isSearching) "No matches" else "No works yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (isSearching) "Try a different title or artist." else "Add an album to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun sortOptions(): List<LibrarySortSpec> = listOf(
    LibrarySortSpec(LibrarySortKey.TITLE, SortDirection.ASC),
    LibrarySortSpec(LibrarySortKey.TITLE, SortDirection.DESC),
    LibrarySortSpec(LibrarySortKey.ARTIST, SortDirection.ASC),
    LibrarySortSpec(LibrarySortKey.ARTIST, SortDirection.DESC),
    LibrarySortSpec(LibrarySortKey.RECENTLY_ADDED, SortDirection.DESC),
    LibrarySortSpec(LibrarySortKey.YEAR, SortDirection.ASC),
    LibrarySortSpec(LibrarySortKey.YEAR, SortDirection.DESC),
)

private fun sortLabel(spec: LibrarySortSpec): String = when (spec.key) {
    LibrarySortKey.TITLE -> "Title ${if (spec.direction == SortDirection.ASC) "A–Z" else "Z–A"}"
    LibrarySortKey.ARTIST -> "Artist ${if (spec.direction == SortDirection.ASC) "A–Z" else "Z–A"}"
    LibrarySortKey.RECENTLY_ADDED -> "Recently added"
    LibrarySortKey.YEAR -> "Year ${if (spec.direction == SortDirection.ASC) "Asc" else "Desc"}"
}

private fun groupLabel(key: LibraryGroupKey): String = when (key) {
    LibraryGroupKey.NONE -> "No grouping"
    LibraryGroupKey.ARTIST -> "Artist"
    LibraryGroupKey.GENRE -> "Genre"
    LibraryGroupKey.STYLE -> "Style"
    LibraryGroupKey.DECADE -> "Decade"
    LibraryGroupKey.YEAR -> "Year"
}

private fun filterLibraryItemsForSearch(
    items: List<LibraryListItem>,
    query: String,
): List<LibraryListItem> {
    val q = query.trim()
    if (q.isBlank()) return items

    // Search mode: show only matching rows (ignore headers).
    return items
        .filterIsInstance<LibraryListItem.Row>()
        .filter { row ->
            row.item.title.contains(q, ignoreCase = true) ||
                    row.item.artistLine.contains(q, ignoreCase = true)
        }
}

@Composable
private fun LibraryGroupHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    level: Int,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    val indent = LibraryLayoutTokens.NestIndentStep * level
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        label = "arrowRotation",
    )

    Surface(
        modifier = modifier
            .padding(start = indent)
            .animateContentSize()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle),
        tonalElevation = if (level == 0) 2.dp else 1.dp,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = title,
                    style = if (level == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            CountPill(count = count)
        }
    }
}

@Composable
private fun CountPill(count: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = count.toString(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WorkRow(
    item: LibraryItemUi,
    level: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val indent = LibraryLayoutTokens.NestIndentStep * level

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!item.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = item.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.artistLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
}
