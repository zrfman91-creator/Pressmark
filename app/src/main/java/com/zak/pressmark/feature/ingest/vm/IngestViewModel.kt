package com.zak.pressmark.feature.ingest.vm

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.core.analytics.UxEventLogger
import com.zak.pressmark.core.util.ocr.OcrHint
import com.zak.pressmark.core.util.ocr.OcrService
import com.zak.pressmark.data.prefs.ScannerPreferences
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.remote.discogs.DiscogsClient
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Consolidated ingest VM:
 * - Barcode -> release search -> release fetch -> master search -> single master candidate
 * - Manual text -> master search -> list of candidates
 * - Manual add
 * - OCR anchors -> populate artist/title candidates (optional)
 *
 * NOTE:
 * This file intentionally preserves the core behaviors from:
 * - AddBarcodeViewModel
 * - AddWorkViewModel
 *
 * Next steps (later files):
 * - Wire routes to THIS VM
 * - Remove old VMs
 */
data class BarcodeMasterCandidateUi(
    val masterId: Long,
    val displayTitle: String,
    val year: Int?,
    val thumbUrl: String?,
    val coverUrl: String?,
    val artistLine: String,
    val releaseTitle: String,
)

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

data class IngestUiState(
    // Inputs (overlay)
    val barcode: String = "",
    val artist: String = "",
    val title: String = "",
    val year: String = "",

    // OCR evidence + candidates
    val evidenceUris: List<String> = emptyList(),
    val ocrTitleCandidates: List<String> = emptyList(),
    val ocrArtistCandidates: List<String> = emptyList(),
    val isOcrProcessing: Boolean = false,
    val ocrMessage: String? = null,

    // UI state
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,

    // Results
    val masterCandidate: BarcodeMasterCandidateUi? = null,   // barcode path
    val results: List<DiscogsCandidateUi> = emptyList(),     // text path

    // Preferences
    val autoReopenScanner: Boolean = false,
)

enum class IngestMethod { BARCODE, DISCOGS_TEXT, MANUAL }

enum class OcrCaptureSource(val label: String) {
    COVER("cover"),
    LABEL("label"),
}

@HiltViewModel
class IngestViewModel @Inject constructor(
    // Barcode path uses DiscogsApiService today
    private val discogsApi: DiscogsApiService,
    // Text path uses DiscogsClient today
    private val discogsClient: DiscogsClient,

    private val workRepositoryV2: WorkRepositoryV2,
    private val scannerPreferences: ScannerPreferences,
    private val ocrService: OcrService,
    private val uxEventLogger: UxEventLogger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngestUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scannerPreferences.autoReopenScannerFlow.collect { enabled ->
                _uiState.update { it.copy(autoReopenScanner = enabled) }
            }
        }
    }

    // ----------------------------
    // Input events
    // ----------------------------

    fun onBarcodeChanged(value: String) {
        val cleaned = value.filter(Char::isDigit)
        _uiState.update {
            it.copy(
                barcode = cleaned,
                errorMessage = null,
                infoMessage = null,
                masterCandidate = null,
            )
        }
    }

    fun onArtistChanged(value: String) {
        _uiState.update { it.copy(artist = value, errorMessage = null, infoMessage = null) }
    }

    fun onTitleChanged(value: String) {
        _uiState.update { it.copy(title = value, errorMessage = null, infoMessage = null) }
    }

    fun onYearChanged(value: String) {
        val cleaned = value.filter { it.isDigit() }
        _uiState.update { it.copy(year = cleaned, errorMessage = null, infoMessage = null) }
    }

    fun applyTitleCandidate(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun applyArtistCandidate(value: String) {
        _uiState.update { it.copy(artist = value) }
    }

    // ----------------------------
    // OCR (from AddWorkViewModel)
    // ----------------------------

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

                    val newTitle = if (state.title.isBlank()) titleCandidates.firstOrNull().orEmpty() else state.title
                    val newArtist = if (state.artist.isBlank()) artistCandidates.firstOrNull().orEmpty() else state.artist

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

    // ----------------------------
    // Unified logging + prefs
    // ----------------------------

    fun setAutoReopen(enabled: Boolean) {
        viewModelScope.launch {
            scannerPreferences.setAutoReopenScanner(enabled)
        }
    }

    fun logIngestStart(method: IngestMethod) {
        val methodValue = when (method) {
            IngestMethod.BARCODE -> "barcode"
            IngestMethod.DISCOGS_TEXT -> "discogs"
            IngestMethod.MANUAL -> "manual"
        }
        uxEventLogger.logEvent("pm_ingest_start", mapOf("method" to methodValue))
    }

    // ----------------------------
    // Barcode lookup (from AddBarcodeViewModel)
    // ----------------------------

    fun searchByBarcode() {
        if (BuildConfig.DISCOGS_TOKEN.isBlank()) {
            Log.w("IngestViewModel", "Discogs token missing; returning user-safe error.")
            _uiState.update { it.copy(errorMessage = "Service error. Try again.", masterCandidate = null) }
            return
        }

        val barcode = _uiState.value.barcode.trim()
        if (barcode.length < 8) {
            _uiState.update {
                it.copy(
                    errorMessage = "Barcode looks too short. Enter the full UPC/EAN and try again.",
                    masterCandidate = null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    infoMessage = null,
                    masterCandidate = null,
                    results = emptyList(),
                )
            }

            try {
                val releaseSearch = discogsApi.searchReleases(
                    type = "release",
                    barcode = barcode,
                    perPage = 10,
                    page = 1,
                )

                val candidates = releaseSearch.results
                if (candidates.isEmpty()) {
                    uxEventLogger.logEvent("pm_discogs_lookup_result", mapOf("result" to "empty", "http_code" to 200))
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No match found. Try manual entry.",
                            masterCandidate = null,
                        )
                    }
                    return@launch
                }

                // Try candidates until we can fetch a valid release (handles Discogs 404s for stale IDs).
                val resolvedRelease = run {
                    var lastError: Throwable? = null
                    for (candidate in candidates.take(10)) {
                        try {
                            return@run discogsApi.getRelease(candidate.id)
                        } catch (e: HttpException) {
                            if (e.code() == 404) {
                                lastError = e
                                continue
                            }
                            throw e
                        } catch (t: Throwable) {
                            lastError = t
                            continue
                        }
                    }
                    throw IllegalStateException(
                        "Discogs returned release candidates, but none could be fetched (stale IDs / 404).",
                        lastError,
                    )
                }

                val bestSearchTitle = candidates.first().title

                val artistLine = resolvedRelease.artists
                    ?.mapNotNull { it.name?.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.distinct()
                    ?.joinToString(", ")
                    ?.takeIf { it.isNotBlank() }
                    ?: parseArtistFromSearchTitle(bestSearchTitle)
                    ?: "Unknown Artist"

                val releaseTitle = (resolvedRelease.title ?: parseTitleFromSearchTitle(bestSearchTitle)).trim()
                if (releaseTitle.isBlank()) {
                    throw IllegalStateException("Could not resolve a release title for this barcode.")
                }

                val masterSearch = discogsApi.searchReleases(
                    type = "master",
                    artist = artistLine,
                    releaseTitle = releaseTitle,
                    perPage = 10,
                    page = 1,
                )

                val bestMaster = masterSearch.results.firstOrNull()
                    ?: run {
                        uxEventLogger.logEvent("pm_discogs_lookup_result", mapOf("result" to "empty", "http_code" to 200))
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "No match found. Try manual entry.",
                                masterCandidate = null,
                            )
                        }
                        return@launch
                    }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        masterCandidate = BarcodeMasterCandidateUi(
                            masterId = bestMaster.id,
                            displayTitle = bestMaster.title,
                            year = bestMaster.year,
                            thumbUrl = bestMaster.thumb,
                            coverUrl = bestMaster.coverImage,
                            artistLine = artistLine,
                            releaseTitle = releaseTitle,
                        ),
                    )
                }

                uxEventLogger.logEvent("pm_discogs_lookup_result", mapOf("result" to "success", "http_code" to 200))
            } catch (t: Throwable) {
                logDiscogsLookupFailure(t)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapUserSafeError(t),
                        masterCandidate = null,
                    )
                }
            }
        }
    }

    fun addMasterToLibrary(
        candidate: BarcodeMasterCandidateUi,
        onAdded: (String, Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val master = discogsApi.getMaster(candidate.masterId)

                val artwork = master.images
                    ?.firstOrNull { it.uri?.isNotBlank() == true }
                    ?.uri
                    ?: candidate.coverUrl
                    ?: candidate.thumbUrl

                val title = master.title.ifBlank { candidate.releaseTitle }
                val year = master.year ?: candidate.year

                val result = workRepositoryV2.upsertDiscogsMasterWork(
                    discogsMasterId = master.id,
                    title = title,
                    artistLine = candidate.artistLine,
                    year = year,
                    primaryArtworkUri = artwork,
                    genres = master.genres.orEmpty(),
                    styles = master.styles.orEmpty(),
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

                val autoReopen = _uiState.value.autoReopenScanner
                _uiState.value = IngestUiState(
                    infoMessage = info,
                    autoReopenScanner = autoReopen,
                )

                uxEventLogger.logEvent("pm_work_add_success", mapOf("method" to "barcode", "dedupe" to dedupe))

                val workId = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> result.workId
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> result.workId
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> result.existingWorkId.orEmpty()
                }
                if (workId.isNotBlank()) {
                    onAdded(workId, autoReopen)
                }
            } catch (t: Throwable) {
                Log.e("IngestViewModel", "Failed to add master to library.", t)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapUserSafeError(t),
                    )
                }
            }
        }
    }

    // ----------------------------
    // Manual text search (from AddWorkViewModel)
    // ----------------------------

    fun searchDiscogs() {
        if (BuildConfig.DISCOGS_TOKEN.isBlank()) {
            Log.w("IngestViewModel", "Discogs token missing; returning user-safe error.")
            _uiState.update { it.copy(errorMessage = "Service error. Try again.", results = emptyList()) }
            return
        }

        val artist = _uiState.value.artist.trim()
        val title = _uiState.value.title.trim()
        val year = _uiState.value.year.toIntOrNull()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    infoMessage = null,
                    results = emptyList(),
                    masterCandidate = null,
                )
            }

            try {
                val candidates = discogsClient.searchMasters(
                    artist = artist,
                    title = title,
                    year = year,
                    limit = 10,
                )

                if (candidates.isEmpty()) {
                    uxEventLogger.logEvent("pm_discogs_lookup_result", mapOf("result" to "empty", "http_code" to 200))
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No match found. Try manual entry.",
                            results = emptyList(),
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
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
                }
                uxEventLogger.logEvent("pm_discogs_lookup_result", mapOf("result" to "success", "http_code" to 200))
            } catch (t: Throwable) {
                logDiscogsLookupFailure(t)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapUserSafeError(t),
                        results = emptyList(),
                    )
                }
            }
        }
    }

    fun addToLibrary(
        candidate: DiscogsCandidateUi,
        onAdded: (String) -> Unit,
    ) {
        val (artist, title) = parseArtistTitle(candidate.displayTitle)
        val year = candidate.year

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
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

                uxEventLogger.logEvent("pm_work_add_success", mapOf("method" to "discogs", "dedupe" to dedupe))

                val workId = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> result.workId
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> result.workId
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> result.existingWorkId.orEmpty()
                }

                _uiState.update { it.copy(isLoading = false, infoMessage = info) }
                if (workId.isNotBlank()) {
                    onAdded(workId)
                }
            } catch (t: Throwable) {
                Log.e("IngestViewModel", "Failed to add Discogs work.", t)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapUserSafeError(t),
                    )
                }
            }
        }
    }

    fun addManualWork(onAdded: (String) -> Unit) {
        val artist = _uiState.value.artist.trim()
        val title = _uiState.value.title.trim()
        val year = _uiState.value.year.toIntOrNull()

        if (artist.isBlank() || title.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Artist and title are required.",
                    infoMessage = null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
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

                uxEventLogger.logEvent("pm_work_add_success", mapOf("method" to "manual", "dedupe" to dedupe))

                val workId = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> result.workId
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> result.workId
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> result.existingWorkId.orEmpty()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        infoMessage = info,
                        results = emptyList(),
                    )
                }
                if (workId.isNotBlank()) {
                    onAdded(workId)
                }
            } catch (t: Throwable) {
                Log.e("IngestViewModel", "Failed to add manual work.", t)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapUserSafeError(t),
                    )
                }
            }
        }
    }

    // ----------------------------
    // Helpers
    // ----------------------------

    private fun parseArtistFromSearchTitle(searchTitle: String): String? {
        val parts = searchTitle.split(" - ", limit = 2)
        return parts.getOrNull(0)?.trim()?.ifBlank { null }
    }

    private fun parseTitleFromSearchTitle(searchTitle: String): String {
        val parts = searchTitle.split(" - ", limit = 2)
        return parts.getOrNull(1)?.trim() ?: searchTitle.trim()
    }

    private fun parseArtistTitle(discogsTitle: String): Pair<String, String> {
        val parts = discogsTitle.split(" - ", limit = 2)
        return if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else {
            _uiState.value.artist.trim() to _uiState.value.title.trim()
        }
    }

    private fun mapUserSafeError(t: Throwable): String {
        return when (t) {
            is IOException -> "No connection. Try again."
            is HttpException -> "Service error. Try again."
            else -> "Service error. Try again."
        }
    }

    private fun logDiscogsLookupFailure(t: Throwable) {
        when (t) {
            is HttpException -> {
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
