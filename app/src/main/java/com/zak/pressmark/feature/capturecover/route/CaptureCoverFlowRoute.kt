package com.zak.pressmark.feature.capturecover.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.feature.capturecover.screen.CameraCoverCaptureRoute
import kotlinx.coroutines.launch

/**
 * Feature-owned wrapper that persists the captured cover to the album, then exits.
 *
 * Why this exists:
 * - Keeps app/ as wiring-only (no repository writes in PressmarkNavHost)
 * - Keeps capture flow behavior localized to this feature
 */
@Composable
fun CaptureCoverFlowRoute(
    albumId: String,
    albumRepository: AlbumRepository,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    CameraCoverCaptureRoute(
        modifier = modifier,
        onBack = onBack,
        onCaptured = { uri ->
            scope.launch {
                // Persist best-effort; if storage fails we still exit the flow.
                runCatching { albumRepository.setLocalCover(albumId, uri.toString()) }
                onDone()
            }
        },
    )
}
