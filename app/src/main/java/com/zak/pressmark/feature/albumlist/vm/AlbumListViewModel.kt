package com.zak.pressmark.feature.albumlist.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlbumListUiState(
    val snackMessage: String? = null,
)

class AlbumListViewModel(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AlbumListUiState())
    val ui = _ui.asStateFlow()

    fun dismissSnack() {
        _ui.value = _ui.value.copy(snackMessage = null)
    }

    fun updateAlbumFromList(
        albumId: String,
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                albumRepository.updateAlbum(
                    id = albumId,
                    title = title,
                    artistId = artistId,
                    releaseYear = releaseYear,
                    catalogNo = catalogNo,
                    label = label,
                    format = format,
                    notes = null,
                )
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    snackMessage = t.message ?: "Could not save changes."
                )
            }
        }
    }

    fun deleteAlbum(album: AlbumEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                albumRepository.deleteAlbum(album.id)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    snackMessage = t.message ?: "Failed to delete album."
                )
            }
        }
    }
}
