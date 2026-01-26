// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/barcode/scan/BarcodeScannerRoute.kt
package com.zak.pressmark.feature.ingest.barcode.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerRoute(
    onBarcodeDetected: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val lastDetected = remember { mutableStateOf<String?>(null) }
    val lastDetectedAt = remember { mutableStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode") },
                navigationIcon = {
                    Text(
                        text = "Back",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { onCancel() },
                    )
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
                Text("Camera permission is required to scan barcodes.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant permission")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                BarcodeScannerCamera(
                    onBarcodeDetected = { barcode ->
                        val now = System.currentTimeMillis()
                        val lastCode = lastDetected.value
                        val lastTime = lastDetectedAt.value
                        if (lastCode == barcode && now - lastTime < DEBOUNCE_MS) return@BarcodeScannerCamera
                        lastDetected.value = barcode
                        lastDetectedAt.value = now
                        onBarcodeDetected(barcode)
                    },
                )

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
    onBarcodeDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val hasDetected = remember { AtomicBoolean(false) }

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
            it.setSurfaceProvider(view.surfaceProvider)
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

        runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
            )
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
