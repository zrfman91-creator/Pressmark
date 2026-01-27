package com.zak.pressmark.feature.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.prefs.LibraryGroupKey
import com.zak.pressmark.data.prefs.LibrarySortKey
import com.zak.pressmark.data.prefs.LibrarySortSpec
import com.zak.pressmark.data.prefs.SortDirection

/**
 * Two-control action row for the Library screen.
 *
 * NOTE: Expand/Collapse All is intentionally NOT included yet (per design decision pending).
 * Keep that logic in the screen (e.g., a separate Sections button) until you finalize the UX.
 */
@Composable
fun LibraryActionRow(
    sort: LibrarySortSpec,
    group: LibraryGroupKey,
    sortOptions: List<LibrarySortSpec>,
    groupOptions: List<LibraryGroupKey>,
    sortLabel: (LibrarySortSpec) -> String,
    groupLabel: (LibraryGroupKey) -> String,
    onSortSelected: (LibrarySortSpec) -> Unit,
    onGroupSelected: (LibraryGroupKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var groupMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Sort (button + anchored menu)
        Box(modifier = Modifier.weight(1f)) {
            PressmarkPillButton(
                label = sortLabel(sort),
                icon = Icons.AutoMirrored.Filled.Sort,
                onClick = { sortMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
            )

            PressmarkDropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
                items = sortOptions,
                itemText = { sortLabel(it) },
                isSelected = { it == sort },
                onItemSelected = {
                    sortMenuExpanded = false
                    onSortSelected(it)
                },
            )
        }

        // Group (button + anchored menu)
        Box(modifier = Modifier.weight(1f)) {
            PressmarkPillButton(
                label = groupLabel(group),
                icon = Icons.Default.FilterList,
                onClick = { groupMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
            )

            PressmarkDropdownMenu(
                expanded = groupMenuExpanded,
                onDismissRequest = { groupMenuExpanded = false },
                items = groupOptions,
                itemText = { groupLabel(it) },
                isSelected = { it == group },
                onItemSelected = {
                    groupMenuExpanded = false
                    onGroupSelected(it)
                },
            )
        }
    }
}

private val previewSortOptions = listOf(
    LibrarySortSpec(LibrarySortKey.TITLE, SortDirection.ASC),
    LibrarySortSpec(LibrarySortKey.TITLE, SortDirection.DESC),
    LibrarySortSpec(LibrarySortKey.ARTIST, SortDirection.ASC),
    LibrarySortSpec(LibrarySortKey.ARTIST, SortDirection.DESC),
    LibrarySortSpec(LibrarySortKey.RECENTLY_ADDED, SortDirection.DESC),
    LibrarySortSpec(LibrarySortKey.YEAR, SortDirection.ASC),
    LibrarySortSpec(LibrarySortKey.YEAR, SortDirection.DESC),
)

private fun previewSortLabel(spec: LibrarySortSpec): String = when (spec.key) {
    LibrarySortKey.TITLE -> "Title ${if (spec.direction == SortDirection.ASC) "A–Z" else "Z–A"}"
    LibrarySortKey.ARTIST -> "Artist ${if (spec.direction == SortDirection.ASC) "A–Z" else "Z–A"}"
    LibrarySortKey.RECENTLY_ADDED -> "Recently added"
    LibrarySortKey.YEAR -> "Year ${if (spec.direction == SortDirection.ASC) "Asc" else "Desc"}"
}

private fun previewGroupLabel(key: LibraryGroupKey): String = when (key) {
    LibraryGroupKey.NONE -> "No grouping"
    LibraryGroupKey.ARTIST -> "Artist"
    LibraryGroupKey.GENRE -> "Genre"
    LibraryGroupKey.STYLE -> "Style"
    LibraryGroupKey.DECADE -> "Decade"
    LibraryGroupKey.YEAR -> "Year"
}

@Preview(name = "LibraryActionRow", showBackground = true)
@Composable
private fun PreviewLibraryActionRow() {
    Surface(color = MaterialTheme.colorScheme.background) {
        LibraryActionRow(
            sort = previewSortOptions.first(),
            group = LibraryGroupKey.entries.first(),
            sortOptions = previewSortOptions,
            groupOptions = LibraryGroupKey.entries,
            sortLabel = ::previewSortLabel,
            groupLabel = ::previewGroupLabel,
            onSortSelected = {},
            onGroupSelected = {},
        )
    }
}
