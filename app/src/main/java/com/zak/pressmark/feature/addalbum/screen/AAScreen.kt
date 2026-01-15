package com.zak.pressmark.feature.addalbum.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.feature.addalbum.model.AddAlbumFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlbumScreen(
    state: AddAlbumFormState,
    onStateChange: (AddAlbumFormState) -> Unit,

    showValidationErrors: Boolean = false,

    // NEW (autocomplete)
    artistSuggestions: List<ArtistEntity> = emptyList(),
    onArtistChange: ((String) -> Unit)? = null,
    onArtistSuggestionClick: ((ArtistEntity) -> Unit)? = null,

    snackbarHostState: SnackbarHostState,
    onNavigateUp: () -> Unit,
    onSave: () -> Unit,
) {
    val titleError = state.title.isBlank()
    val artistError = state.artist.isBlank()
    val yearText = state.releaseYear.trim()
    val yearError = yearText.isNotEmpty() && yearText.toIntOrNull() == null
    // We allow save attempts even when invalid so the user can trigger validation feedback.
    // (e.g. required fields only turn red after a save attempt.)

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )

    val effectiveOnArtistChange: (String) -> Unit =
        onArtistChange ?: { text ->
            onStateChange(state.copy(artist = text, artistId = null))
        }

    val showSuggestions =
        state.artistId == null &&
                state.artist.isNotBlank() &&
                artistSuggestions.isNotEmpty()

    val focusManager = LocalFocusManager.current
    val titleFocus = remember { FocusRequester() }
    val artistFocus = remember { FocusRequester() }
    val yearFocus = remember { FocusRequester() }
    val labelFocus = remember { FocusRequester() }
    val catalogFocus = remember { FocusRequester() }
    val formatFocus = remember { FocusRequester() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Album Entry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Album")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Enter the details for the new album.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = { onStateChange(state.copy(title = it)) },
                label = { Text("Title *") },
                isError = showValidationErrors && titleError,
                supportingText = { if (showValidationErrors && titleError) Text("Required") },
                colors = textFieldColors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { artistFocus.requestFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus)
            )

            OutlinedTextField(
                value = state.artist,
                onValueChange = effectiveOnArtistChange,
                label = { Text("Artist *") },
                isError = showValidationErrors && artistError,
                supportingText = { if (showValidationErrors && artistError) Text("Required") },
                colors = textFieldColors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { yearFocus.requestFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(artistFocus)
            )

            if (showSuggestions) {
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val max = minOf(6, artistSuggestions.size)
                        for (i in 0 until max) {
                            val a = artistSuggestions[i]
                            ListItem(
                                headlineContent = { Text(a.displayName) },
                                supportingContent = { Text(a.artistType.orEmpty()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (onArtistSuggestionClick != null) {
                                            onArtistSuggestionClick(a)
                                        } else {
                                            onStateChange(state.copy(artist = a.displayName, artistId = a.id))
                                        }
                                    }
                            )
                            if (i != max - 1) HorizontalDivider()
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.releaseYear,
                onValueChange = { onStateChange(state.copy(releaseYear = it)) },
                label = { Text("Release Year") },
                isError = yearError,
                supportingText = { if (yearError) Text("Numbers only (e.g. 1984)") },
                colors = textFieldColors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { labelFocus.requestFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(yearFocus)
            )

            OutlinedTextField(
                value = state.label,
                onValueChange = { onStateChange(state.copy(label = it)) },
                label = { Text("Label") },
                colors = textFieldColors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { catalogFocus.requestFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(labelFocus)
            )

            OutlinedTextField(
                value = state.catalogNo,
                onValueChange = { onStateChange(state.copy(catalogNo = it)) },
                label = { Text("Catalog #") },
                colors = textFieldColors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { formatFocus.requestFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(catalogFocus)
            )

            // ✅ NEW: Format
            OutlinedTextField(
                value = state.format,
                onValueChange = { onStateChange(state.copy(format = it)) },
                label = { Text("Format") },
                colors = textFieldColors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(formatFocus)
            )
        }
    }
}
