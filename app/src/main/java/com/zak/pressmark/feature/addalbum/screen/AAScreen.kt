package com.zak.pressmark.feature.addalbum.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val canSave = !titleError && !artistError && !yearError

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
            // Default behavior if caller doesn't provide autocomplete wiring:
            // typing clears selection.
            onStateChange(state.copy(artist = text, artistId = null))
        }

    val showSuggestions =
        state.artistId == null &&
                state.artist.isNotBlank() &&
                artistSuggestions.isNotEmpty()

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
                    IconButton(onClick = onSave, enabled = canSave) {
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
                isError = titleError,
                supportingText = { if (titleError) Text("Required") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.artist,
                onValueChange = effectiveOnArtistChange,
                label = { Text("Artist *") },
                isError = artistError,
                supportingText = { if (artistError) Text("Required") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
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
                                supportingContent = { Text(a.artistType) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (onArtistSuggestionClick != null) {
                                            onArtistSuggestionClick(a)
                                        } else {
                                            // fallback: lock selection locally
                                            onStateChange(state.copy(artist = a.displayName, artistId = a.id))
                                        }
                                    }
                            )
                            if (i != max - 1) HorizontalDivider()
                        }
                    }
                    if (state.artistId != null) {
                        Text(
                            text = "Selected from library",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            OutlinedTextField(
                value = state.label,
                onValueChange = { onStateChange(state.copy(label = it)) },
                label = { Text("Label") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.catalogNo,
                onValueChange = { onStateChange(state.copy(catalogNo = it)) },
                label = { Text("Catalog #") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
