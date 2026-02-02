// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/scan/BarcodeAnalyzer.kt
package com.zak.pressmark.feature.ingest.scan

import android.annotation.SuppressLint
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * ML Kit barcode analyzer with "commit gating".
 *
 * This fixes two common scanner UX issues:
 * 1) Instant-fire on first detection (jarring): require stability for N frames.
 * 2) Accidental triggers from the edges: optionally require the barcode to be within the reticle.
 *
 * Notes:
 * - This analyzer *does not* own navigation or network calls; it only emits a committed barcode string.
 * - Use [reset] when returning to scan mode to re-arm quickly.
 */
class MlKitBarcodeAnalyzer(
    private val isEnabled: () -> Boolean = { true },

    // Gate tuning
    private val warmupMs: Long = 900L,
    private val requiredConsecutiveFrames: Int = 30,
    private val cooldownMs: Long = 1500L,

    // Reticle gating
    private val requireInReticle: Boolean = true,
    private val reticle: NormalizedRect = DefaultReticle,

    private val onBarcodeDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    private var lastValue: String? = null
    private var consecutive = 0
    private var lastTriggerAtMs: Long = 0L
    private var armedAtMs: Long = SystemClock.elapsedRealtime() + warmupMs

    fun reset() {
        lastValue = null
        consecutive = 0
        lastTriggerAtMs = 0L
        armedAtMs = SystemClock.elapsedRealtime() + warmupMs
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (!isEnabled()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val candidate = pickBestBarcode(barcodes) ?: return@addOnSuccessListener

                val raw = candidate.rawValue?.trim().orEmpty()
                if (raw.isBlank()) return@addOnSuccessListener

                if (requireInReticle) {
                    val box = candidate.boundingBox ?: return@addOnSuccessListener
                    val (frameW, frameH) = rotatedFrameSize(imageProxy.width, imageProxy.height, rotation)
                    val cx = box.exactCenterX() / frameW.toFloat()
                    val cy = box.exactCenterY() / frameH.toFloat()
                    if (!reticle.contains(cx, cy)) {
                        // Not centered in the reticle: reset streak so user must stabilize inside.
                        lastValue = null
                        consecutive = 0
                        return@addOnSuccessListener
                    }
                }

                val digitsOnly = raw.filter(Char::isDigit).ifBlank { raw }
                if (shouldCommit(digitsOnly)) {
                    onBarcodeDetected(digitsOnly)
                }
            }
            .addOnFailureListener {
                // ignore; user can keep scanning
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun shouldCommit(value: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now < armedAtMs) return false
        // Cooldown: prevent double-triggers when the barcode remains in view.
        if (now - lastTriggerAtMs < cooldownMs) return false

        if (value == lastValue) {
            consecutive++
        } else {
            lastValue = value
            consecutive = 1
        }

        if (consecutive >= requiredConsecutiveFrames) {
            lastTriggerAtMs = now
            lastValue = null
            consecutive = 0
            return true
        }
        return false
    }

    private fun pickBestBarcode(barcodes: List<Barcode>): Barcode? {
        if (barcodes.isEmpty()) return null
        // Prefer EAN/UPC formats when present; otherwise fall back to first.
        val preferred = barcodes.firstOrNull {
            when (it.format) {
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E -> true
                else -> false
            }
        }
        return preferred ?: barcodes.first()
    }

    private fun rotatedFrameSize(w: Int, h: Int, rotationDegrees: Int): Pair<Int, Int> {
        return if (rotationDegrees % 180 == 0) {
            w to h
        } else {
            h to w
        }
    }
}
