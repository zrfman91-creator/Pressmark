package com.zak.pressmark.feature.artworkpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.remote.discogs.DiscogsSearchResult
import com.zak.pressmark.data.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CoverSearchState(
    val results: List<DiscogsSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

private data class CoverSearchRequest(
    val albumId: String,
    val artist: String,
    val title: String,
)

class DiscogsCoverSearchViewModel(
    private val albumRepository: AlbumRepository,
    private val discogsApi: DiscogsApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoverSearchState())
    val uiState = _uiState.asStateFlow()

    private var currentRequest: CoverSearchRequest? = null

    /**
     * Call when opening the dialog (or when album/artist/title changes).
     * This keeps the VM construction DI-only, and makes runtime state explicit.
     */
    fun start(
        albumId: String,
        artist: String,
        title: String,
    ) {
        val request = CoverSearchRequest(
            albumId = albumId,
            artist = artist.trim(),
            title = title.trim(),
        )

        // Avoid re-searching if nothing changed and we already have results/loading.
        val sameRequest = currentRequest == request
        if (sameRequest && (_uiState.value.isLoading || _uiState.value.results.isNotEmpty())) return

        currentRequest = request
        search(request)
    }

    private fun search(request: CoverSearchRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = CoverSearchState(isLoading = true)
            try {
                val response = discogsApi.searchReleases(
                    artist = request.artist.ifBlank { null },
                    releaseTitle = request.title.ifBlank { null },
                    perPage = 20,
                )
                _uiState.value = CoverSearchState(results = response.results)
            } catch (t: Throwable) {
                _uiState.value = CoverSearchState(error = t.message ?: "Search failed")
            }
        }
    }

    fun pickResult(result: DiscogsSearchResult) {
        val request = currentRequest ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                albumRepository.setArtworkSelection(
                    albumId = request.albumId,
                    coverUrl = result.coverImage ?: result.thumb ?: "",
                    provider = "discogs",
                    providerItemId = result.id.toString(),
                )
                albumRepository.refreshFromDiscogs(request.albumId)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    error = t.message ?: "Failed to save cover"
                )
            }
        }
    }
}
