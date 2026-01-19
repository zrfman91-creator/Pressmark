// file: app/src/main/java/com/zak/pressmark/feature/albumlist/route/AlbumListRoute.kt
package com.zak.pressmark.feature.albumlist.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.albumlist.screen.AlbumListScreen
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModel

@Composable
fun AlbumListRoute(
    vm: AlbumListViewModel,
    onAddAlbum: () -> Unit,
    onOpenRelease: (releaseId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val releases = vm.releaseListItems.collectAsStateWithLifecycle().value
    val uiState = vm.ui.collectAsStateWithLifecycle().value

    AlbumListScreen(
        releases = releases,
        snackMessage = uiState.snackMessage,
        onSnackShown = { vm.dismissSnack() },
        onAddAlbum = onAddAlbum,
        onOpenRelease = onOpenRelease,
        onDelete = { item -> vm.deleteRelease(item.release) },
        modifier = modifier,
    )
}
