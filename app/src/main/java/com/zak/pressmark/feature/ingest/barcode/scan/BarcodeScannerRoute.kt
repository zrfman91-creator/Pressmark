// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/barcode/scan/BarcodeScannerRoute.kt
package com.zak.pressmark.feature.ingest.barcode.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.zak.pressmark.core.analytics.UxEventLogger
import com.zak.pressmark.core.ui.InlineStatusCard
import com.zak.pressmark.feature.ingest.barcode.ui.ManualIngestInputs
import com.zak.pressmark.feature.ingest.vm.IngestViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerRoute(
    onBarcodeDetected: (String) -> Unit,
    onCancel: () -> Unit,
    onManualEntry: () -> Unit,
    onManualSubmit: (ManualIngestInputs) -> Unit,
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

    val lastDetected = remember { mutableStateOf<String?>(null) }
    val lastDetectedAt = remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            vm.setManualEntryExpanded(false)
            vm.clearManualInputs()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode") },
                navigationIcon = {
                    IconButton(onClick = handleCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                BarcodeScannerCamera(
                    torchEnabled = torchEnabled,
                    onTorchAvailable = { torchAvailable = it },
                    onBarcodeDetected = { barcode ->
                        if (manualEntryExpanded) return@BarcodeScannerCamera
                        val now = System.currentTimeMillis()
                        val lastCode = lastDetected.value
                        val lastTime = lastDetectedAt.longValue
                        if (lastCode == barcode && now - lastTime < DEBOUNCE_MS) return@BarcodeScannerCamera
                        lastDetected.value = barcode
                        lastDetectedAt.longValue = now
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
                    Text("Align the barcode within the frame")
                }
            }
        }
    }
}

private const val DEBOUNCE_MS = 2000L

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun BarcodeScannerCamera(
    torchEnabled: Boolean,
    onTorchAvailable: (Boolean) -> Unit,
    onBarcodeDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val hasDetected = remember { AtomicBoolean(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { analysisExecutor.shutdown() }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { previewView },
    ) { view ->
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = view.surfaceProvider
        }

        val barcodeScanner = BarcodeScanning.getClient()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
            analyzeImageProxy(
                imageProxy = imageProxy,
                barcodeScanner = barcodeScanner,
                hasDetected = hasDetected,
                onBarcodeDetected = onBarcodeDetected,
            )
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val boundCamera = runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
            )
        }.getOrNull()

        camera = boundCamera
        onTorchAvailable(boundCamera?.cameraInfo?.hasFlashUnit() == true)
    }

    LaunchedEffect(torchEnabled, camera) {
        val currentCamera = camera ?: return@LaunchedEffect
        if (currentCamera.cameraInfo.hasFlashUnit()) {
            currentCamera.cameraControl.enableTorch(torchEnabled)
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun analyzeImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    hasDetected: AtomicBoolean,
    onBarcodeDetected: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    barcodeScanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            if (hasDetected.get()) return@addOnSuccessListener

            val raw = barcodes
                .firstOrNull { !it.rawValue.isNullOrBlank() }
                ?.rawValue
                ?.trim()

            if (!raw.isNullOrBlank()) {
                val digitsOnly = raw.filter(Char::isDigit).ifBlank { raw }
                if (hasDetected.compareAndSet(false, true)) {
                    onBarcodeDetected(digitsOnly)
                }
            }
        }
        .addOnFailureListener {
            // ignore; user can keep scanning
        }
        .addOnCompleteListener {
            imageProxy.close()
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

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AnalyticsEntryPoint {
    fun uxEventLogger(): UxEventLogger
}

@Composable
private fun rememberAnalyticsLogger(context: android.content.Context): UxEventLogger {
    return remember(context) {
        EntryPointAccessors.fromApplication(context, AnalyticsEntryPoint::class.java).uxEventLogger()
    }
}
