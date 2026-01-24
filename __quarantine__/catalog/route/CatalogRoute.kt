package com.zak.pressmark.feature.catalog.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.catalog.screen.AlbumListScreen
import com.zak.pressmark.feature.catalog.vm.AlbumListViewModel

@Composable
fun AlbumListRoute(
    vm: AlbumListViewModel,
    onAddAlbum: () -> Unit,
    onOpenRelease: (catalogItemId: String) -> Unit,
    onOpenScanConveyor: () -> Unit,
    showDevSettings: Boolean,
    onOpenDevSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val releases = vm.catalogItems.collectAsStateWithLifecycle().value
    val uiState = vm.ui.collectAsStateWithLifecycle().value
    val query = vm.query.collectAsStateWithLifecycle().value
    val sort = vm.sort.collectAsStateWithLifecycle().value
    val viewMode = vm.viewMode.collectAsStateWithLifecycle().value
    val density = vm.density.collectAsStateWithLifecycle().value

    AlbumListScreen(
        releases = releases,

        query = query,
        onQueryChange = vm::setQuery,

        snackMessage = uiState.snackMessage,
        onSnackShown = { vm.dismissSnack() },

        onAddAlbum = onAddAlbum,
        onOpenScanConveyor = onOpenScanConveyor,
        onOpenRelease = onOpenRelease,
        onDelete = { item -> vm.deleteCatalogItem(item) },

        sort = sort,
        onSortChange = vm::setSort,

        viewMode = viewMode,
        onViewModeChange = vm::setViewMode,
        density = density,
        onDensityChange = vm::setDensity,

        showDevSettings = showDevSettings,
        onOpenDevSettings = onOpenDevSettings,

        modifier = modifier,
    )
}
