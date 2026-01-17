package com.zak.pressmark.feature.capturecover.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Rational
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

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

    val crop = remember { Rational(1, 1) }
    var targetRotation by remember {
        mutableIntStateOf(previewView.display?.rotation ?: Surface.ROTATION_0)
    }

    val imageCapture = remember(targetRotation) {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(targetRotation)
            .setCropAspectRatioCompat(crop)
            .build()
    }

    // Bind/unbind camera only when permission is granted.
    LaunchedEffect(hasPermission, captured, lifecycleOwner, targetRotation, imageCapture) {
        if (!hasPermission || captured != null) {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            return@LaunchedEffect
        }

        val provider = awaitCameraProvider(context)

        val preview = Preview.Builder().build().also { p ->
            p.surfaceProvider = previewView.surfaceProvider
        }

        val viewPort = ViewPort.Builder(crop, targetRotation)
            .setScaleType(ViewPort.FILL_CENTER)
            .build()

        val group = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageCapture)
            .setViewPort(viewPort)
            .build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                group,
            )
            errorText = null
        } catch (t: Throwable) {
            errorText = t.message ?: "Failed to start camera."
        }
    }

    LaunchedEffect(flashOn, imageCapture) {
        imageCapture.flashMode =
            if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                val density = LocalDensity.current
                val minDimPx = with(density) { min(maxWidth, maxHeight).toPx() }
                val framePaddingPx = with(density) { 24.dp.toPx() }
                val frameFraction = remember(minDimPx, framePaddingPx) {
                    if (minDimPx <= 0f) {
                        1f
                    } else {
                        ((minDimPx - framePaddingPx * 2f) / minDimPx).coerceIn(0.4f, 1f)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        !hasPermission -> {
                            PermissionGate(
                                message = errorText ?: "Camera permission is required.",
                                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                onCancel = onBack,
                            )
                        }

                        captured != null -> {
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
                        }

                        else -> {
                            AndroidView(
                                factory = { previewView },
                                modifier = Modifier.fillMaxSize(),
                                update = { view ->
                                    val rotation = view.display?.rotation ?: Surface.ROTATION_0
                                    if (rotation != targetRotation) {
                                        targetRotation = rotation
                                    }
                                },
                            )

                            CaptureFrameOverlay(
                                modifier = Modifier.fillMaxSize(),
                                frameFraction = frameFraction,
                            )

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
                                        runCatching {
                                            takePhotoCropped(
                                                context = context,
                                                imageCapture = imageCapture,
                                                cropFraction = frameFraction,
                                            )
                                        }.onSuccess { result ->
                                            isCapturing = false
                                            captured = result
                                        }.onFailure { t ->
                                            isCapturing = false
                                            errorText = t.message ?: "Failed to capture photo."
                                        }
                                    }
                                }
                            )
                        }
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
        val ctx = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(ctx)
                .data(captured.file) // <- use file directly
                .memoryCacheKey("cover_${captured.file?.lastModified()}")
                .diskCacheKey("cover_${captured.file?.lastModified()}")
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
    frameFraction: Float = 1f,
) {
    // Draw a soft scrim with a square cutout + border, giving a clear framing target.
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    ) {
        drawRect(Color.Black.copy(alpha = 0.35f))

        val cutoutSize = min(size.width, size.height) * frameFraction
        val left = (size.width - cutoutSize) / 2f
        val top = (size.height - cutoutSize) / 2f
        val rect = Rect(
            left = left,
            top = top,
            right = left + cutoutSize,
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
    val bg =
        if (enabled) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    val fg =
        if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

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

/**
 * CameraX's viewport/crop controls often affect **preview** reliably, but many devices/versions still
 * save the full sensor image to disk.
 *
 * To guarantee what the user sees is what they get, we:
 * 1) capture to a file (fast path, no YUV conversion)
 * 2) read EXIF orientation
 * 3) decode (downsampled), rotate, center-crop to a square
 * 4) overwrite the original file with the cropped image
 */
private suspend fun takePhotoCropped(
    context: Context,
    imageCapture: ImageCapture,
    cropFraction: Float,
): CapturedPhoto {
    val dir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
    val base = System.currentTimeMillis()
    val rawFile = File(dir, "cover_raw_$base.jpg")
    val outFile = File(dir, "cover_$base.jpg")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(rawFile).build()

    val finalUri = suspendCancellableCoroutine<Uri> { cont ->
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (!cont.isActive) return@launch
                        try {
                            cropSquareToNewFile(
                                rawFile = rawFile,
                                outFile = outFile,
                                cropFraction = cropFraction,
                            )
                            rawFile.delete()

                            if (cont.isActive) cont.resume(Uri.fromFile(outFile))
                        } catch (t: Throwable) {
                            if (cont.isActive) cont.resumeWithException(t)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            }
        )
    }

    return CapturedPhoto(uri = finalUri, file = outFile)
}

private fun decodeDownsampled(file: File, maxDimPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
    while (maxDim / sample > maxDimPx) sample *= 2

    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeFile(file.absolutePath, opts)
}

private fun exifRotationDegrees(exif: ExifInterface): Int {
    return when (
        exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
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

@Suppress("SwallowedException")
private fun ImageCapture.Builder.setCropAspectRatioCompat(crop: Rational): ImageCapture.Builder {
    // CameraX's setCropAspectRatio has been hidden/changed across versions; reflection keeps us resilient.
    return try {
        val method = ImageCapture.Builder::class.java.getMethod("setCropAspectRatio", Rational::class.java)
        method.invoke(this, crop)
        this
    } catch (_: Throwable) {
        this
    }
}
private fun cropSquareToNewFile(rawFile: File, outFile: File, cropFraction: Float) {
    if (!rawFile.exists()) error("Raw file missing")

    val exif = ExifInterface(rawFile.absolutePath)
    val rotationDegrees = exifRotationDegrees(exif)

    val bitmap = decodeDownsampled(rawFile, maxDimPx = 2048) ?: error("Decode failed")

    val rotated = if (rotationDegrees != 0) {
        val m = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true).also {
            if (it != bitmap) bitmap.recycle()
        }
    } else bitmap

    val minSide = minOf(rotated.width, rotated.height)
    val side = (minSide * cropFraction).toInt().coerceIn(1, minSide)
    val left = (rotated.width - side) / 2
    val top = (rotated.height - side) / 2
    val cropped = Bitmap.createBitmap(rotated, left, top, side, side)
    if (cropped != rotated) rotated.recycle()

    FileOutputStream(outFile, false).use { out ->
        cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
        out.flush()
    }
    cropped.recycle()

    // normalize EXIF on output
    try {
        ExifInterface(outFile.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            saveAttributes()
        }
    } catch (_: Throwable) {
        // ignore
    }
}
