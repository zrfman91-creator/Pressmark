package com.zak.pressmark.feature.albumlist.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.local.model.AlbumWithArtistName
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
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AlbumListUiState())
    val ui: StateFlow<AlbumListUiState> = _ui

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
                _ui.value = _ui.value.copy(snackMessage = t.message ?: "Failed to delete album.")
            }
        }
    }

    fun refreshAlbumFromDiscogs(album: AlbumEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                albumRepository.refreshFromDiscogs(album.id)
            } catch (t: Throwable) {
                _ui.value =
                    _ui.value.copy(snackMessage = t.message ?: "Failed to refresh from Discogs.")
            }
        }
    }

    fun dismissSnack() {
        _ui.value = _ui.value.copy(snackMessage = null)
    }

    fun updateAlbumFromList(
        albumId: String, // ✅ String, because repo expects String
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
                    ?.let { artistRepository.getOrCreateArtistId(it) } // ✅ resolves to Long?

                // ✅ Match your repo’s expected parameter names (artistId/year/catalogNumber)
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
}
