package com.zak.pressmark.feature.scanconveyor.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
    var showBarcodeScanner by remember { mutableStateOf(false) }

    // Expand/collapse "No barcode" options below the primary scan button
    var showNoBarcodeActions by remember { mutableStateOf(false) }

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
            // Bottom action stack.
            // When options expand below the scan button, the scan button shifts upward naturally.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Secondary trigger: TextButton that disappears when options are visible
                AnimatedVisibility(
                    visible = !showNoBarcodeActions,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    TextButton(
                        onClick = { showNoBarcodeActions = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(min = 260.dp, max = 520.dp),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = "No barcode?",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                // Primary CTA pinned above nav bar
                Button(
                    onClick = {
                        // If the panel is open, collapse it before scanning
                        showNoBarcodeActions = false
                        showBarcodeScanner = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 260.dp, max = 520.dp)
                        .heightIn(min = 80.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Scan barcode",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))

                // Revealed options (appear below scan button; scan button shifts up as this expands)
                AnimatedVisibility(
                    visible = showNoBarcodeActions,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                showNoBarcodeActions = false
                                onCaptureCover()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Scan cover",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                showNoBarcodeActions = false
                                showQuickAdd = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ShortText,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Quick add",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Inbox: $inboxCount · Library: $libraryCount",
                    style = MaterialTheme.typography.titleMedium,
                )

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

            // Tap-outside-to-dismiss scrim (covers content area only; bottom bar remains interactive)
            if (showNoBarcodeActions) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.24f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            showNoBarcodeActions = false
                        },
                )
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
