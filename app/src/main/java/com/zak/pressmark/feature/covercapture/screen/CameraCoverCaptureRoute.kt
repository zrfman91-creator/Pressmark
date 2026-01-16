package com.zak.pressmark.feature.covercapture.screen

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val errorText = remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission.value = granted
            if (!granted) {
                errorText.value = "Camera permission is required to take a cover photo."
            } else {
                errorText.value = null
            }
        }
    )

    // If we don't have permission, request it once on entry.
    LaunchedEffect(Unit) {
        if (!hasPermission.value) {
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
    LaunchedEffect(hasPermission.value, lifecycleOwner) {
        if (!hasPermission.value) return@LaunchedEffect

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
            errorText.value = null
        } catch (t: Throwable) {
            errorText.value = t.message ?: "Failed to start camera."
        }
    }

    DisposableEffect(hasPermission.value) {
        onDispose {
            if (hasPermission.value) {
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
                )
            },
            floatingActionButton = {
                // Only show shutter when camera is actually available.
                if (hasPermission.value && errorText.value == null) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                takePhoto(
                                    context = context,
                                    imageCapture = imageCapture,
                                    onSuccess = onCaptured,
                                    onError = { msg -> errorText.value = msg },
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Shutter")
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                if (hasPermission.value) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    PermissionGate(
                        message = errorText.value ?: "Camera permission is required.",
                        onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onCancel = onBack,
                    )
                }

                errorText.value?.let { msg ->
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


private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (Uri) -> Unit,
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
                onSuccess(uri)
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
