package com.zak.pressmark.feature.scanconveyor.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.UploadFile
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
import androidx.compose.ui.text.style.TextAlign
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
    var showBarcodeScanner by remember { mutableStateOf(false) }

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
        bottomBar = {
            // Pinned primary CTA, safely above system nav bar (and keyboard if shown)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showQuickAdd = true },
                        modifier = Modifier
                            .widthIn(min = 260.dp)
                            .heightIn(min = 64.dp),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.width(180.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ShortText,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Quick add",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = onCaptureCover,
                        modifier = Modifier
                            .widthIn(min = 260.dp)
                            .heightIn(min = 64.dp),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.width(180.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cover image search",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Button(
                    onClick = { showBarcodeScanner = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .heightIn(min = 80.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                   Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = "Scan barcode",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
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



            Spacer(modifier = Modifier.height(0.dp))

            Button(
                onClick = onOpenInbox,
                modifier = Modifier
                    .widthIn(min = 260.dp)
                    .heightIn(min = 64.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.width(180.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Go to Inbox",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Button(
                onClick = onImportCsv,
                modifier = Modifier
                    .widthIn(min = 260.dp)
                    .heightIn(min = 64.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.width(180.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.Outlined.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Import Library",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
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

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onDismiss = { showBarcodeScanner = false },
            onDetected = { barcode ->
                onScanBarcode(barcode)
                showBarcodeScanner = false
            },
            onManualEntry = {
                showBarcodeScanner = false
                showBarcodeEntry = true
            },
        )
    }
}
