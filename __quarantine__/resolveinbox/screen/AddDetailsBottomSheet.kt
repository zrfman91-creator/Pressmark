package com.zak.pressmark.feature.resolveinbox.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDetailsBottomSheet(
    title: String?,
    artist: String?,
    label: String?,
    catalogNo: String?,
    onDismiss: () -> Unit,
    onSave: (title: String?, artist: String?, label: String?, catalogNo: String?, format: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var titleText by remember { mutableStateOf(title.orEmpty()) }
    var artistText by remember { mutableStateOf(artist.orEmpty()) }
    var labelText by remember { mutableStateOf(label.orEmpty()) }
    var catalogText by remember { mutableStateOf(catalogNo.orEmpty()) }
    var formatText by remember { mutableStateOf("") }

    LaunchedEffect(title, artist, label, catalogNo) {
        titleText = title.orEmpty()
        artistText = artist.orEmpty()
        labelText = label.orEmpty()
        catalogText = catalogNo.orEmpty()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add details")
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = artistText,
                onValueChange = { artistText = it },
                label = { Text("Artist") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = catalogText,
                onValueChange = { catalogText = it },
                label = { Text("Catalog number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = formatText,
                onValueChange = { formatText = it },
                label = { Text("Format") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    onSave(
                        titleText.trim().takeIf { it.isNotBlank() },
                        artistText.trim().takeIf { it.isNotBlank() },
                        labelText.trim().takeIf { it.isNotBlank() },
                        catalogText.trim().takeIf { it.isNotBlank() },
                        formatText.trim().takeIf { it.isNotBlank() },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Search again")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
