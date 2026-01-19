package com.zak.pressmark.feature.catalog.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.local.model.ReleaseListItem
import com.zak.pressmark.feature.catalog.components.CatalogActionRail
import com.zak.pressmark.feature.catalog.components.CatalogTopActionBar
import com.zak.pressmark.feature.catalog.components.RailMode
import com.zak.pressmark.feature.catalog.components.TopAppBar
import com.zak.pressmark.feature.catalog.vm.CatalogSort


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    releases: List<ReleaseListItem>,
    query: String,
    onQueryChange: (String) -> Unit,
    sort: CatalogSort,
    onSortChange: (CatalogSort) -> Unit,
    snackMessage: String?,
    onSnackShown: () -> Unit,
    onAddAlbum: () -> Unit,
    onOpenRelease: (releaseId: String) -> Unit,
    onDelete: (ReleaseListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenContainerColor = MaterialTheme.colorScheme.background
    val topBarContainerColor = MaterialTheme.colorScheme.surface

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        val msg = snackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg)
        onSnackShown()
    }



    var railMode by rememberSaveable { mutableStateOf(RailMode.Idle) }
    var isSortExpanded by rememberSaveable { mutableStateOf(false) }
    var filterExpanded by rememberSaveable { mutableStateOf(false) }
    var groupExpanded by rememberSaveable { mutableStateOf(false) }

    var selectedSort by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }

    val showClearSelections = selectedSort != null || selectedFilter != null || selectedGroup != null

// Scroll state to collapse menus on scroll
    val listState = rememberLazyListState()

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isSortExpanded = false
            filterExpanded = false
            groupExpanded = false
        }
    }


    Surface(
        modifier = modifier,
        color = screenContainerColor,
    ) {
        Scaffold(
            containerColor = screenContainerColor,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = "Catalog",
                    containerColor = topBarContainerColor,
                    actions = {
                        TextButton(onClick = onAddAlbum) {
                            Text("Add Album")
                        }
                    },
                )
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {

                // Content gets scaffold padding (INCLUDING top). Overlay rail does not.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = padding.calculateStartPadding(LocalLayoutDirection.current),
                            end = padding.calculateEndPadding(LocalLayoutDirection.current),
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding(),
                        )
                ) {
                    // Divider under TopAppBar
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    CatalogTopActionBar(
                        sortExpanded = isSortExpanded,
                        filterExpanded = filterExpanded,
                        groupExpanded = groupExpanded,

                        selectedSort = selectedSort,
                        selectedFilter = selectedFilter,
                        selectedGroup = selectedGroup,

                        onSortToggle = {
                            isSortExpanded = !isSortExpanded
                            filterExpanded = false
                            groupExpanded = false
                        },
                        onFilterToggle = {
                            filterExpanded = !filterExpanded
                            isSortExpanded = false
                            groupExpanded = false
                        },
                        onGroupToggle = {
                            groupExpanded = !groupExpanded
                            isSortExpanded = false
                            filterExpanded = false
                        },

                        sortOptions = listOf("Added newest", "Title A–Z", "Artist A–Z", "Year newest"),
                        filterOptions = listOf("Has barcode", "No barcode"),
                        groupOptions = listOf("Artist", "Year"),

                        onSortSelect = {
                            selectedSort = it
                            isSortExpanded = false
                            // TODO: onSortChange(...)
                        },
                        onFilterSelect = {
                            selectedFilter = it
                            filterExpanded = false
                        },
                        onGroupSelect = {
                            selectedGroup = it
                            groupExpanded = false
                        },

                        showClear = showClearSelections,
                        onClearSelections = {
                            selectedSort = null
                            selectedFilter = null
                            selectedGroup = null
                            isSortExpanded = false
                            filterExpanded = false
                            groupExpanded = false
                        },
                    )


                    // Divider between chips bar and list
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Only bottom padding needed now (rail). The top-gap bug was the 56.dp.
                        contentPadding = PaddingValues(bottom = 120.dp),
                    ) {
                        items(
                            items = releases,
                            key = { it.release.id },
                        ) { item ->
                            ReleaseRow(
                                item = item,
                                onClick = { onOpenRelease(item.release.id) },
                                onDelete = { onDelete(item) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }

                // Bottom overlay rail
                CatalogActionRail(
                    mode = railMode,
                    onModeChange = { railMode = it },
                    query = query,
                    onQueryChange = onQueryChange,
                    addIcon = Icons.Outlined.Add,
                    searchIcon = Icons.Outlined.Search,
                    clearIcon = Icons.Outlined.Close,
                    bulkIcon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    scanIcon = Icons.Outlined.QrCodeScanner,
                    addAlbumIcon = Icons.Outlined.LibraryAdd,
                    onBulkAdd = { railMode = RailMode.Idle /* TODO */ },
                    onScanBarcode = { railMode = RailMode.Idle /* TODO */ },
                    onAddAlbum = { railMode = RailMode.Idle; onAddAlbum() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ReleaseRow(
    item: ReleaseListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.release.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

            val artistLine = item.artistLine.takeIf { it.isNotBlank() } ?: "—"
            Text(
                text = artistLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            val yearText = item.release.releaseYear?.toString()
            if (!yearText.isNullOrBlank()) {
                Text(
                    text = yearText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        TextButton(onClick = onDelete) {
            Text("Delete")
        }
    }
}
