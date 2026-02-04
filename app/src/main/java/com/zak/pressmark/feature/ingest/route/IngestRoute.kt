// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/route/IngestRoute.kt
package com.zak.pressmark.feature.ingest.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.ingest.screen.DiscogsResultsListDialog
import com.zak.pressmark.feature.ingest.screen.IngestMode
import com.zak.pressmark.feature.ingest.screen.IngestScreen
import com.zak.pressmark.feature.ingest.screen.LookupResultsDialog
import com.zak.pressmark.feature.ingest.vm.BarcodeMasterCandidateUi
import com.zak.pressmark.feature.ingest.vm.DiscogsCandidateUi
import com.zak.pressmark.feature.ingest.vm.IngestViewModel

@Composable
fun IngestRoute(
    onBack: () -> Unit,
    onAddedWork: (workId: String) -> Unit,
    // Kept for call-site compatibility, but we force SCAN on entry per spec.
    initialMode: IngestMode = IngestMode.SCAN,
) {
    val vm: IngestViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    // Spec: entering Add flow always starts in SCAN.
    var mode by remember { mutableStateOf(IngestMode.SCAN) }

    // Local-only selection for “pick one result → confirm add”
    var selectedDiscogsCandidate by remember { mutableStateOf<DiscogsCandidateUi?>(null) }

    LaunchedEffect(Unit) {
        mode = IngestMode.SCAN
        vm.onManualEntryExpandedChanged(false)
        selectedDiscogsCandidate = null
    }

    val reopenScanner: () -> Unit = {
        selectedDiscogsCandidate = null
        vm.resetTransientState()
        mode = IngestMode.SCAN
    }

    val handleAddMaster: (BarcodeMasterCandidateUi) -> Unit = { candidate ->
        vm.addMasterToLibrary(candidate) { workId, autoReopen ->
            vm.clearBarcodeCandidate()
            if (autoReopen) {
                mode = IngestMode.SCAN
            } else {
                onAddedWork(workId)
            }
        }
    }

    val handleAddDiscogs: (DiscogsCandidateUi) -> Unit = { candidate ->
        vm.addDiscogsCandidateToLibrary(candidate) { workId, autoReopen ->
            selectedDiscogsCandidate = null
            if (autoReopen) {
                mode = IngestMode.SCAN
            } else {
                onAddedWork(workId)
            }
        }
    }

    IngestScreen(
        state = state,
        mode = mode,
        onModeChange = { mode = it },

        onBack = onBack,

        onBarcodeChanged = vm::onBarcodeChanged,
        onLookupBarcode = { vm.lookupBarcode(state.barcode) },

        // If your screen exposes this toggle, wire it properly later via ScannerPreferences.
        // For now, avoid calling stale VM APIs.
        onSetAutoReopen = { /* preference is driven by ScannerPreferences flow */ },

        onClearManualInputs = vm::clearManualInputs,
        onSetManualOverlayExpanded = vm::onManualEntryExpandedChanged,
    )

    // ----------------------------
    // Discogs text/manual lookup UX
    // ----------------------------

    if (selectedDiscogsCandidate == null && state.results.isNotEmpty()) {
        DiscogsResultsListDialog(
            candidates = state.results,
            onSelect = { selectedDiscogsCandidate = it },
            onDismiss = {
                if (state.autoReopenScanner) reopenScanner() else vm.clearManualInputs()
            },
        )
    }

    selectedDiscogsCandidate?.let { candidate ->
        LookupResultsDialog(
            candidate = candidate,
            onDismiss = {
                selectedDiscogsCandidate = null
                if (state.autoReopenScanner) reopenScanner()
            },
            onConfirmAdd = handleAddDiscogs,
        )
    }

    // ----------------------------
    // Barcode lookup UX
    // ----------------------------

    state.masterCandidate?.let { candidate ->
        LookupResultsDialog(
            candidate = candidate,
            onDismiss = {
                if (state.autoReopenScanner) reopenScanner() else vm.clearBarcodeCandidate()
            },
            onConfirmAdd = handleAddMaster,
        )
    }
}
