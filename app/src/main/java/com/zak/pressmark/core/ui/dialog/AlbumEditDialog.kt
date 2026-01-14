// file: core/src/main/java/com/zak/pressmark/core/ui/dialog/AlbumEditDialog.kt
package com.zak.pressmark.core.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class AlbumEditFields(
    val title: String = "",
    val artist: String = "",
    val year: Int? = null,
    val catalogNo: String? = null,
    val label: String? = null,
    val format: String? = null,
)

@Composable
fun AlbumEditDialog(
    initial: AlbumEditFields,
    titleText: String = "Edit album",
    onDismiss: () -> Unit,
    onSave: (AlbumEditFields) -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var artist by remember { mutableStateOf(initial.artist) }
    var yearText by remember { mutableStateOf(initial.year?.toString().orEmpty()) }
    var catalogNo by remember { mutableStateOf(initial.catalogNo.orEmpty()) }
    var label by remember { mutableStateOf(initial.label.orEmpty()) }
    var formatText by remember { mutableStateOf(initial.format.orEmpty()) }

    // If caller recomposes with a new initial object, keep fields in sync.
    LaunchedEffect(initial) {
        title = initial.title
        artist = initial.artist
        yearText = initial.year?.toString().orEmpty()
        catalogNo = initial.catalogNo.orEmpty()
        label = initial.label.orEmpty()
        formatText = initial.format.orEmpty()
    }

    val year: Int? = yearText.trim().takeIf { it.isNotBlank() }?.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Artist") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Year") },
                    singleLine = true,
                    supportingText = {
                        val raw = yearText.trim()
                        if (raw.isNotEmpty() && raw.toIntOrNull() == null) {
                            Text("Enter a number", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Label") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = catalogNo,
                    onValueChange = { catalogNo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catalog #") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = formatText,
                    onValueChange = { formatText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Format") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        AlbumEditFields(
                            title = title.trim(),
                            artist = artist.trim(),
                            year = year,
                            catalogNo = catalogNo.trim().takeIf { it.isNotBlank() },
                            label = label.trim().takeIf { it.isNotBlank() },
                            format = formatText.trim().takeIf { it.isNotBlank() },
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
