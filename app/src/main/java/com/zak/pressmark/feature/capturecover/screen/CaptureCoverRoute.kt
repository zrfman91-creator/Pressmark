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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCoverCaptureRoute(
    onBack: () -> Unit,
    onCaptured: (Uri) -> Unit,
    overlayContent: @Composable (BoxScope.() -> Unit) = {},
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
            errorText = if (!granted) "Camera permission is required to take a cover photo." else null
        }
    )

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            // IMPORTANT: We assume center-crop behavior for correct mapping.
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
                group
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
                runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
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
                val viewWpx = with(density) { maxWidth.toPx() }
                val viewHpx = with(density) { maxHeight.toPx() }

                // Keep overlay padding consistent with CaptureFrameOverlay.
                val framePadPx = with(density) { 24.dp.toPx() }
                val minDimPx = min(viewWpx, viewHpx).coerceAtLeast(1f)
                val cutoutPx = (minDimPx - 2f * framePadPx).coerceAtLeast(minDimPx * 0.4f)
                val frameFraction = (cutoutPx / minDimPx).coerceIn(0.4f, 1f)

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
                                    if (rotation != targetRotation) targetRotation = rotation
                                },
                            )

                            CaptureFrameOverlay(
                                modifier = Modifier.fillMaxSize(),
                                frameFraction = frameFraction,
                            )

                            overlayContent()

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
                                            takePhotoCroppedToOverlay(
                                                context = context,
                                                imageCapture = imageCapture,
                                                viewWpx = viewWpx,
                                                viewHpx = viewHpx,
                                                frameFraction = frameFraction,
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

private data class CapturedPhoto(
    val uri: Uri,
    val file: File,
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

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            blendMode = BlendMode.Clear,
        )

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
 * Captures RAW -> crops to match overlay region as seen in the PreviewView (FILL_CENTER) -> writes OUT file.
 *
 * Why this fixes “thumbnail still full image”:
 * PreviewView with FILL_CENTER shows a center-cropped view of the sensor buffer.
 * If you only “center crop the saved file”, you crop the *full sensor*, not the *preview*.
 * This function maps the overlay rect in VIEW space back into BUFFER space and crops that exact region.
 */
private suspend fun takePhotoCroppedToOverlay(
    context: Context,
    imageCapture: ImageCapture,
    viewWpx: Float,
    viewHpx: Float,
    frameFraction: Float,
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
                    // Heavy work OFF main thread to avoid “not on main thread” / jank.
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        try {
                            cropToOverlayAndWrite(
                                rawFile = rawFile,
                                outFile = outFile,
                                viewWpx = viewWpx,
                                viewHpx = viewHpx,
                                frameFraction = frameFraction,
                            )
                            rawFile.delete()
                            val uri = Uri.fromFile(outFile)
                            // Resume on main thread for safety with UI continuations.
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                if (cont.isActive) cont.resume(uri)
                            }
                        } catch (t: Throwable) {
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                if (cont.isActive) cont.resumeWithException(t)
                            }
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

private fun cropToOverlayAndWrite(
    rawFile: File,
    outFile: File,
    viewWpx: Float,
    viewHpx: Float,
    frameFraction: Float,
) {
    if (!rawFile.exists()) error("Raw file missing")

    val exif = ExifInterface(rawFile.absolutePath)
    val rotationDegrees = exifRotationDegrees(exif)

    val bitmap = decodeDownsampled(rawFile, maxDimPx = 2400) ?: error("Decode failed")

    val rotated = if (rotationDegrees != 0) {
        val m = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true).also {
            if (it != bitmap) bitmap.recycle()
        }
    } else bitmap

    val cropped = cropBitmapToOverlayFillCenter(
        src = rotated,
        viewWpx = viewWpx,
        viewHpx = viewHpx,
        frameFraction = frameFraction,
    )
    if (cropped !== rotated) rotated.recycle()

    FileOutputStream(outFile, false).use { out ->
        cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
        out.flush()
    }
    cropped.recycle()

    // Normalize EXIF orientation on output
    try {
        ExifInterface(outFile.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            saveAttributes()
        }
    } catch (_: Throwable) {
        // ignore
    }
}

/**
 * Map overlay square (in view coords) back into the bitmap coords, using PreviewView.ScaleType.FILL_CENTER math:
 * scale = max(viewW/bufW, viewH/bufH)
 * visible buffer rect is centered in buffer; overlay rect is inside view.
 */
private fun cropBitmapToOverlayFillCenter(
    src: Bitmap,
    viewWpx: Float,
    viewHpx: Float,
    frameFraction: Float,
): Bitmap {
    val bufW = src.width.toFloat()
    val bufH = src.height.toFloat()

    val safeViewW = viewWpx.coerceAtLeast(1f)
    val safeViewH = viewHpx.coerceAtLeast(1f)

    val scale = max(safeViewW / bufW, safeViewH / bufH)

    // Visible buffer size (in buffer pixels) that maps to the view.
    val visibleBufW = safeViewW / scale
    val visibleBufH = safeViewH / scale

    val offsetX = (bufW - visibleBufW) / 2f
    val offsetY = (bufH - visibleBufH) / 2f

    // Overlay square in VIEW coords (centered)
    val cutoutView = min(safeViewW, safeViewH) * frameFraction
    val overlayLeftV = (safeViewW - cutoutView) / 2f
    val overlayTopV = (safeViewH - cutoutView) / 2f

    // Map overlay rect to BUFFER coords
    val leftB = overlayLeftV / scale + offsetX
    val topB = overlayTopV / scale + offsetY
    val sizeB = cutoutView / scale

    // Clamp to bitmap bounds
    var x = leftB.roundToInt().coerceIn(0, src.width - 1)
    var y = topB.roundToInt().coerceIn(0, src.height - 1)

    var s = sizeB.roundToInt().coerceAtLeast(1)
    if (x + s > src.width) s = src.width - x
    if (y + s > src.height) s = src.height - y
    s = s.coerceAtLeast(1)

    return Bitmap.createBitmap(src, x, y, s, s)
}

private fun decodeDownsampled(file: File, maxDimPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    val maxDim = max(bounds.outWidth, bounds.outHeight)
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
    return try {
        val method = ImageCapture.Builder::class.java.getMethod("setCropAspectRatio", Rational::class.java)
        method.invoke(this, crop)
        this
    } catch (_: Throwable) {
        this
    }
}
