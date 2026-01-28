package com.zak.pressmark.feature.library.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.zak.pressmark.feature.library.ui.LibrarySearchBar
import com.zak.pressmark.feature.library.vm.LibraryItemUi

@Composable
fun LibraryOverlays(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    scaffoldBottomPadding: Dp,
    deleteTarget: LibraryItemUi?,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (LibraryItemUi) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search library…",
    expandedKeyboardGap: Dp = LibraryLayoutTokens.SearchExpandedKeyboardGap,
) {
    LibrarySearchBar(
        modifier = modifier,
        query = query,
        onQueryChange = onQueryChange,
        onClear = onClear,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        placeholder = placeholder,
        scaffoldBottomPadding = scaffoldBottomPadding,
        expandedKeyboardGap = expandedKeyboardGap,
    )

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Remove from library?") },
            text = { Text("This will remove the work and any related entries.") },
            confirmButton = {
                Button(onClick = { onConfirmDelete(target) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("Cancel") }
            },
        )
    }
}
