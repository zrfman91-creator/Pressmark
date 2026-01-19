// file: app/src/main/java/com/zak/pressmark/feature/albumlist/vm/AlbumListViewModel.kt
package com.zak.pressmark.feature.albumlist.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.local.entity.ReleaseEntity
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import com.zak.pressmark.data.local.model.ReleaseListItem
import com.zak.pressmark.data.local.repository.ReleaseRepository
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlbumListUiState(
    val snackMessage: String? = null,
)

class AlbumListViewModel(
    // Legacy (kept temporarily to avoid breaking other screens that still depend on album flows)
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,

    // New canonical list source
    private val releaseRepository: ReleaseRepository,
) : ViewModel() {

    companion object {
        @Volatile
        private var didRunLegacyArtworkProviderBackfill: Boolean = false
    }

    init {
        // One-time normalization: ensure provider fields are populated for legacy Discogs covers.
        if (!didRunLegacyArtworkProviderBackfill) {
            didRunLegacyArtworkProviderBackfill = true
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { albumRepository.backfillArtworkProviderFromLegacyDiscogs() }
            }
        }

        refreshReleases()
    }

    private val _ui = MutableStateFlow(AlbumListUiState())
    val ui: StateFlow<AlbumListUiState> = _ui

    // -----------------------------
    // New canonical list (Release-first)
    // -----------------------------

    private val _releaseListItems = MutableStateFlow<List<ReleaseListItem>>(emptyList())
    val releaseListItems: StateFlow<List<ReleaseListItem>> = _releaseListItems

    fun refreshReleases() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _releaseListItems.value = releaseRepository.listReleaseListItems()
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    snackMessage = t.message ?: "Failed to load releases."
                )
            }
        }
    }

    fun deleteRelease(release: ReleaseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                releaseRepository.deleteRelease(release.id)
                refreshReleases()
            } catch (t: Throwable) {
                _ui.value =
                    _ui.value.copy(snackMessage = t.message ?: "Failed to delete release.")
            }
        }
    }

    // -----------------------------
    // Legacy album flows (temporary)
    // -----------------------------

    // ✅ Canonical list for UI (artist name comes from Artist table)
    val albumsWithArtistName: StateFlow<List<AlbumWithArtistName>> =
        albumRepository.observeAllWithArtistName()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // (Optional) legacy list, keep until everything migrated
    val albums: StateFlow<List<AlbumEntity>> =
        albumRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteAlbum(album: AlbumEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                albumRepository.deleteAlbum(album)
            } catch (t: Throwable) {
                _ui.value =
                    _ui.value.copy(snackMessage = t.message ?: "Failed to delete album.")
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
                _ui.value = _ui.value.copy(
                    snackMessage = t.message ?: "Could not save changes."
                )
            }
        }
    }

    fun dismissSnack() {
        _ui.value = _ui.value.copy(snackMessage = null)
    }
}
