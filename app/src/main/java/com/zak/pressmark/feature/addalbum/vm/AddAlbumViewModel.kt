package com.zak.pressmark.feature.addalbum.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.data.local.entity.ArtistType
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.zak.pressmark.feature.addalbum.model.AddAlbumFormState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn


/**
 * UI events that the ViewModel can send to the screen.
 */
sealed interface AddAlbumEvent {
    data object NavigateUp : AddAlbumEvent
    data class ShowSnackbar(val message: String) : AddAlbumEvent
}
@OptIn(kotlinx.coroutines.FlowPreview::class)
@ExperimentalCoroutinesApi
class AddAlbumViewModel(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
) : ViewModel() {

    private val _events = MutableSharedFlow<AddAlbumEvent>()
    val events = _events.asSharedFlow()

    private val artistQuery = MutableStateFlow("")

    val artistSuggestions: StateFlow<List<ArtistEntity>> =
        artistQuery
            .debounce(150)
            .distinctUntilChanged()
            .mapLatest { q ->
                if (q.isBlank()) emptyList() else artistRepository.findByName(q)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onArtistQueryChanged(text: String) {
        artistQuery.value = text
    }


    suspend fun saveAlbum(form: AddAlbumFormState) {
        val cleanTitle = form.title.trim()
        val cleanArtist = form.artist.trim()

        val artistEntity = if (form.artistId != null) {
            // If you want: you *could* skip fetching and trust the id.
            // But if you prefer safety, observeById or fetch-by-id (you already have observeById).
            null
        } else {
            artistRepository.getOrCreate(cleanArtist, ArtistType.BAND) // or PERSON if you have a selector
        }

        val resolvedArtistId = form.artistId ?: (artistEntity?.id
            ?: error("Artist ID missing after getOrCreate"))

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
            viewModelScope.launch {
                _events.emit(AddAlbumEvent.ShowSnackbar("Release year must be a number."))
            }
            return
        }

        val cleanLabel = form.label.trim().takeIf { it.isNotBlank() }
        val cleanCatalogNo = form.catalogNo.trim().takeIf { it.isNotBlank() }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                albumRepository.addAlbum(
                    title = cleanTitle,
                    artist = cleanArtist,
                    artistId = resolvedArtistId,
                    releaseYear = year,
                    label = cleanLabel,
                    catalogNo = cleanCatalogNo,
                )
                withContext(Dispatchers.Main) {
                    _events.emit(AddAlbumEvent.NavigateUp)
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    _events.emit(AddAlbumEvent.ShowSnackbar(t.message ?: "Failed to save album."))
                }
            }
        }
    }
}