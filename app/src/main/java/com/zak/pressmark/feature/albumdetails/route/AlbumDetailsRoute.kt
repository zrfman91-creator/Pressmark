// FILE: app/src/main/java/com/zak/pressmark/feature/albumdetails/route/AlbumDetailsRoute.kt
package com.zak.pressmark.feature.albumdetails.route

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.core.ui.dialog.AlbumEditDialog
import com.zak.pressmark.core.ui.dialog.AlbumEditFields
import com.zak.pressmark.feature.albumdetails.screen.AlbumDetailsScreen
import com.zak.pressmark.feature.albumdetails.vm.AlbumDetailsViewModel

@Composable
fun AlbumDetailsRoute(
    vm: AlbumDetailsViewModel,
    onBack: () -> Unit,
    onOpenArtist: (artistId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val row by vm.album.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()

    AlbumDetailsScreen(
        row = row,
        snackMessage = ui.snackMessage,
        onSnackShown = { vm.dismissSnack() },
        didDelete = ui.didDelete,
        onDeletedNavigatedAway = onBack,
        onBack = onBack,
        onOpenArtist = onOpenArtist,
        onOpenEdit = { vm.openEdit() },
        onOpenDeleteConfirm = { vm.openDeleteConfirm() },
        onRefreshCover = { vm.refreshDiscogsCover() },
        onClearCover = { vm.clearCover() },
        onSetDiscogsCover = { url, rid -> vm.setDiscogsCover(url, rid) },
        modifier = modifier,
    )

    if (ui.editOpen && row != null) {
        val a = row!!.album
        val artistName = row!!.artist

        AlbumEditDialog(
            initial = AlbumEditFields(
                title = a.title.orEmpty(),
                artist = artistName.orEmpty(),
                year = a.releaseYear,
                catalogNo = a.catalogNo,
                label = a.label,
                format = a.format,
            ),
            onDismiss = { vm.closeEdit() },
            onSave = { fields ->
                vm.saveEdits(
                    title = fields.title,
                    artist = fields.artist,
                    releaseYear = fields.year,
                    catalogNo = fields.catalogNo,
                    label = fields.label,
                    format = fields.format,
                )
            },
        )
    }

    if (ui.deleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = { vm.closeDeleteConfirm() },
            title = { Text("Delete album?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteAlbum() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { vm.closeDeleteConfirm() }) { Text("Cancel") }
            },
        )
    }
}
