package com.zak.pressmark.feature.albumdetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Color


import com.zak.pressmark.data.local.entity.AlbumEntity

@Composable
fun EditAlbumDialog(
    album: AlbumEntity,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        artist: String,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        //tracklist: String,
        //notes: String?,
    ) -> Unit,
) {
    var title by remember(album.id) { mutableStateOf(album.title) }
    var artist by remember(album.id) { mutableStateOf(album.artist) }
    var releaseYear by remember(album.id) { mutableStateOf(album.releaseYear?.toString() ?: "") }
    var catalogNo by remember(album.id) { mutableStateOf(album.catalogNo ?: "") }
    var label by remember(album.id) { mutableStateOf(album.label ?: "") }
    //var tracklist by remember(album.id) { mutableStateOf(album.tracklist ?: "") }
    //var notes by remember(album.id) { mutableStateOf(album.notes ?: "") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
        unfocusedContainerColor = Color.Transparent,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,


    )


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit album") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, colors = textFieldColors, singleLine = true)
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, colors = textFieldColors, singleLine = true)
                OutlinedTextField(value = releaseYear, onValueChange = { releaseYear = it }, label = { Text("Year") }, colors = textFieldColors, singleLine = true)
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, colors = textFieldColors, singleLine = true)
                OutlinedTextField(value = catalogNo, onValueChange = { catalogNo = it }, label = { Text("Catalog #") }, colors = textFieldColors, singleLine = true)
                //OutlinedTextField(value = tracklist, onValueChange = { tracklist = it }, label = { Text("Tracklist") }, colors = textFieldColors)
                //OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, colors = textFieldColors)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        title,
                        artist,
                        releaseYear.toIntOrNull(),
                        catalogNo,
                        label,
                        //tracklist,
                        //notes,
                        )
                }
            )
            { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
