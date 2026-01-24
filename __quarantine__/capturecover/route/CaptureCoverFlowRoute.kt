package com.zak.pressmark.feature.capturecover.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zak.pressmark.data.repository.v1.ReleaseRepository
import com.zak.pressmark.feature.capturecover.screen.CameraCoverCaptureRoute
import com.zak.pressmark.feature.capturecover.vm.CaptureCoverEffect
import com.zak.pressmark.feature.capturecover.vm.CaptureCoverFlowViewModel
import com.zak.pressmark.feature.capturecover.vm.CaptureCoverFlowViewModelFactory

/**
 * Feature-owned wrapper that persists the captured cover to the album, then exits.
 *
 * Why this exists:
 * - Keeps app/ as wiring-only (no repository writes in PressmarkNavHost)
 * - Keeps capture flow behavior localized to this feature
 */
@Composable
fun CaptureCoverFlowRoute(
    releaseId: String,
    releaseRepository: ReleaseRepository,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val factory = remember(releaseRepository) {
        CaptureCoverFlowViewModelFactory(releaseRepository)
    }

    val vm: CaptureCoverFlowViewModel = viewModel(
        key = "capture_cover_flow_$releaseId",
        factory = factory,
    )

    // VM-owned side effects (exit flow when VM says we're done).
    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                is CaptureCoverEffect.Done -> onDone()
            }
        }
    }

    CameraCoverCaptureRoute(
        modifier = modifier,
        onBack = onBack,
        onCaptured = { uri ->
            // Best-effort: persist and exit.
            vm.saveCover(releaseId = releaseId, coverUri = uri.toString())
        },
    )
}
