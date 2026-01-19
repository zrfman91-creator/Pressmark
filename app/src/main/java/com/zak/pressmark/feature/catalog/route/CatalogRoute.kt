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
    val listItems = vm.catalogListItems.collectAsStateWithLifecycle().value
    val uiState = vm.ui.collectAsStateWithLifecycle().value
    val query = vm.query.collectAsStateWithLifecycle().value
    val sort = vm.sort.collectAsStateWithLifecycle().value
    val filter = vm.filter.collectAsStateWithLifecycle().value
    val grouping = vm.grouping.collectAsStateWithLifecycle().value

    AlbumListScreen(
        listItems = listItems,

        query = query,
        onQueryChange = vm::setQuery,

        snackMessage = uiState.snackMessage,
        onSnackShown = { vm.dismissSnack() },

        onAddAlbum = onAddAlbum,
        onOpenRelease = onOpenRelease,
        onDelete = { item -> vm.deleteRelease(item.release) },

        sort = sort,
        filter = filter,
        grouping = grouping,
        onSortChange = vm::setSort,
        onFilterChange = vm::setFilter,
        onGroupingChange = vm::setGrouping,

        modifier = modifier,
    )
}
