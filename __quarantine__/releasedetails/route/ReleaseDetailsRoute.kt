package com.zak.pressmark.feature.releasedetails.route

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.core.ui.dialog.ReleaseEditDialog
import com.zak.pressmark.core.ui.dialog.ReleaseEditFields
import com.zak.pressmark.feature.releasedetails.screen.ReleaseDetailsScreen
import com.zak.pressmark.feature.releasedetails.vm.ReleaseDetailsViewModel

@Composable
fun ReleaseDetailsRoute(
    vm: ReleaseDetailsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val release by vm.release.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val discogsExtras by vm.discogsExtras.collectAsStateWithLifecycle()

    ReleaseDetailsScreen(
        details = release,
        discogsExtras = discogsExtras,
        snackMessage = ui.snackMessage,
        onSnackShown = { vm.dismissSnack() },
        didDelete = ui.didDelete,
        onDeletedNavigatedAway = onBack,
        onBack = onBack,
        onOpenEdit = { vm.openEdit() },
        onOpenDeleteConfirm = { vm.openDeleteConfirm() },
        onClearCover = { vm.clearCover() },
        onSetDiscogsCover = { url, rid -> vm.setDiscogsCover(url, rid) },
        modifier = modifier,
    )

    if (ui.editOpen && release != null) {
        ReleaseEditDialog(
            initial = ReleaseEditFields(
                title = release!!.title,
                artist = release!!.artistLine,
                year = release!!.releaseYear,
                catalogNo = release!!.catalogNo,
                label = release!!.label,
                format = release!!.format,
                barcode = release!!.barcode,
                country = release!!.country,
                releaseType = release!!.releaseType,
                notes = release!!.notes,
            ),
            onDismiss = { vm.closeEdit() },
            onSave = { fields ->
                vm.saveEdits(
                    title = fields.title,
                    rawArtist = fields.artist,
                    releaseYear = fields.year,
                    label = fields.label,
                    catalogNo = fields.catalogNo,
                    format = fields.format,
                    barcode = fields.barcode,
                    country = fields.country,
                    releaseType = fields.releaseType,
                    notes = fields.notes,
                )
            },
        )
    }

    if (ui.deleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = { vm.closeDeleteConfirm() },
            title = { Text("Delete release?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteRelease() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { vm.closeDeleteConfirm() }) { Text("Cancel") }
            },
        )
    }
}
