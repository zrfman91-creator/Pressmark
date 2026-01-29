package com.zak.pressmark.feature.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.prefs.LibraryGroupKey
import com.zak.pressmark.data.prefs.LibrarySortSpec

/**
 * Two-control action row for the Library screen (Sort + Group).
 *
 * - Keeps horizontal padding OUT of this component. Parent decides gutter.
 * - Long-press Group pill => "Expand all / Collapse all" menu (when enabled).
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
    onToggleAllSections: ((expand: Boolean) -> Unit)?,
    sectionsMenuEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var sortMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var groupMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var sectionsMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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

        Box(modifier = Modifier.weight(1f)) {
            PressmarkPillButton(
                label = groupLabel(group),
                icon = Icons.Default.FilterList,
                onClick = { groupMenuExpanded = true },
                onLongClick = if (sectionsMenuEnabled && onToggleAllSections != null) {
                    { sectionsMenuExpanded = true }
                } else null,
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

            DropdownMenu(
                expanded = sectionsMenuExpanded,
                onDismissRequest = { sectionsMenuExpanded = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.UnfoldMore, contentDescription = null) },
                    text = { Text("Expand all") },
                    onClick = {
                        sectionsMenuExpanded = false
                        onToggleAllSections?.invoke(true)
                    },
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.UnfoldLess, contentDescription = null) },
                    text = { Text("Collapse all") },
                    onClick = {
                        sectionsMenuExpanded = false
                        onToggleAllSections?.invoke(false)
                    },
                )
            }
        }
    }
}
