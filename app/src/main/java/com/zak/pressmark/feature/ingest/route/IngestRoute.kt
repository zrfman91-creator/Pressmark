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
import com.zak.pressmark.feature.ingest.vm.IngestMethod
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

    LaunchedEffect(Unit) {
        mode = IngestMode.SCAN
        vm.setManualEntryExpanded(false)
        vm.logIngestStart(IngestMethod.BARCODE)
    }

    val reopenScanner: () -> Unit = {
        vm.dismissLookupDialog()
        vm.dismissDiscogsResults()
        vm.clearManualInputs()
        vm.setManualEntryExpanded(false)
        mode = IngestMode.SCAN
    }

    val handleAddMaster: (BarcodeMasterCandidateUi) -> Unit = { candidate ->
        vm.addMasterToLibrary(candidate) { workId, _ ->
            // Always close dialog + reset state.
            vm.dismissLookupDialog()
            vm.clearManualInputs()
            vm.setManualEntryExpanded(false)

            if (state.autoReopenScanner) {
                mode = IngestMode.SCAN
            } else {
                onAddedWork(workId)
            }
        }
    }

    val handleAddDiscogs: (DiscogsCandidateUi) -> Unit = { candidate ->
        vm.addToLibrary(candidate) { workId ->
            // Always close dialogs + reset state.
            vm.dismissDiscogsResults()
            vm.clearManualInputs()
            vm.setManualEntryExpanded(false)

            if (state.autoReopenScanner) {
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
        onLookupBarcode = vm::searchByBarcode,
        onSetAutoReopen = vm::setAutoReopen,

        onClearManualInputs = vm::clearManualInputs,
        onSetManualOverlayExpanded = vm::setManualEntryExpanded,
    )

    // ----------------------------
    // Discogs text/manual lookup UX
    // ----------------------------

    // 1) If user did a text lookup and we have multiple results, show a picker.
    if (state.selectedDiscogsCandidate == null && state.results.isNotEmpty()) {
        DiscogsResultsListDialog(
            candidates = state.results,
            onSelect = vm::selectDiscogsCandidate,
            onDismiss = vm::dismissDiscogsResults,
        )
    }

    // 2) If user selected a Discogs candidate (or got exactly one), show confirm-add dialog.
    state.selectedDiscogsCandidate?.let { candidate ->
        LookupResultsDialog(
            candidate = candidate,
            onDismiss = {
                // Cancel/dismiss should reopen scanner when auto-reopen is enabled; otherwise return to list/input.
                if (state.autoReopenScanner) {
                    reopenScanner()
                } else {
                    vm.dismissDiscogsConfirm()
                }
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
                // Cancel/dismiss should also reopen the scanner (when auto-reopen is enabled).
                if (state.autoReopenScanner) {
                    reopenScanner()
                } else {
                    vm.dismissLookupDialog()
                }
            },
            onConfirmAdd = handleAddMaster,
        )
    }
}
