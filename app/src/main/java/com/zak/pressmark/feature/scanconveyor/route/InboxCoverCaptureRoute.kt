package com.zak.pressmark.feature.scanconveyor.route

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.core.ocr.TextExtractor
import com.zak.pressmark.data.repository.DevSettingsRepository
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.work.InboxPipelineScheduler
import com.zak.pressmark.feature.capturecover.screen.CameraCoverCaptureRoute
import com.zak.pressmark.feature.scanconveyor.vm.InboxCoverCaptureEffect
import com.zak.pressmark.feature.scanconveyor.vm.InboxCoverCaptureViewModel
import com.zak.pressmark.feature.scanconveyor.vm.InboxCoverCaptureViewModelFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Composable
fun InboxCoverCaptureRoute(
    inboxRepository: InboxRepository,
    textExtractor: TextExtractor,
    devSettingsRepository: DevSettingsRepository,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val factory = remember(inboxRepository, textExtractor) {
        InboxCoverCaptureViewModelFactory(inboxRepository, textExtractor)
    }
    val vm: InboxCoverCaptureViewModel = viewModel(factory = factory)
    val uiState = vm.uiState.collectAsStateWithLifecycle().value
    val showOverlay = devSettingsRepository
        .observeOcrDebugOverlayEnabled()
        .collectAsStateWithLifecycle(initialValue = false)
        .value
    val logCandidates = devSettingsRepository
        .observeOcrLogCandidatesEnabled()
        .collectAsStateWithLifecycle(initialValue = false)
        .value

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                is InboxCoverCaptureEffect.Done -> {
                    InboxPipelineScheduler.enqueueOcrDrain(context)
                    Toast.makeText(context, "Saved to Inbox", Toast.LENGTH_SHORT).show()
                    onDone()
                }
            }
        }
    }

    CameraCoverCaptureRoute(
        modifier = modifier,
        onBack = onBack,
        onCaptured = { uri ->
            vm.saveCover(uri, logCandidates)
        },
        overlayContent = {
            if (showOverlay) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .widthIn(max = 280.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "OCR session: ${uiState.elapsedMs}ms • stable ${uiState.stableCount}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (uiState.lastLines.isNotEmpty()) {
                        Text(
                            text = uiState.lastLines.take(3).joinToString(" / "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    uiState.bestFields?.let { fields ->
                        val title = fields.title ?: "—"
                        val artist = fields.artist ?: "—"
                        Text(
                            text = "Best: $artist • $title",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    uiState.confidenceScore?.let { score ->
                        Text(
                            text = "Confidence $score",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    if (uiState.reasonCodes.isNotEmpty()) {
                        Text(
                            text = uiState.reasonCodes.take(3).joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        },
    )
}
