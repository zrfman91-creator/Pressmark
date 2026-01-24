// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/manual/vm/AddWorkViewModel.kt
package com.zak.pressmark.feature.ingest.manual.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.data.remote.discogs.DiscogsClient
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val results: List<DiscogsCandidateUi> = emptyList(),
)

@HiltViewModel
class AddWorkViewModel @Inject constructor(
    private val discogsClient: DiscogsClient,
    private val workRepositoryV2: WorkRepositoryV2,
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

    fun searchDiscogs() {
        if (BuildConfig.DISCOGS_TOKEN.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Missing Discogs token. Add DISCOGS_TOKEN to local.properties and rebuild.",
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
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Discogs request failed",
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
    ) {
        val (artist, title) = parseArtistTitle(candidate.displayTitle)
        val year = candidate.year

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

                _uiState.value = _uiState.value.copy(isLoading = false, infoMessage = info)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Failed to add to library",
                )
            }
        }
    }

    fun addManualWork() {
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

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    infoMessage = info,
                    results = emptyList(),
                )
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Failed to add to library",
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
}
