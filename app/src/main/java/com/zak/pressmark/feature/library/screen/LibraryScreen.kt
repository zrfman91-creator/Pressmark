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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onToggleGroup: (groupId: String) -> Unit,
    onToggleAllSections: (expand: Boolean) -> Unit,
    onSearchResultsUpdated: (query: String, resultsCount: Int) -> Unit,
    onDismissOnboarding: (source: String) -> Unit,
    deleteTarget: LibraryItemUi?,
    onRequestDelete: (LibraryItemUi) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (LibraryItemUi) -> Unit,
    addedWorkId: String?,
    onConsumeAddedWorkId: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(addedWorkId) {
        if (!addedWorkId.isNullOrBlank()) {
            val result = snackbarHostState.showSnackbar(
                message = "Added to Library",
                actionLabel = "View",
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                onOpenWork(addedWorkId)
            }
            onConsumeAddedWorkId()
        }
    }

    Scaffold(
        // Keep bottom insets OUT of content; we apply nav-bar padding precisely to the list.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text( text = "Library", style = MaterialTheme.typography.displayLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()


        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 12.dp),
        ) {
            // Search is now provided by the app shell (NavigationSuiteScaffold).
            // LibraryContent still supports filtering by searchQuery; for now it is empty.
            LibraryContent(
                state = state,
                searchQuery = "",
                bottomContentPadding = navBarBottom,
                onAddManual = onAddManual,
                onAddBarcode = onAddBarcode,
                onOpenWork = onOpenWork,
                onSortChanged = onSortChanged,
                onGroupChanged = onGroupChanged,
                onToggleGroup = onToggleGroup,
                onToggleAllSections = onToggleAllSections,
                onRequestDelete = onRequestDelete,
                onSearchResultsUpdated = onSearchResultsUpdated,
                modifier = Modifier.fillMaxSize(),
            )

            LibraryOverlays(
                deleteTarget = deleteTarget,
                onDismissDelete = onDismissDelete,
                onConfirmDelete = onConfirmDelete,
            )

            if (state.showOnboarding) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { onDismissOnboarding("dismiss") },
                    title = {
                        Text(
                            text = "Welcome to Pressmark",
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    text = {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        ) {
                            Text("• Scan barcode to add fast")
                            Text("• Manual add works anytime")
                            Text("• Sort + Group keep it tidy")
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                onDismissOnboarding("scan")
                                onAddBarcode()
                            },
                        ) { Text("Scan my first record") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { onDismissOnboarding("dismiss") },
                        ) { Text("Not now") }
                    },
                )
            }
        }
    }
}
