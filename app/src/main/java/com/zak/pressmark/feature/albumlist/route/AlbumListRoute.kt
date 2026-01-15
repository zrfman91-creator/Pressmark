package com.zak.pressmark.feature.albumlist.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.core.ui.dialog.AlbumEditDialog
import com.zak.pressmark.core.ui.dialog.AlbumEditFields
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import com.zak.pressmark.feature.albumdetails.components.AlbumDetailsHero
import com.zak.pressmark.feature.albumlist.components.AlbumArtwork
import com.zak.pressmark.feature.albumlist.screen.AlbumListScreen
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModel

@Composable
fun AlbumListRoute(
    vm: AlbumListViewModel,
    graph: AppGraph,
    onOpenAlbum: (albumId: String) -> Unit,
    onAddAlbum: () -> Unit,
    onOpenCoverSearch: (albumId: String, artist: String, title: String) -> Unit,
    savedAlbumId: String?,
    onAlbumSavedConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val albums by vm.albumsWithArtistName.collectAsStateWithLifecycle()
    val uiState by vm.ui.collectAsStateWithLifecycle()

    var rowToEdit by remember { mutableStateOf<AlbumWithArtistName?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var savedAlbumForDialog by remember { mutableStateOf<AlbumWithArtistName?>(null) }

    LaunchedEffect(savedAlbumId, albums) {
        if (savedAlbumId != null) {
            val match = albums.firstOrNull { it.album.id == savedAlbumId }
            if (match != null) {
                savedAlbumForDialog = match
                onAlbumSavedConsumed()
            }
        }
    }

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

    savedAlbumForDialog?.let { row ->
        AlertDialog(
            onDismissRequest = { savedAlbumForDialog = null },
            confirmButton = {
                TextButton(onClick = { savedAlbumForDialog = null }) {
                    Text("OK")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AlbumDetailsHero(
                        title = row.album.title,
                        artist = row.artist,
                        artworkSize = 180.dp,
                    ) {
                        AlbumArtwork(
                            artworkUrl = row.album.persistedArtworkUrl,
                            contentDescription = "${row.album.title} artwork",
                            size = 180.dp,
                            cornerRadius = 12.dp,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Added successfully",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}
