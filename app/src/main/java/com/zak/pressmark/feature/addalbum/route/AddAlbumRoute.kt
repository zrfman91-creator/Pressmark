// FILE: app/src/main/java/com/zak/pressmark/feature/addalbum/route/AddAlbumRoute.kt
package com.zak.pressmark.feature.addalbum.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.addalbum.model.AddAlbumFormState
import com.zak.pressmark.feature.addalbum.model.AddAlbumFormStateSaver
import com.zak.pressmark.feature.addalbum.screen.AddAlbumScreen
import com.zak.pressmark.feature.addalbum.vm.AddAlbumEvent
import com.zak.pressmark.feature.addalbum.vm.AddAlbumViewModel
import com.zak.pressmark.feature.addalbum.vm.SaveIntent

@Composable
fun AddAlbumRoute(
    vm: AddAlbumViewModel,
    onNavigateUp: () -> Unit,
    clearFormRequested: Boolean,
    onClearFormConsumed: () -> Unit,
    onAlbumSaved: (
        albumId: String,
        artist: String,
        title: String,
        releaseYear: String,
        label: String,
        catalogNo: String,
        barcode: String,
        intent: SaveIntent,
    ) -> Unit,
) {
    var form by rememberSaveable(stateSaver = AddAlbumFormStateSaver) {
        mutableStateOf(AddAlbumFormState())
    }

    // Only show required-field errors after the user tries to save.
    var hasAttemptedSave by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val suggestions by vm.artistSuggestions.collectAsStateWithLifecycle()

    LaunchedEffect(clearFormRequested) {
        if (clearFormRequested) {
            form = AddAlbumFormState()
            hasAttemptedSave = false
            vm.onArtistQueryChanged("")
            onClearFormConsumed()
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is AddAlbumEvent.NavigateUp -> onNavigateUp()
                is AddAlbumEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is AddAlbumEvent.SaveResult -> {
                    // Always move into cover selection after save.
                    onAlbumSaved(
                        event.albumId,
                        form.artist,
                        form.title,
                        form.releaseYear,
                        form.label,
                        form.catalogNo,
                        form.barcode,
                        event.intent,
                    )
                }
            }
        }
    }

    AddAlbumScreen(
        state = form,
        onStateChange = { form = it },

        showValidationErrors = hasAttemptedSave,

        artistSuggestions = suggestions,
        onArtistChange = { text ->
            form = form.copy(artist = text, artistId = null)
            vm.onArtistQueryChanged(text)
        },
        onArtistSuggestionClick = { a ->
            form = form.copy(artist = a.displayName, artistId = a.id)
        },

        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,
        onSaveAndExit = {
            hasAttemptedSave = true
            vm.saveAlbum(form, SaveIntent.SaveAndExit)
        },
        onAddAnother = {
            hasAttemptedSave = true
            vm.saveAlbum(form, SaveIntent.AddAnother)
        },
    )
}
