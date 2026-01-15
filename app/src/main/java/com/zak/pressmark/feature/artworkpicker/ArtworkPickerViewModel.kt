package com.zak.pressmark.feature.artworkpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.core.util.FuzzyMatch
import com.zak.pressmark.core.util.Normalizer
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

data class CoverSearchRequest(
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

        val sameRequest = currentRequest == request
        if (sameRequest && (_uiState.value.isLoading || _uiState.value.results.isNotEmpty())) return

        currentRequest = request
        search(request)
    }

    private fun search(request: CoverSearchRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = CoverSearchState(isLoading = true)

            try {
                val userArtist = request.artist
                val userTitle = request.title

                val titleQuery = userTitle.takeIf { it.isNotBlank() }

                // First pass: strict-ish search using smart artist variants.
                val artistVariants = Normalizer.artistSearchVariants(userArtist)
                    .ifEmpty { listOf(userArtist).filter { it.isNotBlank() } }

                val merged = LinkedHashMap<Long, DiscogsSearchResult>()
                var lastError: Throwable? = null

                for (artist in artistVariants.take(4)) {
                    try {
                        val response = discogsApi.searchReleases(
                            artist = artist.ifBlank { null },
                            releaseTitle = titleQuery,
                            perPage = 25,
                        )
                        for (r in response.results) {
                            merged.putIfAbsent(r.id, r)
                            if (merged.size >= 30) break
                        }
                        if (merged.size >= 12) break
                    } catch (t: Throwable) {
                        lastError = t
                    }
                }

                var results = merged.values.toList()

                // Fuzzy fallback: if strict search produces nothing, broaden queries.
                if (results.isEmpty()) {
                    results = fuzzyFallbackSearch(
                        userArtist = userArtist,
                        userTitle = userTitle,
                        titleQuery = titleQuery,
                        artistVariants = artistVariants,
                    )
                }

                // Always rank results by fuzzy similarity so fat-fingers float the right match.
                val ranked = rankResults(userArtist, userTitle, results)
                    .take(20)

                _uiState.value = CoverSearchState(
                    results = ranked,
                    error = if (ranked.isEmpty()) {
                        lastError?.message ?: "No results found"
                    } else null
                )
            } catch (t: Throwable) {
                _uiState.value = CoverSearchState(error = t.message ?: "Search failed")
            }
        }
    }

    private suspend fun fuzzyFallbackSearch(
        userArtist: String,
        userTitle: String,
        titleQuery: String?,
        artistVariants: List<String>,
    ): List<DiscogsSearchResult> {
        val merged = LinkedHashMap<Long, DiscogsSearchResult>()

        // 1) Try title-only (best chance if artist is misspelled).
        if (!titleQuery.isNullOrBlank()) {
            val byTitle = runCatching {
                discogsApi.searchReleases(
                    artist = null,
                    releaseTitle = titleQuery,
                    perPage = 50,
                ).results
            }.getOrElse { emptyList() }

            for (r in byTitle) merged.putIfAbsent(r.id, r)
        }

        // 2) Try artist-only (best chance if title is misspelled).
        if (userArtist.isNotBlank()) {
            // Prefer canonical-ish variants first.
            val broadArtists = (artistVariants + listOf(Normalizer.artistDisplay(userArtist), Normalizer.artistSortName(userArtist)))
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(3)

            for (artist in broadArtists) {
                val byArtist = runCatching {
                    discogsApi.searchReleases(
                        artist = artist,
                        releaseTitle = null,
                        perPage = 50,
                    ).results
                }.getOrElse { emptyList() }

                for (r in byArtist) {
                    merged.putIfAbsent(r.id, r)
                    if (merged.size >= 80) break
                }
                if (merged.size >= 40) break
            }
        }

        // If both are blank, nothing to do.
        if (merged.isEmpty() && userTitle.isBlank() && userArtist.isBlank()) return emptyList()

        // Rank and return a wider set so UI still gets good top 20.
        return rankResults(userArtist, userTitle, merged.values.toList())
            .take(60)
    }

    private fun rankResults(
        userArtist: String,
        userTitle: String,
        results: List<DiscogsSearchResult>,
    ): List<DiscogsSearchResult> {
        if (results.isEmpty()) return emptyList()

        return results
            .distinctBy { it.id }
            .sortedByDescending { r ->
                val (candArtist, candTitle) = splitDiscogsTitle(r.title)

                // Discogs often returns title as "Artist - Album".
                // Score both artist and title, but gracefully handle missing pieces.
                FuzzyMatch.artistTitleScore(
                    userArtist = userArtist,
                    userTitle = userTitle,
                    candidateArtist = candArtist,
                    candidateTitle = candTitle,
                )
            }
    }

    private fun splitDiscogsTitle(raw: String?): Pair<String, String> {
        val t = raw?.trim().orEmpty()
        if (t.isBlank()) return "" to ""

        val parts = t.split(" - ", limit = 2)
        return if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else {
            "" to t
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
                // ✅ Removed: albumRepository.refreshFromDiscogs(...)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    error = t.message ?: "Failed to save cover"
                )
            }
        }
    }
}
