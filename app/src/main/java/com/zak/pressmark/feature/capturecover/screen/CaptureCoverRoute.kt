package com.zak.pressmark.feature.capturecover.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Simple CameraX capture screen:
 * - requests CAMERA permission (runtime)
 * - shows preview
 * - takes photo
 * - saves to app-private storage (filesDir) so it will NOT appear in the system gallery
 * - returns a file:// Uri pointing at durable storage
 *
 * This fixes the common "black preview" issue caused by missing permission or binding before
 * a surface provider is ready.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCoverCaptureRoute(
    onBack: () -> Unit,
    onCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var errorText by remember { mutableStateOf<String?>(null) }
    var captured by remember { mutableStateOf<CapturedPhoto?>(null) }
    var flashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
            errorText = if (!granted) {
                "Camera permission is required to take a cover photo."
            } else {
                null
            }
        }
    )

    // If we don't have permission, request it once on entry.
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            // More reliable across devices than PERFORMANCE for some OEMs.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // Bind/unbind camera only when permission is granted.
    LaunchedEffect(hasPermission, captured, lifecycleOwner) {
        if (!hasPermission || captured != null) {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            return@LaunchedEffect
        }

        val provider = awaitCameraProvider(context)
        val preview = Preview.Builder().build().also { p ->
            p.surfaceProvider = previewView.surfaceProvider
        }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
            )
            errorText = null
        } catch (t: Throwable) {
            errorText = t.message ?: "Failed to start camera."
        }
    }

    LaunchedEffect(flashOn) {
        imageCapture.flashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    DisposableEffect(hasPermission) {
        onDispose {
            if (hasPermission) {
                runCatching {
                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                }
            }
        }
    }

    val container = MaterialTheme.colorScheme.background
    val topBarColor = MaterialTheme.colorScheme.primaryContainer

    Surface(modifier = modifier.fillMaxSize(), color = container) {
        Scaffold(
            containerColor = container,
            topBar = {
                TopAppBar(
                    title = { Text("Capture cover") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Flash toggle (only while actively capturing).
                        if (hasPermission && captured == null && errorText == null) {
                            IconButton(onClick = { flashOn = !flashOn }) {
                                Icon(
                                    imageVector = if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                    contentDescription = if (flashOn) "Flash on" else "Flash off",
                                )
                            }
                        }
                    }
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                if (!hasPermission) {
                    PermissionGate(
                        message = errorText ?: "Camera permission is required.",
                        onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onCancel = onBack,
                    )
                } else if (captured != null) {
                    ConfirmCapturedCover(
                        captured = captured!!,
                        onRetake = {
                            captured?.file?.delete()
                            captured = null
                            errorText = null
                            isCapturing = false
                        },
                        onUse = { onCaptured(captured!!.uri) },
                    )
                } else {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize(),
                    )

                    CaptureFrameOverlay(modifier = Modifier.fillMaxSize())

                    Text(
                        text = "Center the album cover and tap the shutter",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                    )

                    val canCapture = hasPermission && errorText == null && !isCapturing
                    ShutterButton(
                        enabled = canCapture,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        onClick = {
                            if (!canCapture) return@ShutterButton
                            isCapturing = true
                            scope.launch {
                                takePhoto(
                                    context = context,
                                    imageCapture = imageCapture,
                                    onSuccess = { result ->
                                        isCapturing = false
                                        captured = result
                                    },
                                    onError = { msg ->
                                        isCapturing = false
                                        errorText = msg
                                    },
                                )
                            }
                        }
                    )
                }

                errorText?.let { msg ->
                    // Show errors even on top of the preview.
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionGate(
    message: String,
    onRequest: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
            Button(onClick = onRequest) { Text("Grant") }
        }
    }
}

private fun createDurableCoverFile(context: Context): File {
    val dir = File(context.filesDir, "covers").apply {
        if (!exists()) mkdirs()
    }
    // Fallback to filesDir if directory creation fails for any reason.
    val targetDir = if (dir.exists() && dir.isDirectory) dir else context.filesDir
    return File(targetDir, "cover_" + System.currentTimeMillis() + ".jpg")
}

private data class CapturedPhoto(
    val uri: Uri,
    val file: File?,
)

@Composable
private fun ConfirmCapturedCover(
    captured: CapturedPhoto,
    onRetake: () -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(captured.uri)
                .crossfade(true)
                .build(),
            contentDescription = "Captured cover",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Subtle top label
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
        ) {
            Text(
                text = "Use this photo?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onRetake) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Retake")
            }
            Button(onClick = onUse) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Use photo")
            }
        }
    }
}

@Composable
private fun CaptureFrameOverlay(
    modifier: Modifier = Modifier,
) {
    // Draw a soft scrim with a square cutout + border, giving a clear framing target.
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    ) {
        drawRect(Color.Black.copy(alpha = 0.35f))

        val pad = 24.dp.toPx()
        val cutoutSize = size.width - (pad * 2)
        val top = (size.height * 0.18f).coerceAtLeast(pad)

        val rect = Rect(
            left = pad,
            top = top,
            right = pad + cutoutSize,
            bottom = top + cutoutSize,
        )

        // Punch out the cutout.
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            blendMode = BlendMode.Clear,
        )

        // Border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.75f),
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun ShutterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = CircleShape,
        color = bg,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        modifier = modifier
            .size(72.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.CameraAlt, contentDescription = "Shutter", tint = fg)
        }
    }
}


private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (CapturedPhoto) -> Unit,
    onError: (String) -> Unit,
) {
    val file = createDurableCoverFile(context)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uri = outputFileResults.savedUri ?: Uri.fromFile(file)
                onSuccess(CapturedPhoto(uri = uri, file = file))
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Failed to capture photo.")
            }
        }
    )
}

private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider {
    val future = ProcessCameraProvider.getInstance(context)
    return suspendCancellableCoroutine { cont ->
        future.addListener(
            {
                try {
                    cont.resume(future.get())
                } catch (t: Throwable) {
                    cont.resumeWithException(t)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}
