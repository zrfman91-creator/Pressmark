// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/barcode/vm/AddBarcodeViewModel.kt
package com.zak.pressmark.feature.ingest.barcode.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * MASTER-ONLY barcode ingest.
 *
 * Requirement:
 * - Scan barcode -> resolve a release candidate (barcode search)
 * - From that release, extract title + artist
 * - Search Discogs for a MASTER using title + artist
 * - Show ONLY the master candidate (single card) for confirmation
 *
 * Real-world Discogs behavior:
 * - Barcode search can return stale/invalid release IDs (deleted/merged), causing HTTP 404 on /releases/{id}.
 *   We defensively try multiple candidates until we can fetch a valid release.
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

data class AddBarcodeUiState(
    val barcode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val masterCandidate: BarcodeMasterCandidateUi? = null,
)

@HiltViewModel
class AddBarcodeViewModel @Inject constructor(
    private val discogsApi: DiscogsApiService,
    private val workRepositoryV2: WorkRepositoryV2,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBarcodeUiState())
    val uiState = _uiState.asStateFlow()

    fun onBarcodeChanged(value: String) {
        val cleaned = value.filter(Char::isDigit)
        _uiState.value = _uiState.value.copy(
            barcode = cleaned,
            errorMessage = null,
            masterCandidate = null,
        )
    }

    /**
     * Lookup a SINGLE master candidate:
     * 1) barcode -> release search
     * 2) release id -> release details (title/artist)
     * 3) title/artist -> master search
     */
    fun searchByBarcode() {
        if (BuildConfig.DISCOGS_TOKEN.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Missing Discogs token. Add DISCOGS_TOKEN to local.properties and rebuild.",
                masterCandidate = null,
            )
            return
        }

        val barcode = _uiState.value.barcode.trim()
        if (barcode.length < 8) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Barcode looks too short. Enter the full UPC/EAN and try again.",
                masterCandidate = null,
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, masterCandidate = null)

            try {
                val releaseSearch = discogsApi.searchReleases(
                    type = "release",
                    barcode = barcode,
                    perPage = 10,
                    page = 1,
                )

                val candidates = releaseSearch.results
                if (candidates.isEmpty()) {
                    throw IllegalStateException("No Discogs releases found for this barcode.")
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
                        lastError
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
                    ?: throw IllegalStateException("No Discogs master found for '$artistLine — $releaseTitle'.")

                _uiState.value = _uiState.value.copy(
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
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Discogs request failed",
                    masterCandidate = null,
                )
            }
        }
    }

    /**
     * Commit ONLY the master-level Work.
     *
     * Behavior:
     * - Dedupes by discogsMasterId (restores duplicate detection)
     * - After add, resets UI back to the "Add by barcode" menu (no navigation)
     */
    fun addMasterToLibrary(
        candidate: BarcodeMasterCandidateUi,
        onAdded: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val master = discogsApi.getMaster(candidate.masterId)

                val artwork = master.images
                    ?.firstOrNull { it.uri?.isNotBlank() == true }
                    ?.uri
                    ?: candidate.coverUrl
                    ?: candidate.thumbUrl

                val title = master.title.ifBlank { candidate.releaseTitle }
                val year = master.year ?: candidate.year

                val workId = workRepositoryV2.upsertDiscogsMasterWork(
                    discogsMasterId = master.id,
                    title = title,
                    artistLine = candidate.artistLine,
                    year = year,
                    primaryArtworkUri = artwork,
                    genres = master.genres.orEmpty(),
                    styles = master.styles.orEmpty(),
                )

                // Reset the screen back to "Add by barcode" menu, ready for the next scan.
                _uiState.value = AddBarcodeUiState()

                onAdded(workId)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Failed to add to library",
                )
            }
        }
    }

    private fun parseArtistFromSearchTitle(searchTitle: String): String? {
        val parts = searchTitle.split(" - ", limit = 2)
        return parts.getOrNull(0)?.trim()?.ifBlank { null }
    }

    private fun parseTitleFromSearchTitle(searchTitle: String): String {
        val parts = searchTitle.split(" - ", limit = 2)
        return parts.getOrNull(1)?.trim() ?: searchTitle.trim()
    }
}
