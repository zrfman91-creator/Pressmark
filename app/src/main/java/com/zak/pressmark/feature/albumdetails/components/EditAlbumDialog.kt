package com.zak.pressmark.feature.albumdetails.components

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
import com.zak.pressmark.data.local.entity.AlbumEntity

@Composable
fun EditAlbumDialog(
    album: AlbumEntity,
    artistDisplayName: String,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, year: Int?, catalogNo: String?, label: String?) -> Unit,
) {
    EditAlbumDialog(
        album = album,
        artistDisplayName = artistDisplayName,
        format = album.format,
        onDismiss = onDismiss,
        onSave = { title, artist, year, catalogNo, label, _ ->
            onSave(title, artist, year, catalogNo, label)
        }
    )
}

@Composable
fun EditAlbumDialog(
    album: AlbumEntity,
    artistDisplayName: String,
    format: String?,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, year: Int?, catalogNo: String?, label: String?, format: String?) -> Unit,
) {
    var title by remember { mutableStateOf(album.title) }
    var artist by remember { mutableStateOf(artistDisplayName) }
    var yearText by remember { mutableStateOf(album.releaseYear?.toString().orEmpty()) }
    var catalogNo by remember { mutableStateOf(album.catalogNo.orEmpty()) }
    var label by remember { mutableStateOf(album.label.orEmpty()) }
    var formatText by remember { mutableStateOf(format.orEmpty()) }

    LaunchedEffect(artistDisplayName) { artist = artistDisplayName }
    LaunchedEffect(format) { formatText = format.orEmpty() }

    val year: Int? = yearText.trim().takeIf { it.isNotBlank() }?.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit album") },
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
                    // ✅ function type call must be positional args (no named args)
                    onSave(
                        title.trim(),
                        artist.trim(),
                        year,
                        catalogNo.trim().takeIf { it.isNotBlank() },
                        label.trim().takeIf { it.isNotBlank() },
                        formatText.trim().takeIf { it.isNotBlank() },
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
