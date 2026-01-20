@file:OptIn(FlowPreview::class)

package com.zak.pressmark.feature.catalog.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.model.ReleaseSummary
import com.zak.pressmark.data.repository.ReleaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlbumListUiState(
    val snackMessage: String? = null,
)

enum class CatalogSort {
    AddedNewest,
    TitleAZ,
    ArtistAZ,
    YearNewest,
}

class AlbumListViewModel(
    private val releaseRepository: ReleaseRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AlbumListUiState())
    val ui: StateFlow<AlbumListUiState> = _ui

    // VM-owned controls
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _sort = MutableStateFlow(CatalogSort.TitleAZ)
    val sort: StateFlow<CatalogSort> = _sort

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setSort(value: CatalogSort) {
        _sort.value = value
    }

    fun clearQuery() {
        _query.value = ""
    }

    // Reactive list, filtered + sorted
    val releaseListItems: StateFlow<List<ReleaseSummary>> =
        combine(
            releaseRepository.observeReleaseSummaries(),
            _query.debounce(150).distinctUntilChanged(),
            _sort,
        ) { items, qRaw, sort ->
            val q = qRaw.trim()
            val filtered = if (q.isBlank()) items else items.filter { it.matchesQuery(q) }
            filtered.sortedWith(sort.comparator())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteRelease(release: ReleaseSummary) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                releaseRepository.deleteRelease(release.releaseId)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(snackMessage = t.message ?: "Failed to delete release.")
            }
        }
    }

    fun dismissSnack() {
        _ui.value = _ui.value.copy(snackMessage = null)
    }
}

private fun ReleaseSummary.matchesQuery(qRaw: String): Boolean {
    val needle = qRaw.lowercase()

    fun hit(value: String?): Boolean =
        !value.isNullOrBlank() && value.lowercase().contains(needle)

    return hit(title) ||
        hit(artistLine) ||
        hit(catalogNo) ||
        hit(barcode) ||
        hit(label) ||
        hit(country) ||
        hit(format) ||
        hit(releaseType) ||
        hit(releaseYear?.toString())
}

private fun CatalogSort.comparator(): Comparator<ReleaseSummary> {
    fun titleKey(it: ReleaseSummary) = it.title.lowercase()
    fun artistKey(it: ReleaseSummary) = it.artistLine.lowercase()
    fun addedKey(it: ReleaseSummary) = it.addedAt
    fun yearKey(it: ReleaseSummary) = it.releaseYear ?: Int.MIN_VALUE

    return when (this) {
        CatalogSort.AddedNewest ->
            compareByDescending<ReleaseSummary> { addedKey(it) }
                .thenBy { it.releaseId }

        CatalogSort.TitleAZ ->
            compareBy<ReleaseSummary> { titleKey(it) }
                .thenByDescending { addedKey(it) }
                .thenBy { it.releaseId }

        CatalogSort.ArtistAZ ->
            compareBy<ReleaseSummary> { artistKey(it) }
                .thenBy { titleKey(it) }
                .thenByDescending { addedKey(it) }
                .thenBy { it.releaseId }

        CatalogSort.YearNewest ->
            compareByDescending<ReleaseSummary> { yearKey(it) }
                .thenBy { titleKey(it) }
                .thenByDescending { addedKey(it) }
                .thenBy { it.releaseId }
    }
}
