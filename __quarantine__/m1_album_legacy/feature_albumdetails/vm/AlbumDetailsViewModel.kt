// FILE: app/src/main/java/com/zak/pressmark/feature/albumdetails/vm/AlbumDetailsViewModel.kt
package com.zak.pressmark.feature.albumdetails.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository
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
    private val artistRepo: ArtistRepository,
) : ViewModel() {

    val album: StateFlow<AlbumWithArtistName?> =
        repo.observeByIdWithArtistName(albumId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(AlbumDetailsUiState())
    val ui: StateFlow<AlbumDetailsUiState> = _ui.asStateFlow()

    fun openEdit() {
        _ui.value = _ui.value.copy(editOpen = true)
    }

    fun closeEdit() {
        _ui.value = _ui.value.copy(editOpen = false)
    }

    fun openDeleteConfirm() {
        _ui.value = _ui.value.copy(deleteConfirmOpen = true)
    }

    fun closeDeleteConfirm() {
        _ui.value = _ui.value.copy(deleteConfirmOpen = false)
    }

    fun saveEdits(
        title: String,
        artist: String,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ) {
        val t = title.trim()
        val a = artist.trim()

        if (t.isBlank()) {
            _ui.value = _ui.value.copy(snackMessage = "Title is required")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Artist optional: only resolve if user actually provided one
                val artistId = if (a.isBlank()) null else artistRepo.getOrCreateArtistId(a)

                repo.updateAlbum(
                    albumId = albumId,
                    title = t,
                    artistId = artistId,
                    releaseYear = releaseYear,
                    catalogNo = catalogNo,
                    label = label,
                    format = format,
                )

                withContext(Dispatchers.Main.immediate) {
                    _ui.value = _ui.value.copy(editOpen = false)
                }
            } catch (e: IllegalArgumentException) {
                // Repo validation (Title required, Year invalid, etc.)
                reportError(e.message ?: "Invalid input")
            } catch (t: Throwable) {
                reportError(t.message ?: "Failed to save changes.")
            }
        }
    }

    fun deleteAlbum() {
        val current = album.value?.album ?: run {
            _ui.value = _ui.value.copy(snackMessage = "Album not found.")
            return
        }

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

    fun clearCover() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.setArtworkSelection(
                    albumId = albumId,
                    coverUrl = null,
                    provider = null,
                    providerItemId = null,
                )
            } catch (t: Throwable) {
                reportError(t.message ?: "Failed to clear cover.")
            }
        }
    }

    fun refreshDiscogsCover() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // For now: clear and show message (actual refresh/search is handled by cover search flow)
                repo.setArtworkSelection(
                    albumId = albumId,
                    coverUrl = null,
                    provider = null,
                    providerItemId = null,
                )
                withContext(Dispatchers.Main.immediate) {
                    _ui.value = _ui.value.copy(
                        snackMessage = "Cover cleared. Next: we’ll wire Discogs refresh/search here."
                    )
                }
            } catch (t: Throwable) {
                reportError(t.message ?: "Failed to refresh cover.")
            }
        }
    }

    fun setDiscogsCover(
        coverUrl: String?,
        discogsReleaseId: Long?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.setArtworkSelection(
                    albumId = albumId,
                    coverUrl = coverUrl,
                    provider = "discogs",
                    providerItemId = discogsReleaseId?.toString(),
                )
            } catch (t: Throwable) {
                reportError(t.message ?: "Failed to set Discogs cover.")
            }
        }
    }

    fun dismissSnack() {
        _ui.value = _ui.value.copy(snackMessage = null)
    }

    private fun reportError(message: String) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _ui.value = _ui.value.copy(snackMessage = message)
        }
    }
}
