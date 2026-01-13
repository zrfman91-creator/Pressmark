package com.zak.pressmark.feature.addalbum.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.feature.addalbum.model.AddAlbumFormState
import com.zak.pressmark.feature.addalbum.model.AddAlbumFormStateSaver
import com.zak.pressmark.feature.addalbum.screen.AddAlbumScreen
import com.zak.pressmark.feature.addalbum.vm.AddAlbumEvent
import com.zak.pressmark.feature.addalbum.vm.AddAlbumViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@Composable
fun AddAlbumRoute(
    vm: AddAlbumViewModel,
    onNavigateUp: () -> Unit,
) {
    var form by rememberSaveable(stateSaver = AddAlbumFormStateSaver) {
        mutableStateOf(AddAlbumFormState())
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is AddAlbumEvent.NavigateUp -> onNavigateUp()
                is AddAlbumEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val suggestions by vm.artistSuggestions.collectAsState()

    AddAlbumScreen(
        state = form,
        onStateChange = { form = it },

        artistSuggestions = suggestions,
        onArtistChange = { text ->
            form = form.copy(artist = text, artistId = null)
            vm.onArtistQueryChanged(text)
        },
        onArtistSuggestionClick = { artist: ArtistEntity ->
            form = form.copy(artist = artist.displayName, artistId = artist.id)
            vm.onArtistQueryChanged("") // optional: collapse suggestions
        },

        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,

        // Works whether saveAlbum is suspend or not.
        onSave = { scope.launch { vm.saveAlbum(form) } }
    )
}
