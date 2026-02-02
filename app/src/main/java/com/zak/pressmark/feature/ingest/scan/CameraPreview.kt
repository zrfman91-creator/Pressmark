// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/scan/CameraPreview.kt
package com.zak.pressmark.feature.ingest.scan

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/**
 * CameraX Preview + ImageAnalysis binder.
 *
 * - Owns the camera binding lifecycle (bind/unbind).
 * - Accepts an ImageAnalysis.Analyzer (e.g., MlKitBarcodeAnalyzer).
 * - Torch is controlled via [torchEnabled] when available.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    torchEnabled: Boolean,
    onTorchAvailable: (Boolean) -> Unit,
    analyzer: ImageAnalysis.Analyzer,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { analysisExecutor.shutdown() }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView },
        update = { view ->
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor, analyzer)
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
        },
    )

    LaunchedEffect(torchEnabled, camera) {
        val currentCamera = camera ?: return@LaunchedEffect
        if (currentCamera.cameraInfo.hasFlashUnit()) {
            currentCamera.cameraControl.enableTorch(torchEnabled)
        }
    }
}
