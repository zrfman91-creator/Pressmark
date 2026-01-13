// file: app/src/main/java/com/zak/pressmark/ui/albumlist/components/AddAlbumDialog.kt
package com.zak.pressmark.feature.albumlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddAlbumDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, year: Int?, catalogNo: String, label: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var yearText by rememberSaveable { mutableStateOf("") }
    var catalogNo by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }

    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val t = title.trim()
    val a = artist.trim()
    val yRaw = yearText.trim()
    val yearOrNull: Int? = yRaw.takeIf { it.isNotBlank() }?.toIntOrNull()

    val titleError = error != null && t.isBlank()
    val artistError = error != null && a.isBlank()
    val yearError = error != null && yRaw.isNotBlank() && yearOrNull == null

    val canSave = t.isNotBlank() && a.isNotBlank() && (yRaw.isBlank() || yearOrNull != null)

    // ---- Styling knobs (change these) ----
    val dialogContainer = MaterialTheme.colorScheme.secondaryContainer
    val fieldContainer = MaterialTheme.colorScheme.onPrimary
    // --------------------------------------

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogContainer, // ✅ dialog background
        title = { Text("Add album") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            )
            {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Title*") },
                    isError = titleError,
                    supportingText = { if (titleError) Text("Required") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldContainer,
                        unfocusedContainerColor = fieldContainer),)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Artist*") },
                    isError = artistError,
                    supportingText = { if (artistError) Text("Required") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldContainer,
                        unfocusedContainerColor = fieldContainer
                    ),
                )

                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Release year") },
                    isError = yearError,
                    supportingText = {
                        if (yearError) Text("Must be a number (e.g., 1999)")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldContainer,
                        unfocusedContainerColor = fieldContainer
                    ),
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Label") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldContainer,
                        unfocusedContainerColor = fieldContainer
                    ),
                )

                OutlinedTextField(
                    value = catalogNo,
                    onValueChange = { catalogNo = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Catalog #") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldContainer,
                        unfocusedContainerColor = fieldContainer
                    ),
                )

                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    error = null
                    if (t.isBlank() || a.isBlank()) {
                        error = "Title and artist are required."
                        return@TextButton
                    }
                    if (yRaw.isNotBlank() && yearOrNull == null) {
                        error = "Release year must be a number."
                        return@TextButton
                    }

                    onSave(
                        t,
                        a,
                        yearOrNull,
                        catalogNo.trim(),
                        label.trim()
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
