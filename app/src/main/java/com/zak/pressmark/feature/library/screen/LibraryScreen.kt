package com.zak.pressmark.feature.library.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zak.pressmark.data.prefs.LibraryGroupKey
import com.zak.pressmark.data.prefs.LibrarySortSpec
import com.zak.pressmark.feature.library.vm.LibraryItemUi
import com.zak.pressmark.feature.library.vm.LibraryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onOpenWork: (String) -> Unit,
    onAddManual: () -> Unit,
    onAddBarcode: () -> Unit,
    onSortChanged: (LibrarySortSpec) -> Unit,
    onGroupChanged: (LibraryGroupKey) -> Unit,
    onToggleGroup: (groupId: String, isExpanded: Boolean) -> Unit,
    deleteTarget: LibraryItemUi?,
    onRequestDelete: (LibraryItemUi) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (LibraryItemUi) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        // Keep bottom insets OUT of the content; overlays own IME/nav spacing.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(onClick = onAddBarcode) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                    IconButton(onClick = onAddManual) {
                        Icon(Icons.Default.Add, contentDescription = "Add manually")
                    }
                },
            )
        },
    ) { innerPadding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LibraryContent(
                state = state,
                searchQuery = searchQuery,
                onOpenWork = onOpenWork,
                onSortChanged = onSortChanged,
                onGroupChanged = onGroupChanged,
                onToggleGroup = onToggleGroup,
                onRequestDelete = onRequestDelete,
                modifier = Modifier.fillMaxSize(),
            )

            LibraryOverlays(
                modifier = Modifier.fillMaxSize(),
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" },
                expanded = isSearchExpanded,
                onExpandedChange = { isSearchExpanded = it },
                scaffoldBottomPadding = navBarBottom,
                deleteTarget = deleteTarget,
                onDismissDelete = onDismissDelete,
                onConfirmDelete = onConfirmDelete,
                expandedKeyboardGap = LibraryLayoutTokens.SearchExpandedKeyboardGap,
            )
        }
    }
}
