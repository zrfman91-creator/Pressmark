package com.zak.pressmark.feature.scanconveyor.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanConveyorScreen(
    inboxCount: Int,
    libraryCount: Int,
    onScanBarcode: (String) -> Unit,
    onCaptureCover: () -> Unit,
    onQuickAdd: (title: String, artist: String) -> Unit,
    onImportCsv: () -> Unit,
    onOpenInbox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showQuickAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var showBarcodeEntry by remember { mutableStateOf(false) }

    LaunchedEffect(showQuickAdd) {
        if (!showQuickAdd) {
            title = ""
            artist = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Scan Conveyor") })
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Inbox: $inboxCount · Library: $libraryCount",
                style = MaterialTheme.typography.titleMedium,
            )

            Button(
                onClick = { showBarcodeEntry = true },
                modifier = Modifier.widthIn(min = 240.dp),
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Scan barcode")
            }

            Button(
                onClick = onCaptureCover,
                modifier = Modifier.widthIn(min = 240.dp),
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("No barcode? Capture cover")
            }

            Button(
                onClick = { showQuickAdd = true },
                modifier = Modifier.widthIn(min = 240.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ShortText, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Quick add")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onOpenInbox,
                modifier = Modifier.widthIn(min = 240.dp),
            ) {
                Icon(Icons.Outlined.Inventory2, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Go to Inbox")
            }

            Button(
                onClick = onImportCsv,
                modifier = Modifier.widthIn(min = 240.dp),
            ) {
                Icon(Icons.Outlined.CollectionsBookmark, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("CSV import")
            }
        }
    }

    if (showQuickAdd) {
        AlertDialog(
            onDismissRequest = { showQuickAdd = false },
            title = { Text("Quick add") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = { Text("Artist") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onQuickAdd(title.trim(), artist.trim())
                        showQuickAdd = false
                    },
                    enabled = title.isNotBlank() && artist.isNotBlank(),
                ) {
                    Text("Save to Inbox")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAdd = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showBarcodeEntry) {
        BarcodeEntryDialog(
            onDismiss = { showBarcodeEntry = false },
            onSave = { barcode ->
                onScanBarcode(barcode)
                showBarcodeEntry = false
            },
        )
    }
}
