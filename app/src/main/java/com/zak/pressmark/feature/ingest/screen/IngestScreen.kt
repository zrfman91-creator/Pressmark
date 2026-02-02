// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/screen/IngestScreen.kt
package com.zak.pressmark.feature.ingest.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.ui.InlineStatusCard
import com.zak.pressmark.feature.ingest.scan.CameraPreview
import com.zak.pressmark.feature.ingest.scan.DefaultReticle
import com.zak.pressmark.feature.ingest.scan.MlKitBarcodeAnalyzer
import com.zak.pressmark.feature.ingest.scan.ReticleOverlay
import com.zak.pressmark.feature.ingest.vm.IngestUiState

enum class IngestMode { SCAN, MANUAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngestScreen(
    state: IngestUiState,

    // Hoisted: route owns “what mode am I in?”
    mode: IngestMode,
    onModeChange: (IngestMode) -> Unit,

    onBack: () -> Unit,

    onBarcodeChanged: (String) -> Unit,
    onLookupBarcode: () -> Unit,
    onSetAutoReopen: (Boolean) -> Unit,

    onClearManualInputs: () -> Unit,
    onSetManualOverlayExpanded: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    var torchEnabled by remember { mutableStateOf(false) }
    var torchAvailable by remember { mutableStateOf(false) }

    val startScanMs = remember { SystemClock.elapsedRealtime() }

    val analyzer = remember {
        MlKitBarcodeAnalyzer(
            isEnabled = { mode == IngestMode.SCAN && !state.manualEntryExpanded },
            warmupMs = 1100L,
            requiredConsecutiveFrames = 30,
            cooldownMs = 1500L,
            requireInReticle = true,
            reticle = DefaultReticle,
            onBarcodeDetected = { barcode ->
                onBarcodeChanged(barcode)
                onLookupBarcode()
                onModeChange(IngestMode.MANUAL)
            },
        )
    }

    fun switchToScan() {
        onModeChange(IngestMode.SCAN)
        onSetManualOverlayExpanded(false)
        analyzer.reset()
        torchEnabled = false
    }

    fun switchToManual() {
        onModeChange(IngestMode.MANUAL)
        torchEnabled = false
    }

    LaunchedEffect(mode) {
        torchEnabled = false
        if (mode == IngestMode.SCAN) {
            analyzer.reset()
            if (!hasCameraPermission) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add to library", style = MaterialTheme.typography.displayLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onSetManualOverlayExpanded(false)
                            onClearManualInputs()
                            onBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                actions = {
                    if (mode == IngestMode.SCAN) {
                        OutlinedButton(
                            onClick = { switchToManual() },
                            enabled = !state.isLoading,
                            modifier = Modifier.padding(end = 8.dp),
                        ) { Text("Manual") }
                    } else {
                        OutlinedButton(
                            onClick = { switchToScan() },
                            enabled = !state.isLoading,
                            modifier = Modifier.padding(end = 8.dp),
                        ) { Text("Scan") }
                    }
                },
            )
        },
        bottomBar = {
            if (mode == IngestMode.MANUAL) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                ) {
                    Button(
                        onClick = { switchToScan() },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Scan with camera")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                ) {
                    OutlinedButton(
                        onClick = { switchToManual() },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Enter barcode manually")
                    }
                }
            }
        },
    ) { padding ->
        if (mode == IngestMode.SCAN) {
            ScanPanel(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                hasCameraPermission = hasCameraPermission,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                },
                torchEnabled = torchEnabled,
                torchAvailable = torchAvailable,
                onTorchAvailable = { torchAvailable = it },
                onToggleTorch = { torchEnabled = !torchEnabled },
                analyzer = analyzer,
                overlayBlocked = state.manualEntryExpanded,
                elapsedMs = SystemClock.elapsedRealtime() - startScanMs,
            )
        } else {
            ManualBarcodePanel(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                state = state,
                onBarcodeChanged = onBarcodeChanged,
                onLookup = onLookupBarcode,
                onSetAutoReopen = onSetAutoReopen,
            )
        }
    }
}

@Composable
private fun ScanPanel(
    modifier: Modifier,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    torchEnabled: Boolean,
    torchAvailable: Boolean,
    onTorchAvailable: (Boolean) -> Unit,
    onToggleTorch: () -> Unit,
    analyzer: MlKitBarcodeAnalyzer,
    overlayBlocked: Boolean,
    elapsedMs: Long,
) {
    Box(modifier = modifier) {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                InlineStatusCard(message = "Camera access lets Pressmark scan barcodes.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Grant camera access") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Open settings") }
            }
            return
        }

        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            torchEnabled = torchEnabled,
            onTorchAvailable = onTorchAvailable,
            analyzer = analyzer,
        )

        ReticleOverlay(modifier = Modifier.fillMaxSize())

        if (torchAvailable) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                shadowElevation = 2.dp,
                tonalElevation = 2.dp,
            ) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (torchEnabled) "Turn torch off" else "Turn torch on",
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (overlayBlocked) {
                Text("Scanner paused", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Close manual entry to resume scanning.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Align the barcode within the frame", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ready • ${elapsedMs}ms", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ManualBarcodePanel(
    modifier: Modifier,
    state: IngestUiState,
    onBarcodeChanged: (String) -> Unit,
    onLookup: () -> Unit,
    onSetAutoReopen: (Boolean) -> Unit,
) {
    val canRetry = !state.isLoading && state.barcode.isNotBlank()

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLookup,
            enabled = !state.isLoading && state.barcode.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Lookup on Discogs")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Auto-reopen scanner")
            Switch(
                checked = state.autoReopenScanner,
                onCheckedChange = onSetAutoReopen,
            )
        }

        if (state.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
        }

        state.errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(12.dp))
            InlineStatusCard(
                message = msg,
                actionLabel = if (canRetry) "Retry" else null,
                onAction = if (canRetry) onLookup else null,
            )
        }

        state.infoMessage?.let { msg ->
            Spacer(modifier = Modifier.height(12.dp))
            InlineStatusCard(message = msg)
        }
    }
}
