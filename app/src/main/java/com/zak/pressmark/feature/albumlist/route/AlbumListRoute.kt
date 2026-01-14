package com.zak.pressmark.feature.albumlist.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.core.ui.dialog.AlbumEditDialog
import com.zak.pressmark.core.ui.dialog.AlbumEditFields
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import com.zak.pressmark.feature.albumlist.screen.AlbumListScreen
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModel

@Composable
fun AlbumListRoute(
    vm: AlbumListViewModel,
    graph: AppGraph,
    onOpenAlbum: (albumId: String) -> Unit,
    onAddAlbum: () -> Unit,
    onOpenCoverSearch: (albumId: String, artist: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val albums by vm.albumsWithArtistName.collectAsStateWithLifecycle()
    val uiState by vm.ui.collectAsStateWithLifecycle()

    var rowToEdit by remember { mutableStateOf<AlbumWithArtistName?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    AlbumListScreen(
        albums = albums,
        snackMessage = uiState.snackMessage,
        onSnackShown = { vm.dismissSnack() },
        onAddAlbum = onAddAlbum,
        onOpenAlbum = onOpenAlbum,
        onDelete = { row -> vm.deleteAlbum(row.album) },
        onFindCover = { row ->
            val albumId = row.album.id
            val artist = row.artistDisplayName?.trim().orEmpty()
            val title = row.album.title
            onOpenCoverSearch(albumId, artist, title)
        },
        onEdit = { row ->
            rowToEdit = row
            showEditDialog = true
        },
        modifier = modifier,
    )

    // ---- Edit dialog wiring (still Route-owned; optional to move later) ----
    if (showEditDialog) {
        rowToEdit?.let { row ->
            val a = row.album
            val artistName = row.artistDisplayName?.trim().orEmpty()

            AlbumEditDialog(
                initial = AlbumEditFields(
                    title = a.title,
                    artist = artistName,
                    year = a.releaseYear,
                    catalogNo = a.catalogNo,
                    label = a.label,
                    format = a.format,
                ),
                onDismiss = {
                    showEditDialog = false
                    rowToEdit = null
                },
                onSave = { fields ->
                    // NOTE: this assumes your AlbumListViewModel.updateAlbumFromList() is already corrected
                    // to not reference `graph` directly (i.e., uses injected repos).
                    vm.updateAlbumFromList(
                        albumId = a.id,
                        title = fields.title,
                        artist = fields.artist,
                        releaseYear = fields.year,
                        catalogNo = fields.catalogNo,
                        label = fields.label,
                        format = fields.format,
                    )
                    showEditDialog = false
                    rowToEdit = null
                },
            )
        }
    }
}
