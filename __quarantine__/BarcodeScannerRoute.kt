// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/screen/BarcodeScannerRoute.kt
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.zak.pressmark.core.ui.InlineStatusCard
import com.zak.pressmark.feature.ingest.scan.CameraPreview
import com.zak.pressmark.feature.ingest.scan.DefaultReticle
import com.zak.pressmark.feature.ingest.scan.MlKitBarcodeAnalyzer
import com.zak.pressmark.feature.ingest.scan.ReticleOverlay
import com.zak.pressmark.feature.ingest.vm.IngestViewModel

/**
 * Legacy scanner route kept for compatibility.
 *
 * Current single-flow implementation is IngestRoute/IngestScreen.
 * If this file is still referenced by navigation or tests, it must compile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerRoute(
    onBarcodeDetected: (String) -> Unit,
    onCancel: () -> Unit,
    onManualEntry: () -> Unit,
    manualEntryExpanded: Boolean,
) {
    val vm: IngestViewModel = hiltViewModel()

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var torchAvailable by remember { mutableStateOf(false) }
    val startTimeMs = remember { SystemClock.elapsedRealtime() }
    val analytics = rememberAnalyticsLogger(context)

    val handleCancel = {
        analytics.logEvent("pm_barcode_scan_fail", mapOf("reason" to "user_cancel"))
        onCancel()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            analytics.logEvent("pm_barcode_scan_fail", mapOf("reason" to "permission_denied"))
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // NOTE: legacy cleanup behavior; in the unified flow this should be owned by IngestRoute.
    DisposableEffect(Unit) {
        onDispose {
            vm.setManualEntryExpanded(false)
            vm.clearManualInputs()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add to library",
                        style = MaterialTheme.typography.displayLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = handleCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                InlineStatusCard(message = "Camera access lets Pressmark scan barcodes.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Grant camera access")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open settings")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onManualEntry,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Enter barcode manually")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // Analyzer now uses stability+cooldown+reticle gating (no oneShot parameter).
                val analyzer = remember(manualEntryExpanded, onBarcodeDetected) {
                    MlKitBarcodeAnalyzer(
                        isEnabled = { !manualEntryExpanded },
                        requiredConsecutiveFrames = 25,
                        cooldownMs = 900L,
                        requireInReticle = true,
                        reticle = DefaultReticle,
                        onBarcodeDetected = { barcode ->
                            analytics.logEvent(
                                "pm_barcode_scan_success",
                                mapOf(
                                    "format" to barcodeFormatLabel(barcode),
                                    "duration_ms" to (SystemClock.elapsedRealtime() - startTimeMs),
                                ),
                            )
                            onBarcodeDetected(barcode)
                        },
                    )
                }

                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    torchEnabled = torchEnabled,
                    onTorchAvailable = { torchAvailable = it },
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
                            onClick = { torchEnabled = !torchEnabled },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = if (torchEnabled) "Turn torch off" else "Turn torch on",
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("Align the barcode within the box")
                }
            }
        }
    }
}

private fun barcodeFormatLabel(barcode: String): String {
    val digitsOnly = barcode.filter(Char::isDigit)
    return when (digitsOnly.length) {
        8 -> "EAN-8"
        12 -> "UPC-A"
        13 -> "EAN-13"
        else -> "unknown"
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface AnalyticsEntryPoint {
    fun uxEventLogger(): com.zak.pressmark.core.analytics.UxEventLogger
}

@Composable
private fun rememberAnalyticsLogger(context: android.content.Context): com.zak.pressmark.core.analytics.UxEventLogger {
    return remember(context) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(context, AnalyticsEntryPoint::class.java).uxEventLogger()
    }
}
