package com.zak.pressmark.feature.albumlist.coversearch.vm

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

class DiscogsCoverSearchViewModel(
    private val albumId: String,
    private val artist: String,
    private val title: String,
    private val albumRepository: AlbumRepository,
    private val discogsApi: DiscogsApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoverSearchState())
    val uiState = _uiState.asStateFlow()

    init {
        search()
    }

    private fun search() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = CoverSearchState(isLoading = true)
            try {
                val response = discogsApi.searchReleases(
                    artist = artist,
                    releaseTitle = title,
                    perPage = 20,
                )
                _uiState.value = CoverSearchState(results = response.results)
            } catch (t: Throwable) {
                _uiState.value = CoverSearchState(error = t.message ?: "Search failed")
            }
        }
    }

    fun pickResult(result: DiscogsSearchResult) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Save the chosen cover URL and Discogs ID
                albumRepository.setDiscogsCover(
                    albumId = albumId,
                    coverUrl = result.coverImage ?: result.thumb ?: "",
                    discogsReleaseId = result.id
                )
                // 2. Trigger a full refresh to get all the other metadata
                albumRepository.refreshFromDiscogs(albumId)
            } catch (t: Throwable) {
                // Error handling can be enhanced here if needed
            }
        }
    }
}
