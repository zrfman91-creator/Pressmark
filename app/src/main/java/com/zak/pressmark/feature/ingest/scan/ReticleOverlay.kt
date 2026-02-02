// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/scan/ReticleOverlay.kt
package com.zak.pressmark.feature.ingest.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Normalized coordinates are in [0..1] relative to the camera frame.
 * (0,0) top-left, (1,1) bottom-right.
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

val DefaultReticle = NormalizedRect(
    left = 0.18f,
    top = 0.40f,
    right = 0.82f,
    bottom = 0.60f,
)

@Composable
fun ReticleOverlay(
    modifier: Modifier = Modifier,
    reticle: NormalizedRect = DefaultReticle,
) {
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    val dim = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val r = Rect(
            left = w * reticle.left,
            top = h * reticle.top,
            right = w * reticle.right,
            bottom = h * reticle.bottom,
        )

        val radiusPx = 14.dp.toPx()
        val round = RoundRect(r, CornerRadius(radiusPx, radiusPx))

        // Dim everything outside the reticle.
        val cutout = Path().apply {
            addRect(Rect(Offset.Zero, size))
            addRoundRect(round)
            fillType = PathFillType.EvenOdd
        }
        drawPath(path = cutout, color = dim, style = Fill)

        // Reticle outline.
        drawRoundRect(
            color = outline,
            topLeft = Offset(r.left, r.top),
            size = androidx.compose.ui.geometry.Size(r.width, r.height),
            cornerRadius = CornerRadius(radiusPx, radiusPx),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
