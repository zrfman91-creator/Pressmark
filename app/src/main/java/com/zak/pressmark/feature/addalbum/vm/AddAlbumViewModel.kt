// FILE: app/src/main/java/com/zak/pressmark/feature/addalbum/vm/AddAlbumViewModel.kt
@file:OptIn(ExperimentalCoroutinesApi::class)
package com.zak.pressmark.feature.addalbum.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.data.local.entity.ReleaseEntity
import com.zak.pressmark.data.repository.ReleaseRepository
import com.zak.pressmark.data.repository.ArtistRepository
import com.zak.pressmark.feature.addalbum.model.AddAlbumFormState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface AddAlbumEvent {
    data object NavigateUp : AddAlbumEvent
    data class ShowSnackbar(val message: String) : AddAlbumEvent
    data class SaveResult(
        val albumId: String,
        val intent: SaveIntent,
    ) : AddAlbumEvent
}

enum class SaveIntent {
    AddAnother,
    SaveAndExit,
}

class AddAlbumViewModel(
    private val artistRepository: ArtistRepository,
    private val releaseRepository: ReleaseRepository,
) : ViewModel() {

    private val _events = MutableSharedFlow<AddAlbumEvent>()
    val events = _events.asSharedFlow()

    // -----------------------------
    // Artist suggestions (for AddAlbumRoute)
    // -----------------------------
    private val artistQuery = MutableStateFlow("")

    /**
     * AARoute collects this.
     */
    val artistSuggestions = artistQuery
        .map { it.trim() }
        .flatMapLatest { q ->
            if (q.isBlank()) {
                artistRepository.observeTopArtists()
            } else {
                artistRepository.searchByName(q)
            }
        }
        .catch { emit(emptyList<ArtistEntity>()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * AARoute calls this whenever the artist text changes.
     */
    fun onArtistQueryChanged(text: String) {
        artistQuery.value = text
    }

    // -----------------------------
    // Save
    // -----------------------------
    fun saveAlbum(form: AddAlbumFormState, intent: SaveIntent) {
        val cleanTitle = form.title.trim()
        val cleanArtist = form.artist.trim()

        if (cleanTitle.isBlank()) {
            viewModelScope.launch { _events.emit(AddAlbumEvent.ShowSnackbar("Title is required.")) }
            return
        }

        if (cleanArtist.isBlank()) {
            viewModelScope.launch { _events.emit(AddAlbumEvent.ShowSnackbar("Artist is required.")) }
            return
        }

        val yearText = form.releaseYear.trim()
        val year: Int? = if (yearText.isBlank()) null else yearText.toIntOrNull()

        if (yearText.isNotBlank() && year == null) {
            viewModelScope.launch { _events.emit(AddAlbumEvent.ShowSnackbar("Release year must be a number.")) }
            return
        }

        val cleanLabel = form.label.trim().takeIf { it.isNotBlank() }
        val cleanCatalogNo = form.catalogNo.trim().takeIf { it.isNotBlank() }
        val cleanFormat = form.format.trim().takeIf { it.isNotBlank() }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val releaseId = UUID.randomUUID().toString()
                val release = ReleaseEntity(
                    id = releaseId,
                    title = cleanTitle,
                    releaseYear = year,
                    label = cleanLabel,
                    catalogNo = cleanCatalogNo,
                    format = cleanFormat,
                    addedAt = now,
                )

                // Parse roles like "feat.", "with", "and his orchestra" into structured credits.
                releaseRepository.upsertReleaseFromRawArtist(
                    release = release,
                    rawArtist = cleanArtist,
                )

                withContext(Dispatchers.Main) {
                    _events.emit(
                        AddAlbumEvent.SaveResult(
                            albumId = releaseId,
                            intent = intent,
                        )
                    )
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    _events.emit(AddAlbumEvent.ShowSnackbar(t.message ?: "Failed to save album."))
                }
            }
        }
    }
}
