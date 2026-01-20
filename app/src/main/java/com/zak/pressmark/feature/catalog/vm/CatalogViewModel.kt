@file:OptIn(FlowPreview::class)

package com.zak.pressmark.feature.catalog.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.local.entity.ReleaseEntity
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import com.zak.pressmark.data.local.model.ReleaseListItem
import com.zak.pressmark.data.repository.ReleaseRepository
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository
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
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val releaseRepository: ReleaseRepository,
) : ViewModel() {

    companion object {
        @Volatile
        private var didRunLegacyArtworkProviderBackfill: Boolean = false
    }

    init {
        if (!didRunLegacyArtworkProviderBackfill) {
            didRunLegacyArtworkProviderBackfill = true
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { albumRepository.backfillArtworkProviderFromLegacyDiscogs() }
            }
        }
    }

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
    val releaseListItems: StateFlow<List<ReleaseListItem>> =
        combine(
            releaseRepository.observeReleaseListItems(),
            _query.debounce(150).distinctUntilChanged(),
            _sort,
        ) { items, qRaw, sort ->
            val q = qRaw.trim()
            val filtered = if (q.isBlank()) items else items.filter { it.matchesQuery(q) }
            filtered.sortedWith(sort.comparator())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteRelease(release: ReleaseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                releaseRepository.deleteRelease(release.id)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(snackMessage = t.message ?: "Failed to delete release.")
            }
        }
    }

    // Legacy album flows (kept)
    val albumsWithArtistName: StateFlow<List<AlbumWithArtistName>> =
        albumRepository.observeAllWithArtistName()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<AlbumEntity>> =
        albumRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteAlbum(album: AlbumEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                albumRepository.deleteAlbum(album)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(snackMessage = t.message ?: "Failed to delete album.")
            }
        }
    }

    fun updateAlbumFromList(
        albumId: String,
        title: String,
        artist: String,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val artistId = artist.trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { artistRepository.getOrCreateArtistId(it) }

                albumRepository.updateAlbum(
                    albumId = albumId,
                    title = title,
                    artistId = artistId,
                    releaseYear = releaseYear,
                    catalogNo = catalogNo,
                    label = label,
                    format = format,
                )
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(snackMessage = t.message ?: "Could not save changes.")
            }
        }
    }

    fun dismissSnack() {
        _ui.value = _ui.value.copy(snackMessage = null)
    }
}

private fun ReleaseListItem.matchesQuery(qRaw: String): Boolean {
    val needle = qRaw.lowercase()
    val r = release

    fun hit(value: String?): Boolean =
        !value.isNullOrBlank() && value.lowercase().contains(needle)

    return hit(r.title) ||
            hit(artistLine) ||
            hit(r.catalogNo) ||
            hit(r.barcode) ||
            hit(r.label) ||
            hit(r.country) ||
            hit(r.format) ||
            hit(r.releaseType) ||
            hit(r.releaseYear?.toString())
}

private fun CatalogSort.comparator(): Comparator<ReleaseListItem> {
    fun titleKey(it: ReleaseListItem) = it.release.title.lowercase()
    fun artistKey(it: ReleaseListItem) = it.artistLine.lowercase()
    fun addedKey(it: ReleaseListItem) = it.release.addedAt
    fun yearKey(it: ReleaseListItem) = it.release.releaseYear ?: Int.MIN_VALUE

    return when (this) {
        CatalogSort.AddedNewest ->
            compareByDescending<ReleaseListItem> { addedKey(it) }
                .thenBy { it.release.id }

        CatalogSort.TitleAZ ->
            compareBy<ReleaseListItem> { titleKey(it) }
                .thenByDescending { addedKey(it) }
                .thenBy { it.release.id }

        CatalogSort.ArtistAZ ->
            compareBy<ReleaseListItem> { artistKey(it) }
                .thenBy { titleKey(it) }
                .thenByDescending { addedKey(it) }
                .thenBy { it.release.id }

        CatalogSort.YearNewest ->
            compareByDescending<ReleaseListItem> { yearKey(it) }
                .thenBy { titleKey(it) }
                .thenByDescending { addedKey(it) }
                .thenBy { it.release.id }
    }
}
