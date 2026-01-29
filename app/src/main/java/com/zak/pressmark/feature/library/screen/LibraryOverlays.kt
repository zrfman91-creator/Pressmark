package com.zak.pressmark.feature.library.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.zak.pressmark.feature.library.vm.LibraryItemUi

@Composable
fun LibraryOverlays(
    deleteTarget: LibraryItemUi?,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (LibraryItemUi) -> Unit,
) {
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Remove “${target.title}”?") },
            text = { Text("This will remove it from your library.") },
            confirmButton = {
                Button(
                    onClick = { onConfirmDelete(target) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("Cancel") }
            },
        )
    }
}
