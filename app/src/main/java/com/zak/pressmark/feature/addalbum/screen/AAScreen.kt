// FILE: app/src/main/java/com/zak/pressmark/feature/addalbum/screen/AAScreen.kt
package com.zak.pressmark.feature.addalbum.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.feature.addalbum.model.AddAlbumFormState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

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
    onSaveAndExit: () -> Unit,
    onAddAnother: () -> Unit,
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
    val barcodeFocus = remember { FocusRequester() }
    val formatFocus = remember { FocusRequester() }
    val titleBringIntoView = remember { BringIntoViewRequester() }
    val artistBringIntoView = remember { BringIntoViewRequester() }
    val yearBringIntoView = remember { BringIntoViewRequester() }
    val labelBringIntoView = remember { BringIntoViewRequester() }
    val catalogBringIntoView = remember { BringIntoViewRequester() }
    val barcodeBringIntoView = remember { BringIntoViewRequester() }
    val formatBringIntoView = remember { BringIntoViewRequester() }
    var titleBringIntoViewJob by remember { mutableStateOf<Job?>(null) }
    var artistBringIntoViewJob by remember { mutableStateOf<Job?>(null) }
    var yearBringIntoViewJob by remember { mutableStateOf<Job?>(null) }
    var labelBringIntoViewJob by remember { mutableStateOf<Job?>(null) }
    var catalogBringIntoViewJob by remember { mutableStateOf<Job?>(null) }
    var barcodeBringIntoViewJob by remember { mutableStateOf<Job?>(null) }
    var formatBringIntoViewJob by remember { mutableStateOf<Job?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find a Pressing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onAddAnother,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Another")
                    }
                    Button(
                        onClick = onSaveAndExit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Find pressings")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Tell us what you know and we'll find the exact pressing. Discogs only fills fields you leave blank.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            SectionCard(
                title = "Essentials",
                subtitle = "Start with the basics to anchor the search.",
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onStateChange(state.copy(title = it)) },
                    label = { Text("Release title *") },
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
                        .bringIntoViewRequester(titleBringIntoView)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                titleBringIntoViewJob?.cancel()
                                titleBringIntoViewJob = coroutineScope.launch {
                                    delay(80)
                                    titleBringIntoView.bringIntoView()
                                }
                            } else {
                                titleBringIntoViewJob?.cancel()
                                titleBringIntoViewJob = null
                            }
                        }
                )
                FocusedBringIntoViewEffect(
                    isFocused = titleFocused,
                    requester = titleBringIntoView,
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
                        .bringIntoViewRequester(artistBringIntoView)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                artistBringIntoViewJob?.cancel()
                                artistBringIntoViewJob = coroutineScope.launch {
                                    delay(80)
                                    artistBringIntoView.bringIntoView()
                                }
                            } else {
                                artistBringIntoViewJob?.cancel()
                                artistBringIntoViewJob = null
                            }
                        }
                )
                FocusedBringIntoViewEffect(
                    isFocused = artistFocused,
                    requester = artistBringIntoView,
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
            }

            SectionCard(
                title = "Optional details",
                subtitle = "More details = higher confidence match.",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.releaseYear,
                        onValueChange = { onStateChange(state.copy(releaseYear = it)) },
                        label = { Text("Year") },
                        isError = yearError,
                        supportingText = { if (yearError) Text("Numbers only") },
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
                            .weight(1f)
                            .focusRequester(yearFocus)
                            .bringIntoViewRequester(yearBringIntoView)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    yearBringIntoViewJob?.cancel()
                                    yearBringIntoViewJob = coroutineScope.launch {
                                        delay(80)
                                        yearBringIntoView.bringIntoView()
                                    }
                                } else {
                                    yearBringIntoViewJob?.cancel()
                                    yearBringIntoViewJob = null
                                }
                            }
                    )
                    FocusedBringIntoViewEffect(
                        isFocused = yearFocused,
                        requester = yearBringIntoView,
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
                            .weight(1f)
                            .focusRequester(labelFocus)
                            .bringIntoViewRequester(labelBringIntoView)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    labelBringIntoViewJob?.cancel()
                                    labelBringIntoViewJob = coroutineScope.launch {
                                        delay(80)
                                        labelBringIntoView.bringIntoView()
                                    }
                                } else {
                                    labelBringIntoViewJob?.cancel()
                                    labelBringIntoViewJob = null
                                }
                            }
                    )
                    FocusedBringIntoViewEffect(
                        isFocused = labelFocused,
                        requester = labelBringIntoView,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
                            onNext = { barcodeFocus.requestFocus() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(catalogFocus)
                            .bringIntoViewRequester(catalogBringIntoView)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    catalogBringIntoViewJob?.cancel()
                                    catalogBringIntoViewJob = coroutineScope.launch {
                                        delay(80)
                                        catalogBringIntoView.bringIntoView()
                                    }
                                } else {
                                    catalogBringIntoViewJob?.cancel()
                                    catalogBringIntoViewJob = null
                                }
                            }
                    )
                    FocusedBringIntoViewEffect(
                        isFocused = catalogFocused,
                        requester = catalogBringIntoView,
                    )

                    OutlinedTextField(
                        value = state.barcode,
                        onValueChange = { onStateChange(state.copy(barcode = it)) },
                        label = { Text("Barcode") },
                        colors = textFieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { formatFocus.requestFocus() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(barcodeFocus)
                            .bringIntoViewRequester(barcodeBringIntoView)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    barcodeBringIntoViewJob?.cancel()
                                    barcodeBringIntoViewJob = coroutineScope.launch {
                                        delay(80)
                                        barcodeBringIntoView.bringIntoView()
                                    }
                                } else {
                                    barcodeBringIntoViewJob?.cancel()
                                    barcodeBringIntoViewJob = null
                                }
                            }
                    )
                    FocusedBringIntoViewEffect(
                        isFocused = barcodeFocused,
                        requester = barcodeBringIntoView,
                    )
                }

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
                        .bringIntoViewRequester(formatBringIntoView)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                formatBringIntoViewJob?.cancel()
                                formatBringIntoViewJob = coroutineScope.launch {
                                    delay(80)
                                    formatBringIntoView.bringIntoView()
                                }
                            } else {
                                formatBringIntoViewJob?.cancel()
                                formatBringIntoViewJob = null
                            }
                        }
                )
                FocusedBringIntoViewEffect(
                    isFocused = formatFocused,
                    requester = formatBringIntoView,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun FocusedBringIntoViewEffect(
    isFocused: Boolean,
    requester: BringIntoViewRequester,
) {
    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(80)
            requester.bringIntoView()
        }
    }
}
