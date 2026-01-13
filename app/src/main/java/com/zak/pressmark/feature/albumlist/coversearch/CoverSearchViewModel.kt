package com.zak.pressmark.feature.albumlist.coversearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.remote.discogs.DiscogsSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CoverSearchState(
    val loading: Boolean = false,
    val error: String? = null,
    val results: List<DiscogsSearchResult> = emptyList(),
)

class CoverSearchViewModel(
    private val api: DiscogsApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(CoverSearchState())
    val state: StateFlow<CoverSearchState> = _state

    fun search(
        artist: String,
        title: String,
        label: String?,
        catno: String?,
    ) {
        _state.value = _state.value.copy(loading = true, error = null, results = emptyList())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = api.searchReleases(
                    artist = artist.ifBlank { null },
                    releaseTitle = title.ifBlank { null },
                    label = label?.takeIf { it.isNotBlank() },
                    catno = catno?.takeIf { it.isNotBlank() },
                )
                _state.value = CoverSearchState(loading = false, results = resp.results)
            } catch (t: Throwable) {
                _state.value = CoverSearchState(loading = false, error = t.message ?: "Search failed")
            }
        }
    }
}