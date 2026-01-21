package com.zak.pressmark.feature.artworkpicker.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.model.ReleaseDiscogsCandidate
import com.zak.pressmark.data.repository.ReleaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CoverSearchState(
    val candidates: List<DiscogsPressingCandidateUi> = emptyList(),
    val selectedCandidateId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class DiscogsPressingCandidateUi(
    val candidate: ReleaseDiscogsCandidate,
    val fillLabels: List<String>,
)

data class CoverSearchRequest(
    val releaseId: String,
    val artist: String,
    val title: String,
    val releaseYear: Int?,
    val label: String,
    val catalogNo: String,
    val barcode: String,
)

sealed interface CoverSearchEffect {
    data object Close : CoverSearchEffect
}

class DiscogsCoverSearchViewModel(
    private val releaseRepository: ReleaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoverSearchState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CoverSearchEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    private var currentRequest: CoverSearchRequest? = null

    // Step 3: cancellation + "latest wins".
    private var searchJob: Job? = null
    private var searchSeq: Long = 0L

    fun start(
        releaseId: String,
        artist: String,
        title: String,
        releaseYear: Int?,
        label: String,
        catalogNo: String,
        barcode: String,
    ) {
        val request = CoverSearchRequest(
            releaseId = releaseId,
            artist = artist.trim(),
            title = title.trim(),
            releaseYear = releaseYear,
            label = label.trim(),
            catalogNo = catalogNo.trim(),
            barcode = barcode.trim(),
        )

        val sameRequest = currentRequest == request
        if (sameRequest && (_uiState.value.isLoading || _uiState.value.candidates.isNotEmpty())) return

        currentRequest = request
        search(request)
    }

    private fun search(request: CoverSearchRequest) {
        // Cancel any in-flight search so stale results can't win.
        searchJob?.cancel()

        val mySeq = ++searchSeq

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            // Only the latest search should update state.
            if (mySeq != searchSeq) return@launch

            _uiState.value = CoverSearchState(isLoading = true)

            try {
                val release = releaseRepository.getRelease(request.releaseId)

                val results = releaseRepository.searchDiscogsCandidates(
                    title = request.title,
                    artist = request.artist,
                    releaseYear = request.releaseYear,
                    label = request.label.takeIf { it.isNotBlank() },
                    catalogNo = request.catalogNo.takeIf { it.isNotBlank() },
                    barcode = request.barcode.takeIf { it.isNotBlank() },
                )

                val summaries = results.map { candidate ->
                    DiscogsPressingCandidateUi(
                        candidate = candidate,
                        fillLabels = buildFillLabels(
                            releaseYear = release?.releaseYear,
                            label = release?.label,
                            catalogNo = release?.catalogNo,
                            format = release?.format,
                            barcode = release?.barcode,
                            country = release?.country,
                            releaseType = release?.releaseType,
                            notes = release?.notes,
                            candidate = candidate,
                        ),
                    )
                }

                if (!isActive || mySeq != searchSeq) return@launch

                _uiState.value = CoverSearchState(
                    candidates = summaries,
                    selectedCandidateId = summaries.firstOrNull()?.candidate?.discogsReleaseId,
                    error = if (summaries.isEmpty()) "No results found" else null,
                )
            } catch (t: Throwable) {
                if (!isActive || mySeq != searchSeq) return@launch
                _uiState.value = CoverSearchState(error = t.message ?: "Search failed")
            }
        }
    }

    fun selectCandidate(candidateId: Long?) {
        _uiState.value = _uiState.value.copy(selectedCandidateId = candidateId)
    }

    fun applySelectedCandidate() {
        val request = currentRequest ?: return
        val selected = _uiState.value.candidates
            .firstOrNull { it.candidate.discogsReleaseId == _uiState.value.selectedCandidateId }
            ?.candidate ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                releaseRepository.applyDiscogsCandidateFillMissing(
                    releaseId = request.releaseId,
                    candidate = selected,
                )

                selected.coverUrl?.let { coverUrl ->
                    releaseRepository.setDiscogsCover(
                        releaseId = request.releaseId,
                        coverUrl = coverUrl,
                        discogsReleaseId = selected.discogsReleaseId,
                    )
                }

                _effects.tryEmit(CoverSearchEffect.Close)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    error = t.message ?: "Failed to apply Discogs data",
                )
            }
        }
    }

    private fun buildFillLabels(
        releaseYear: Int?,
        label: String?,
        catalogNo: String?,
        format: String?,
        barcode: String?,
        country: String?,
        releaseType: String?,
        notes: String?,
        candidate: ReleaseDiscogsCandidate,
    ): List<String> = buildList {
        if (releaseYear == null && candidate.year != null) add("Year: ${candidate.year}")
        if (label.isNullOrBlank() && !candidate.label.isNullOrBlank()) add("Label: ${candidate.label}")
        if (catalogNo.isNullOrBlank() && !candidate.catalogNo.isNullOrBlank()) {
            add("Catalog #: ${candidate.catalogNo}")
        }
        if (format.isNullOrBlank() && !candidate.format.isNullOrBlank()) add("Format: ${candidate.format}")
        if (barcode.isNullOrBlank() && !candidate.barcode.isNullOrBlank()) add("Barcode: ${candidate.barcode}")
        if (country.isNullOrBlank() && !candidate.country.isNullOrBlank()) add("Country: ${candidate.country}")
        if (releaseType.isNullOrBlank() && !candidate.releaseType.isNullOrBlank()) {
            add("Release type: ${candidate.releaseType}")
        }
        if (notes.isNullOrBlank() && !candidate.notes.isNullOrBlank()) add("Notes")
    }

}
