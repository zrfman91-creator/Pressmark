package com.zak.pressmark.feature.albumlist.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AlbumListUiState(
    val snackMessage: String? = null,
)

class AlbumListViewModel(
    private val albumRepository: AlbumRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AlbumListUiState())
    val ui: StateFlow<AlbumListUiState> = _ui

    val albums: StateFlow<List<AlbumEntity>> =
        albumRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteAlbum(album: AlbumEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { albumRepository.deleteAlbum(album) }
                .onFailure { t ->
                    postSnack(t.message ?: "Failed to delete album.")
                }
        }
    }

    fun updateAlbum(
        albumId: String,
        title: String,
        artist: String,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        onError: (String) -> Unit,
        //tracklist: String,
        //notes: String?,
    ) {
        val t = title.trim()
        val a = artist.trim()

        if (t.isBlank() || a.isBlank()) {
            _ui.value = _ui.value.copy(snackMessage = "Title and Artist are required.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                albumRepository.updateAlbum(
                    albumId = albumId,
                    title = t,
                    artist = a,
                    releaseYear = releaseYear,
                    catalogNo = catalogNo,
                    label = label,
                    //tracklist = {/* */},
                    //notes = { /* */ }
                )
            }.onFailure { err ->
                postSnack(err.message ?: "Failed to update album.")
            }
        }
    }

    fun dismissSnack() {
        _ui.value = _ui.value.copy(snackMessage = null)
    }

    private suspend fun postSnack(message: String) {
        withContext(Dispatchers.Main.immediate) {
            _ui.value = _ui.value.copy(snackMessage = message)
        }
    }
}
