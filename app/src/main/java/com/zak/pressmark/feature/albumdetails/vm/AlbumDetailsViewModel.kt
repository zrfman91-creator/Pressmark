package com.zak.pressmark.feature.albumdetails.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AlbumDetailsUiState(
    val editOpen: Boolean = false,
    val deleteConfirmOpen: Boolean = false,
    val snackMessage: String? = null,
    val didDelete: Boolean = false,
)

class AlbumDetailsViewModel(
    private val albumId: String,
    private val repo: AlbumRepository,
) : ViewModel() {

    // This is correct. The repository has this function.
    val album: StateFlow<AlbumEntity?> = repo.observeById(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(AlbumDetailsUiState())
    val ui: StateFlow<AlbumDetailsUiState> = _ui.asStateFlow()

    init {
        // This is correct. The repository has these functions.
        viewModelScope.launch(Dispatchers.IO) {
            val a = repo.getById(albumId) ?: return@launch
            if (a.artistId == null && a.artist.isNotBlank()) {
                runCatching { repo.ensureArtistMasterLink(a) }
            }
        }
    }

    // --- FIX: ADD THE MISSING VIEWMODEL FUNCTIONS ---
    fun refreshFromDiscogs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.refreshFromDiscogs(albumId)
            } catch (t: Throwable) {
                reportError("Failed to refresh from Discogs: ${t.message}")
            }
        }
    }

    fun clearCover() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.clearDiscogsCover(albumId)
        }
    }

    fun setDiscogsCover(coverUrl: String, releaseId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.setDiscogsCover(albumId, coverUrl, releaseId)
        }
    }

    fun dismissSnack() { _ui.value = _ui.value.copy(snackMessage = null) }
    fun openEdit() { _ui.value = _ui.value.copy(editOpen = true) }
    fun closeEdit() { _ui.value = _ui.value.copy(editOpen = false) }
    fun openDeleteConfirm() { _ui.value = _ui.value.copy(deleteConfirmOpen = true) }
    fun closeDeleteConfirm() { _ui.value = _ui.value.copy(deleteConfirmOpen = false) }

    // --- FIX: UPDATE THE saveEdits FUNCTION SIGNATURE ---
    fun saveEdits(
        title: String,
        artist: String,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        //notes: String?,
        //tracklist: String?,
    ) {
        val t = title.trim()
        val a = artist.trim()
        if (t.isBlank() || a.isBlank()) {
            _ui.value = _ui.value.copy(snackMessage = "Title and Artist are required.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.updateAlbum(
                    albumId = albumId,
                    title = t,
                    artist = a,
                    releaseYear = releaseYear,
                    catalogNo = catalogNo,
                    label = label,
                    //notes = notes,
                    //tracklist = tracklist,
                )
                withContext(Dispatchers.Main.immediate) {
                    _ui.value = _ui.value.copy(editOpen = false)
                }
            } catch (t: Throwable) {
                reportError(t.message ?: "Failed to save changes.")
            }
        }
    }

    fun deleteAlbum() {
        val current = album.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.deleteAlbum(current)
                withContext(Dispatchers.Main.immediate) {
                    _ui.value = _ui.value.copy(deleteConfirmOpen = false, didDelete = true)
                }
            } catch (t: Throwable) {
                reportError(t.message ?: "Failed to delete album.")
            }
        }
    }

    private suspend fun reportError(message: String) {
        withContext(Dispatchers.Main.immediate) {
            _ui.value = _ui.value.copy(snackMessage = message)
        }
    }
}
