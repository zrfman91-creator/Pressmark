// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/manual/vm/AddWorkViewModel.kt
package com.zak.pressmark.feature.ingest.manual.vm

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.core.analytics.UxEventLogger
import com.zak.pressmark.core.util.completion.CompletionRules
import com.zak.pressmark.core.util.ocr.OcrHint
import com.zak.pressmark.core.util.ocr.OcrService
import com.zak.pressmark.data.remote.discogs.DiscogsClient
import com.zak.pressmark.data.repository.v2.CanonicalWorkRepositoryV2
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.IOException

data class DiscogsCandidateUi(
    val masterId: Long,
    val displayTitle: String,
    val subtitle: String?,
    val year: Int?,
    val thumbUrl: String?,
    val coverUrl: String?,
    val genres: List<String>,
    val styles: List<String>,
)

data class AddWorkUiState(
    val artist: String = "",
    val title: String = "",
    val year: String = "",
    val evidenceUris: List<String> = emptyList(),
    val ocrTitleCandidates: List<String> = emptyList(),
    val ocrArtistCandidates: List<String> = emptyList(),
    val isOcrProcessing: Boolean = false,
    val ocrMessage: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val results: List<DiscogsCandidateUi> = emptyList(),
)

@HiltViewModel
class AddWorkViewModel @Inject constructor(
    private val discogsClient: DiscogsClient,
    private val workRepositoryV2: WorkRepositoryV2,
    private val canonicalWorkRepositoryV2: CanonicalWorkRepositoryV2,
    private val ocrService: OcrService,
    private val uxEventLogger: UxEventLogger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddWorkUiState())
    val uiState = _uiState.asStateFlow()

    fun onArtistChanged(value: String) {
        _uiState.value = _uiState.value.copy(artist = value, errorMessage = null, infoMessage = null)
    }

    fun onTitleChanged(value: String) {
        _uiState.value = _uiState.value.copy(title = value, errorMessage = null, infoMessage = null)
    }

    fun onYearChanged(value: String) {
        val cleaned = value.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(year = cleaned, errorMessage = null, infoMessage = null)
    }

    fun onOcrImageCaptured(uri: Uri, source: OcrCaptureSource) {
        val state = _uiState.value
        _uiState.value = state.copy(
            isOcrProcessing = true,
            ocrMessage = "Processing ${source.label}...",
            errorMessage = null,
            infoMessage = null,
        )

        viewModelScope.launch(Dispatchers.IO) {
            val hint = OcrHint(
                fallbackTitle = state.title.takeIf { it.isNotBlank() },
                fallbackArtist = state.artist.takeIf { it.isNotBlank() },
            )

            val result = ocrService.extractAnchors(uri, hint)
            val updated = result.fold(
                onSuccess = { anchors ->
                    val titleCandidates = anchors.titleCandidates
                    val artistCandidates = anchors.artistCandidates

                    val newTitle = if (state.title.isBlank()) {
                        titleCandidates.firstOrNull().orEmpty()
                    } else {
                        state.title
                    }
                    val newArtist = if (state.artist.isBlank()) {
                        artistCandidates.firstOrNull().orEmpty()
                    } else {
                        state.artist
                    }

                    state.copy(
                        title = newTitle,
                        artist = newArtist,
                        evidenceUris = state.evidenceUris + uri.toString(),
                        ocrTitleCandidates = titleCandidates,
                        ocrArtistCandidates = artistCandidates,
                        isOcrProcessing = false,
                        ocrMessage = "OCR complete.",
                    )
                },
                onFailure = { error ->
                    state.copy(
                        isOcrProcessing = false,
                        ocrMessage = error.message ?: "OCR failed.",
                    )
                },
            )
            _uiState.value = updated
        }
    }

    fun applyTitleCandidate(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun applyArtistCandidate(value: String) {
        _uiState.value = _uiState.value.copy(artist = value)
    }

    fun searchDiscogs() {
        if (BuildConfig.DISCOGS_TOKEN.isBlank()) {
            Log.w("AddWorkViewModel", "Discogs token missing; returning user-safe error.")
            _uiState.value = _uiState.value.copy(
                errorMessage = "Service error. Try again.",
                results = emptyList(),
            )
            return
        }

        val artist = _uiState.value.artist.trim()
        val title = _uiState.value.title.trim()
        val year = _uiState.value.year.toIntOrNull()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null,
                results = emptyList(),
            )

            try {
                val candidates = discogsClient.searchMasters(
                    artist = artist,
                    title = title,
                    year = year,
                    limit = 10,
                )

                if (candidates.isEmpty()) {
                    uxEventLogger.logEvent(
                        "pm_discogs_lookup_result",
                        mapOf("result" to "empty", "http_code" to 200),
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No match found. Try manual entry.",
                        results = emptyList(),
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = candidates.map { c ->
                        DiscogsCandidateUi(
                            masterId = c.masterId,
                            displayTitle = c.displayTitle,
                            subtitle = c.subtitle,
                            year = c.year,
                            thumbUrl = c.thumbUrl,
                            coverUrl = c.coverUrl,
                            genres = c.genres,
                            styles = c.styles,
                        )
                    },
                )
                uxEventLogger.logEvent(
                    "pm_discogs_lookup_result",
                    mapOf("result" to "success", "http_code" to 200),
                )
            } catch (t: Throwable) {
                logDiscogsLookupFailure(t)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapUserSafeError(t),
                    results = emptyList(),
                )
            }
        }
    }

    /**
     * MASTER-ONLY ingest (Discogs-only phase):
     * - Persist Work anchored by discogsMasterId
     * - Dedupe by discogsMasterId
     */
    fun addToLibrary(
        candidate: DiscogsCandidateUi,
        onAdded: (String) -> Unit,
    ) {
        val (artist, title) = parseArtistTitle(candidate.displayTitle)
        val year = candidate.year
        val releaseType = CompletionRules.inferReleaseType(candidate.styles, candidate.genres)
        val formatType = CompletionRules.inferFormatType(title, candidate.styles, candidate.genres)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            try {
                val result = workRepositoryV2.upsertDiscogsMasterWork(
                    discogsMasterId = candidate.masterId,
                    title = title,
                    artistLine = artist,
                    year = year,
                    primaryArtworkUri = candidate.coverUrl ?: candidate.thumbUrl,
                    genres = candidate.genres,
                    styles = candidate.styles,
                )

                val info = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> "Added to library."
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> "Already in library — updated details."
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> "Possible duplicate — added anyway."
                }

                val dedupe = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> "created"
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> "updated"
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> "duplicate"
                }
                uxEventLogger.logEvent(
                    "pm_work_add_success",
                    mapOf("method" to "discogs", "dedupe" to dedupe),
                )

                val workId = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> result.workId
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> result.workId
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> result.existingWorkId.orEmpty()
                }

                if (workId.isNotBlank()) {
                    val canonicalWorkId = canonicalWorkRepositoryV2.upsertCanonicalWorkFromDiscogs(
                        artistName = artist,
                        discogsMasterId = candidate.masterId,
                        title = title,
                        year = year,
                        formatType = formatType,
                        releaseType = releaseType,
                    )
                    workRepositoryV2.updateCanonicalWorkId(workId, canonicalWorkId)
                }

                _uiState.value = _uiState.value.copy(isLoading = false, infoMessage = info)
                if (workId.isNotBlank()) {
                    onAdded(workId)
                }
            } catch (t: Throwable) {
                Log.e("AddWorkViewModel", "Failed to add Discogs work.", t)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapUserSafeError(t),
                )
            }
        }
    }

    fun addManualWork(onAdded: (String) -> Unit) {
        val artist = _uiState.value.artist.trim()
        val title = _uiState.value.title.trim()
        val year = _uiState.value.year.toIntOrNull()

        if (artist.isBlank() || title.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Artist and title are required.",
                infoMessage = null,
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            try {
                val result = workRepositoryV2.upsertManualWork(
                    title = title,
                    artistLine = artist,
                    year = year,
                )

                val info = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> "Added to library."
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> "Already in library — updated details."
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> result.reason
                }

                val dedupe = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> "created"
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> "updated"
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> "duplicate"
                }
                uxEventLogger.logEvent(
                    "pm_work_add_success",
                    mapOf("method" to "manual", "dedupe" to dedupe),
                )

                val workId = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> result.workId
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> result.workId
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> result.existingWorkId.orEmpty()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    infoMessage = info,
                    results = emptyList(),
                )
                if (workId.isNotBlank()) {
                    onAdded(workId)
                }
            } catch (t: Throwable) {
                Log.e("AddWorkViewModel", "Failed to add manual work.", t)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapUserSafeError(t),
                )
            }
        }
    }

    private fun parseArtistTitle(discogsTitle: String): Pair<String, String> {
        val parts = discogsTitle.split(" - ", limit = 2)
        return if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else {
            _uiState.value.artist.trim() to _uiState.value.title.trim()
        }
    }

    fun logIngestStart() {
        uxEventLogger.logEvent("pm_ingest_start", mapOf("method" to "manual"))
    }

    private fun mapUserSafeError(t: Throwable): String {
        return when (t) {
            is IOException -> "No connection. Try again."
            is retrofit2.HttpException -> "Service error. Try again."
            else -> "Service error. Try again."
        }
    }

    private fun logDiscogsLookupFailure(t: Throwable) {
        when (t) {
            is retrofit2.HttpException -> {
                uxEventLogger.logEvent(
                    "pm_discogs_lookup_result",
                    mapOf("result" to "http_error", "http_code" to t.code()),
                )
            }
            is IOException -> {
                uxEventLogger.logEvent(
                    "pm_discogs_lookup_result",
                    mapOf("result" to "network_error", "http_code" to null),
                )
            }
            else -> {
                uxEventLogger.logEvent(
                    "pm_discogs_lookup_result",
                    mapOf("result" to "unknown_error", "http_code" to null),
                )
            }
        }
    }
}

enum class OcrCaptureSource(val label: String) {
    COVER("cover"),
    LABEL("label"),
}
