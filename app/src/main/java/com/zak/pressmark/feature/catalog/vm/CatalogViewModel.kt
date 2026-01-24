@file:OptIn(FlowPreview::class)

package com.zak.pressmark.feature.catalog.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.model.CatalogItemSummary
import com.zak.pressmark.data.repository.CatalogDensity
import com.zak.pressmark.data.repository.CatalogRepository
import com.zak.pressmark.data.repository.CatalogSettingsRepository
import com.zak.pressmark.data.repository.CatalogViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val catalogRepository: CatalogRepository,
    private val catalogSettingsRepository: CatalogSettingsRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AlbumListUiState())
    val ui: StateFlow<AlbumListUiState> = _ui

    // VM-owned controls
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _sort = MutableStateFlow(CatalogSort.TitleAZ)
    val sort: StateFlow<CatalogSort> = _sort

    val viewMode: StateFlow<CatalogViewMode> = catalogSettingsRepository
        .observeViewMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogViewMode.LIST)

    val density: StateFlow<CatalogDensity> = catalogSettingsRepository
        .observeDensity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogDensity.SPACIOUS)

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setSort(value: CatalogSort) {
        _sort.value = value
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun setViewMode(mode: CatalogViewMode) {
        viewModelScope.launch(Dispatchers.IO) {
            catalogSettingsRepository.setViewMode(mode)
        }
    }

    fun setDensity(density: CatalogDensity) {
        viewModelScope.launch(Dispatchers.IO) {
            catalogSettingsRepository.setDensity(density)
        }
    }

    // Reactive list, filtered + sorted in Room
    val catalogItems: StateFlow<List<CatalogItemSummary>> =
        catalogRepository.observeCatalogItemSummaries(
            query = _query.debounce(150).distinctUntilChanged(),
            sort = _sort,
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteCatalogItem(item: CatalogItemSummary) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                catalogRepository.deleteCatalogItem(item.catalogItemId)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(snackMessage = t.message ?: "Failed to delete catalog item.")
            }
        }
    }

    fun dismissSnack() {
        _ui.value = _ui.value.copy(snackMessage = null)
    }
}
