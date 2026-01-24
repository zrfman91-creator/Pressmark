package com.zak.pressmark.feature.catalog.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.ui.elements.AlbumArtwork
import com.zak.pressmark.data.model.CatalogItemSummary
import com.zak.pressmark.data.repository.CatalogDensity
import com.zak.pressmark.data.repository.CatalogViewMode
import com.zak.pressmark.feature.catalog.components.CatalogActionRail
import com.zak.pressmark.feature.catalog.components.CatalogTopActionBar
import com.zak.pressmark.feature.catalog.components.RailMode
import com.zak.pressmark.feature.catalog.components.TopAppBar
import com.zak.pressmark.feature.catalog.vm.CatalogSort


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    releases: List<CatalogItemSummary>,
    query: String,
    onQueryChange: (String) -> Unit,
    sort: CatalogSort,
    onSortChange: (CatalogSort) -> Unit,
    snackMessage: String?,
    onSnackShown: () -> Unit,
    onAddAlbum: () -> Unit,
    onOpenScanConveyor: () -> Unit,
    onOpenRelease: (catalogItemId: String) -> Unit,
    onDelete: (CatalogItemSummary) -> Unit,
    viewMode: CatalogViewMode,
    onViewModeChange: (CatalogViewMode) -> Unit,
    density: CatalogDensity,
    onDensityChange: (CatalogDensity) -> Unit,
    showDevSettings: Boolean,
    onOpenDevSettings: () -> Unit,
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

    var selectedFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }

    // Sort selection is VM-owned; show label only when non-default.
    val selectedSortLabel: String? = when (sort) {
        CatalogSort.TitleAZ -> null
        CatalogSort.AddedNewest -> "Added newest"
        CatalogSort.ArtistAZ -> "Artist A–Z"
        CatalogSort.YearNewest -> "Year newest"
    }

    val showClearSelections = selectedSortLabel != null || selectedFilter != null || selectedGroup != null

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
                            Text("Find a Pressing")
                        }
                        if (showDevSettings) {
                            IconButton(onClick = onOpenDevSettings) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Developer settings",
                                )
                            }
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

                        selectedSort = selectedSortLabel,
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

                        onSortSelect = { label ->
                            val selected = when (label) {
                                "Title A–Z" -> CatalogSort.TitleAZ
                                "Artist A–Z" -> CatalogSort.ArtistAZ
                                "Year newest" -> CatalogSort.YearNewest
                                "Added newest" -> CatalogSort.AddedNewest
                                else -> CatalogSort.TitleAZ
                            }
                            onSortChange(selected)
                            isSortExpanded = false
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
                            onSortChange(CatalogSort.TitleAZ)
                            selectedFilter = null
                            selectedGroup = null
                            isSortExpanded = false
                            filterExpanded = false
                            groupExpanded = false
                        },
                    )


                    // Divider between chips bar and list
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    DensityAndViewModeRow(
                        viewMode = viewMode,
                        onViewModeChange = onViewModeChange,
                        density = density,
                        onDensityChange = onDensityChange,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    val densitySpec = densitySpec(density)
                    if (viewMode == CatalogViewMode.GRID) {
                        ReleaseGrid(
                            releases = releases,
                            onOpenRelease = onOpenRelease,
                            densitySpec = densitySpec,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            // Only bottom padding needed now (rail). The top-gap bug was the 56.dp.
                            contentPadding = PaddingValues(bottom = 120.dp),
                        ) {
                            items(
                                items = releases,
                                key = { it.catalogItemId },
                            ) { item ->
                                ReleaseRow(
                                    item = item,
                                    densitySpec = densitySpec,
                                    onClick = { onOpenRelease(item.catalogItemId) },
                                    onDelete = { onDelete(item) },
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
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
                    onScanBarcode = { railMode = RailMode.Idle; onOpenScanConveyor() },
                    onAddAlbum = { railMode = RailMode.Idle; onAddAlbum() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ReleaseRow(
    item: CatalogItemSummary,
    densitySpec: CatalogDensitySpec,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = densitySpec.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtwork(
            artworkUrl = item.primaryArtworkUri,
            contentDescription = "${item.displayArtistLine} — ${item.displayTitle}",
            size = densitySpec.artworkSize,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle,
                style = densitySpec.titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = densitySpec.titleMaxLines,
            )

            val artistLine = item.displayArtistLine.takeIf { it.isNotBlank() } ?: "—"
            Text(
                text = artistLine,
                style = densitySpec.artistStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = densitySpec.artistMaxLines,
            )

            val yearText = item.releaseYear?.toString()
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

@Composable
private fun DensityAndViewModeRow(
    viewMode: CatalogViewMode,
    onViewModeChange: (CatalogViewMode) -> Unit,
    density: CatalogDensity,
    onDensityChange: (CatalogDensity) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = viewMode == CatalogViewMode.LIST,
            onClick = { onViewModeChange(CatalogViewMode.LIST) },
            label = { Text("List") },
        )
        FilterChip(
            selected = viewMode == CatalogViewMode.GRID,
            onClick = { onViewModeChange(CatalogViewMode.GRID) },
            label = { Text("Gallery") },
        )
        Spacer(modifier = Modifier.width(12.dp))
        FilterChip(
            selected = density == CatalogDensity.COMPACT,
            onClick = { onDensityChange(CatalogDensity.COMPACT) },
            label = { Text("Compact") },
        )
        FilterChip(
            selected = density == CatalogDensity.SPACIOUS,
            onClick = { onDensityChange(CatalogDensity.SPACIOUS) },
            label = { Text("Spacious") },
        )
    }
}

@Composable
private fun ReleaseGrid(
    releases: List<CatalogItemSummary>,
    onOpenRelease: (String) -> Unit,
    densitySpec: CatalogDensitySpec,
) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = releases,
            key = { it.catalogItemId },
        ) { item ->
            ReleaseTile(
                item = item,
                densitySpec = densitySpec,
                onClick = { onOpenRelease(item.catalogItemId) },
            )
        }
    }
}

@Composable
private fun ReleaseTile(
    item: CatalogItemSummary,
    densitySpec: CatalogDensitySpec,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AlbumArtwork(
            artworkUrl = item.primaryArtworkUri,
            contentDescription = "${item.displayArtistLine} — ${item.displayTitle}",
            size = densitySpec.gridArtworkSize,
            cornerRadius = 10.dp,
        )
        Text(
            text = item.displayTitle,
            style = densitySpec.titleStyle,
            maxLines = densitySpec.titleMaxLines,
        )
        Text(
            text = item.displayArtistLine.ifBlank { "—" },
            style = densitySpec.artistStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = densitySpec.artistMaxLines,
        )
    }
}

private data class CatalogDensitySpec(
    val rowPadding: Dp,
    val artworkSize: Dp,
    val gridArtworkSize: Dp,
    val titleStyle: TextStyle,
    val artistStyle: TextStyle,
    val titleMaxLines: Int,
    val artistMaxLines: Int,
)

@Composable
private fun densitySpec(density: CatalogDensity): CatalogDensitySpec {
    return when (density) {
        CatalogDensity.COMPACT -> CatalogDensitySpec(
            rowPadding = 8.dp,
            artworkSize = 48.dp,
            gridArtworkSize = 120.dp,
            titleStyle = MaterialTheme.typography.titleSmall,
            artistStyle = MaterialTheme.typography.bodySmall,
            titleMaxLines = 1,
            artistMaxLines = 1,
        )
        CatalogDensity.SPACIOUS -> CatalogDensitySpec(
            rowPadding = 12.dp,
            artworkSize = 64.dp,
            gridArtworkSize = 140.dp,
            titleStyle = MaterialTheme.typography.titleMedium,
            artistStyle = MaterialTheme.typography.bodyMedium,
            titleMaxLines = 2,
            artistMaxLines = 1,
        )
    }
}
