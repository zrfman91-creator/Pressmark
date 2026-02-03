package com.zak.pressmark.feature.ingest.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.core.analytics.UxEventLogger
import com.zak.pressmark.core.util.ocr.OcrHint
import com.zak.pressmark.core.util.ocr.OcrService
import com.zak.pressmark.data.prefs.ScannerPreferences
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.remote.discogs.primaryFullUrl
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

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
    val title: String,        // display title (often "Artist - Title")
    val subtitle: String?,
    val year: Int?,
    val thumbUrl: String?,
    val coverUrl: String?,
    val genres: List<String>,
    val styles: List<String>,
)

data class IngestUiState(
    val barcode: String = "",
    val artist: String = "",
    val title: String = "",
    val year: String = "",
    val method: IngestMethod = IngestMethod.BARCODE,

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,

    // Barcode flow
    val masterCandidate: BarcodeMasterCandidateUi? = null,

    // Manual Discogs text flow
    val results: List<DiscogsCandidateUi> = emptyList(),

    // OCR flow (UI can be added later; keep state minimal + stable)
    val ocrCaptureSource: OcrCaptureSource = OcrCaptureSource.COVER,
    val ocrHint: OcrHint? = null,

    // Scanner pref
    val autoReopenScanner: Boolean = false,

    // Scanner overlay
    val manualEntryExpanded: Boolean = false,
)

enum class IngestMethod { BARCODE, DISCOGS_TEXT, MANUAL }

enum class OcrCaptureSource(val label: String) {
    COVER("cover"),
    LABEL("label"),
}

@HiltViewModel
class IngestViewModel @Inject constructor(
    /**
     * ✅ Single Discogs stack:
     * - OkHttp configured in NetworkModule
     * - DiscogsApiService provided in DiscogsModule
     * - No secondary Discogs client / duplicate wiring here
     */
    private val discogsApi: DiscogsApiService,
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
    // UI → State setters
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
        _uiState.update { it.copy(year = value, errorMessage = null, infoMessage = null) }
    }

    fun onMethodChanged(method: IngestMethod) {
        _uiState.update {
            it.copy(
                method = method,
                errorMessage = null,
                infoMessage = null,
                results = emptyList(),
                masterCandidate = null,
            )
        }
    }

    fun onManualEntryExpandedChanged(expanded: Boolean) {
        _uiState.update { it.copy(manualEntryExpanded = expanded) }
    }

    /**
     * Canonical “reset” used by routes/scaffold.
     * Keeps autoReopenScanner value intact.
     */
    fun resetTransientState() {
        val autoReopen = _uiState.value.autoReopenScanner
        _uiState.value = IngestUiState(autoReopenScanner = autoReopen)
    }

    fun clearManualInputs() {
        _uiState.update { it.copy(artist = "", title = "", year = "", errorMessage = null, infoMessage = null, results = emptyList()) }
    }

    fun clearBarcodeCandidate() {
        _uiState.update { it.copy(masterCandidate = null, errorMessage = null, infoMessage = null) }
    }

    // ----------------------------
    // Barcode lookup → candidate master
    // ----------------------------

    fun lookupBarcode(barcode: String) {
        if (BuildConfig.DISCOGS_TOKEN.isBlank()) {
            Log.w("IngestViewModel", "Discogs token missing; returning user-safe error.")
            _uiState.update { it.copy(errorMessage = "Service error. Try again.", masterCandidate = null) }
            return
        }

        val cleaned = barcode.filter(Char::isDigit)
        if (cleaned.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    infoMessage = null,
                    masterCandidate = null,
                    results = emptyList(),
                    method = IngestMethod.BARCODE,
                )
            }

            try {
                val releaseSearch = discogsApi.searchReleases(
                    barcode = cleaned,
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

                // Resolve a real release (Discogs can return stale IDs that 404)
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

                val releaseTitle = (resolvedRelease.title ?: parseTitleFromSearchTitle(bestSearchTitle))!!.trim()
                if (releaseTitle.isBlank()) {
                    throw IllegalStateException("Could not resolve a release title for this barcode.")
                }

                val masterSearch = discogsApi.searchMasters(
                    artist = artistLine,
                    releaseTitle = releaseTitle,
                    year = resolvedRelease.year,
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

                val artwork = master.images.primaryFullUrl()
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
    // Manual text search (Discogs-only) + add
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
                    method = IngestMethod.DISCOGS_TEXT,
                )
            }

            try {
                val masterSearch = discogsApi.searchMasters(
                    artist = artist,
                    releaseTitle = title,
                    year = year,
                    perPage = 10,
                    page = 1,
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        results = masterSearch.results.map { m ->
                            DiscogsCandidateUi(
                                masterId = m.id,
                                title = m.title,
                                subtitle = null,
                                year = m.year,
                                thumbUrl = m.thumb,
                                coverUrl = m.coverImage,
                                genres = emptyList(),
                                styles = emptyList(),
                            )
                        },
                    )
                }
            } catch (t: Throwable) {
                Log.e("IngestViewModel", "Discogs master search failed.", t)
                _uiState.update { it.copy(isLoading = false, errorMessage = mapUserSafeError(t)) }
            }
        }
    }

    fun addDiscogsCandidateToLibrary(
        candidate: DiscogsCandidateUi,
        onAdded: (workId: String, autoReopen: Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val master = discogsApi.getMaster(candidate.masterId)

                val artwork = master.images.primaryFullUrl()
                    ?: candidate.coverUrl
                    ?: candidate.thumbUrl

                val (artistLine, releaseTitle) = splitArtistTitle(candidate.title)

                val result = workRepositoryV2.upsertDiscogsMasterWork(
                    discogsMasterId = master.id,
                    title = master.title.ifBlank { releaseTitle },
                    artistLine = artistLine,
                    year = master.year ?: candidate.year,
                    primaryArtworkUri = artwork,
                    genres = master.genres.orEmpty(),
                    styles = master.styles.orEmpty(),
                )

                val info = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> "Added to library."
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> "Already in library — updated details."
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> "Possible duplicate — added anyway."
                }

                val autoReopen = _uiState.value.autoReopenScanner
                _uiState.value = IngestUiState(
                    infoMessage = info,
                    autoReopenScanner = autoReopen,
                )

                val workId = when (result) {
                    is WorkRepositoryV2.UpsertResult.Created -> result.workId
                    is WorkRepositoryV2.UpsertResult.UpdatedExisting -> result.workId
                    is WorkRepositoryV2.UpsertResult.PossibleDuplicate -> result.existingWorkId.orEmpty()
                }

                if (workId.isNotBlank()) onAdded(workId, autoReopen)
            } catch (t: Throwable) {
                Log.e("IngestViewModel", "Failed to add Discogs master to library.", t)
                _uiState.update { it.copy(isLoading = false, errorMessage = mapUserSafeError(t)) }
            }
        }
    }

    // ----------------------------
    // Helpers
    // ----------------------------

    private fun splitArtistTitle(text: String): Pair<String, String> {
        // Common Discogs master string is "Artist - Title"
        val parts = text.split(" - ", limit = 2)
        return if (parts.size == 2) {
            parts[0].trim().ifBlank { "Unknown Artist" } to parts[1].trim().ifBlank { text.trim() }
        } else {
            "Unknown Artist" to text.trim()
        }
    }

    private fun parseArtistFromSearchTitle(searchTitle: String?): String? {
        if (searchTitle.isNullOrBlank()) return null
        val parts = searchTitle.split(" - ", limit = 2)
        return parts.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun parseTitleFromSearchTitle(searchTitle: String?): String? {
        if (searchTitle.isNullOrBlank()) return null
        val parts = searchTitle.split(" - ", limit = 2)
        return parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun logDiscogsLookupFailure(t: Throwable) {
        val httpCode = (t as? HttpException)?.code()
        uxEventLogger.logEvent(
            "pm_discogs_lookup_result",
            buildMap {
                put("result", "failure")
                if (httpCode != null) put("http_code", httpCode)
                put("error", t::class.java.simpleName)
            },
        )
        Log.e("IngestViewModel", "Discogs barcode lookup failed.", t)
    }

    private fun mapUserSafeError(t: Throwable): String {
        return when (t) {
            is HttpException -> when (t.code()) {
                401, 403 -> "Service auth error. Try again later."
                404 -> "Not found. Try manual entry."
                429 -> "Rate limited. Try again in a moment."
                in 500..599 -> "Service is having trouble. Try again."
                else -> "Request failed. Try again."
            }
            is IOException -> "Network error. Check connection."
            else -> "Something went wrong. Try again."
        }
    }
}
