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
    onOpenRelease: (releaseId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val releases = vm.releaseListItems.collectAsStateWithLifecycle().value
    val uiState = vm.ui.collectAsStateWithLifecycle().value
    val query = vm.query.collectAsStateWithLifecycle().value
    val sort = vm.sort.collectAsStateWithLifecycle().value

    AlbumListScreen(
        releases = releases,

        query = query,
        onQueryChange = vm::setQuery,

        snackMessage = uiState.snackMessage,
        onSnackShown = { vm.dismissSnack() },

        onAddAlbum = onAddAlbum,
        onOpenRelease = onOpenRelease,
        onDelete = { item -> vm.deleteRelease(item) },

        sort = sort,
        onSortChange = vm::setSort,

        modifier = modifier,
    )
}
