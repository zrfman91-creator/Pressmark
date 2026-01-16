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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.core.artwork.ArtworkCandidate
import com.zak.pressmark.core.artwork.ArtworkPickerDialog
import com.zak.pressmark.core.artwork.ArtworkProviderId
import com.zak.pressmark.data.remote.discogs.DiscogsAutofillCandidate
import com.zak.pressmark.data.remote.discogs.toAutofillCandidate
import com.zak.pressmark.feature.artworkpicker.ArtworkPickerViewModelFactory
import com.zak.pressmark.feature.artworkpicker.DiscogsCoverSearchViewModel
import com.zak.pressmark.feature.artworkpicker.components.DiscogsConfirmDetailsSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverSearchRoute(
    graph: AppGraph,
    albumId: String,
    artist: String,
    title: String,
    shouldPromptAutofill: Boolean,
    onTakePhoto: () -> Unit,
    onClose: () -> Unit,
) {
    val factory = remember(graph) {
        ArtworkPickerViewModelFactory(
            albumRepository = graph.albumRepository,
            discogsApi = graph.discogsApiService,
        )
    }

    val vm: DiscogsCoverSearchViewModel = viewModel(
        key = "cover_search_$albumId",
        factory = factory,
    )

    val state by vm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Kick off / update search when inputs change
    LaunchedEffect(albumId, artist, title) {
        if (albumId.isNotBlank()) {
            vm.start(
                albumId = albumId,
                artist = artist,
                title = title,
            )
        }
    }

    val discogsResults = state.results
    val discogsById = remember(discogsResults) {
        discogsResults.associateBy { it.id.toString() }
    }

    val candidates: List<ArtworkCandidate> = remember(discogsResults) {
        discogsResults.map { r ->
            ArtworkCandidate(
                provider = ArtworkProviderId.DISCOGS,
                providerItemId = r.id.toString(),
                imageUrl = r.coverImage ?: r.thumb,
                thumbUrl = r.thumb,
                displayTitle = r.title.toString(),
                displayArtist = null,
                subtitle = null,
            )
        }
    }

    // Reflection-free autofill mapping lives in data/remote/discogs.

    fun computeWillFillLabels(
        existingReleaseYear: Int?,
        existingCatalogNo: String?,
        existingLabel: String?,
        existingFormat: String?,
        candidate: DiscogsAutofillCandidate,
    ): List<String> = buildList {
        if (existingReleaseYear == null && candidate.releaseYear != null) add("Year: ${candidate.releaseYear}")
        if (existingCatalogNo.isNullOrBlank() && !candidate.catalogNo.isNullOrBlank()) add("Catalog #: ${candidate.catalogNo}")
        if (existingLabel.isNullOrBlank() && !candidate.label.isNullOrBlank()) add("Label: ${candidate.label}")
        if (existingFormat.isNullOrBlank() && !candidate.format.isNullOrBlank()) add("Format: ${candidate.format}")
    }

    var pendingCandidate by remember { mutableStateOf<DiscogsAutofillCandidate?>(null) }
    var willFillLabels by remember { mutableStateOf<List<String>>(emptyList()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun closeAndReset() {
        pendingCandidate = null
        willFillLabels = emptyList()
        onClose()
    }

    if (pendingCandidate != null) {
        DiscogsConfirmDetailsSheet(
            sheetState = sheetState,
            willFillLabels = willFillLabels,
            onUseDiscogsFillMissing = {
                val c = pendingCandidate ?: return@DiscogsConfirmDetailsSheet
                scope.launch {
                    graph.albumRepository.fillMissingFields(
                        albumId = albumId,
                        releaseYear = c.releaseYear,
                        catalogNo = c.catalogNo,
                        label = c.label,
                        format = c.format,
                    )
                    closeAndReset()
                }
            },
            onKeepMyEntry = { closeAndReset() },
            onDismiss = { closeAndReset() },
        )
    }

    val noResults = !state.isLoading && candidates.isEmpty() &&
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
                        albumId = albumId,
                        artist = artist,
                        title = title,
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
                        albumId = albumId,
                        artist = artist,
                        title = title,
                    )
                },
                onTakePhoto = onTakePhoto,
                onSkip = onClose,
                onDismiss = onClose,
            )
        }

        else -> {
            ArtworkPickerDialog(
                artist = artist,
                title = title,
                results = candidates,
                onPick = { candidate ->
                    discogsById[candidate.providerItemId]?.let { picked ->
                        // Always set the cover selection first.
                        vm.pickResult(picked)

                        // Batch E: only prompt in Save & Exit flow (list success origin).
                        if (!shouldPromptAutofill) {
                            onClose()
                            return@ArtworkPickerDialog
                        }

                        scope.launch {
                            val album = graph.albumRepository.getById(albumId)
                            if (album == null) {
                                onClose()
                                return@launch
                            }

                            val extracted = picked.toAutofillCandidate()
                            val labels = computeWillFillLabels(
                                existingReleaseYear = album.releaseYear,
                                existingCatalogNo = album.catalogNo,
                                existingLabel = album.label,
                                existingFormat = album.format,
                                candidate = extracted,
                            )

                            if (labels.isEmpty()) {
                                onClose()
                            } else {
                                pendingCandidate = extracted
                                willFillLabels = labels
                            }
                        }
                    } ?: onClose()
                },
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
