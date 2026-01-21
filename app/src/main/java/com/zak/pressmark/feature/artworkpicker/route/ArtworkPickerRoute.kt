package com.zak.pressmark.feature.artworkpicker.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.feature.artworkpicker.components.DiscogsPressingPickerDialog
import com.zak.pressmark.feature.artworkpicker.vm.ArtworkPickerViewModelFactory
import com.zak.pressmark.feature.artworkpicker.vm.DiscogsCoverSearchViewModel
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverSearchRoute(
    graph: AppGraph,
    releaseId: String,
    artist: String,
    title: String,
    releaseYearText: String,
    label: String,
    catalogNo: String,
    barcode: String,
    onTakePhoto: () -> Unit,
    onClose: () -> Unit,
) {
    val factory = remember(graph) {
        ArtworkPickerViewModelFactory(
            releaseRepository = graph.releaseRepository,
        )
    }

    val vm: DiscogsCoverSearchViewModel = viewModel(
        key = "cover_search_$releaseId",
        factory = factory,
    )

    val state by vm.uiState.collectAsStateWithLifecycle()

    // VM-owned side effects (close when VM says we're done).
    LaunchedEffect(vm) {
        vm.effects.collect {
            onClose()
        }
    }

    // Kick off / update search when inputs change.
    LaunchedEffect(releaseId, artist, title, releaseYearText, label, catalogNo, barcode) {
        if (releaseId.isNotBlank()) {
            vm.start(
                releaseId = releaseId,
                artist = artist,
                title = title,
                releaseYear = releaseYearText.toIntOrNull(),
                label = label,
                catalogNo = catalogNo,
                barcode = barcode,
            )
        }
    }

    val noResults = !state.isLoading && state.candidates.isEmpty() &&
        (state.error == null || state.error!!.contains("no results", ignoreCase = true))

    val errorMessage = state.error?.takeIf { !noResults }

    when {
        state.isLoading -> {
            CoverSearchFeedbackDialog(
                title = "Searching covers",
                message = "Looking up results for '$title'.",
                isLoading = true,
                primaryAction = null,
                onPrimary = null,
                onTakePhoto = onTakePhoto,
                onSkip = onClose,
                onDismiss = onClose,
            )
        }

        errorMessage != null -> {
            CoverSearchFeedbackDialog(
                title = "Couldn't load covers",
                message = errorMessage,
                isLoading = false,
                primaryAction = "Retry",
                onPrimary = {
                    vm.start(
                        releaseId = releaseId,
                        artist = artist,
                        title = title,
                        releaseYear = releaseYearText.toIntOrNull(),
                        label = label,
                        catalogNo = catalogNo,
                        barcode = barcode,
                    )
                },
                onTakePhoto = onTakePhoto,
                onSkip = onClose,
                onDismiss = onClose,
            )
        }

        noResults -> {
            CoverSearchFeedbackDialog(
                title = "No results",
                message = "No covers found for '$title'.",
                isLoading = false,
                primaryAction = "Retry",
                onPrimary = {
                    vm.start(
                        releaseId = releaseId,
                        artist = artist,
                        title = title,
                        releaseYear = releaseYearText.toIntOrNull(),
                        label = label,
                        catalogNo = catalogNo,
                        barcode = barcode,
                    )
                },
                onTakePhoto = onTakePhoto,
                onSkip = onClose,
                onDismiss = onClose,
            )
        }

        else -> {
            DiscogsPressingPickerDialog(
                artist = artist,
                title = title,
                candidates = state.candidates,
                selectedId = state.selectedCandidateId,
                onSelectCandidate = vm::selectCandidate,
                onConfirm = vm::applySelectedCandidate,
                onSkip = onClose,
                onTakePhoto = onTakePhoto,
                onDismiss = onClose,
            )
        }
    }
}

@Composable
private fun CoverSearchFeedbackDialog(
    title: String,
    message: String,
    isLoading: Boolean,
    primaryAction: String?,
    onPrimary: (() -> Unit)?,
    onTakePhoto: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (isLoading) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onTakePhoto) {
                        Text("Take Photo")
                    }

                    OutlinedButton(onClick = onSkip) {
                        Text("Skip")
                    }

                    if (primaryAction != null && onPrimary != null) {
                        Button(onClick = onPrimary) {
                            Text(primaryAction)
                        }
                    }
                }
            }
        }
    }
}
