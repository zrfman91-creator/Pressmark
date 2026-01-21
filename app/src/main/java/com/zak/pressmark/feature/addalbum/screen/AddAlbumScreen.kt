// FILE: app/src/main/java/com/zak/pressmark/feature/addalbum/screen/AddAlbumScreen.kt
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.feature.addalbum.model.AddAlbumFormState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlbumScreen(
    state: AddAlbumFormState,
    onStateChange: (AddAlbumFormState) -> Unit,
    showValidationErrors: Boolean = false,
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
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val scrollState = rememberScrollState()

    // Track container + child coordinates so we can scroll reliably.
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var titleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var artistCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var yearCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var labelCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var catalogCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var barcodeCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var formatCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var scrollJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleScrollIntoView(getChildCoords: () -> LayoutCoordinates?) {
        scrollJob?.cancel()
        scrollJob = scope.launch {
            // Let layout settle after focus change. (Bottom CTA also re-measures when IME appears.)
            delay(140)

            val container = containerCoords ?: return@launch
            val child = getChildCoords() ?: return@launch

            val containerBounds = container.boundsInWindow()
            val childBounds = child.boundsInWindow()

            val paddingPx = with(density) { 24.dp.toPx() }

            // Because the bottom CTA bar uses imePadding(), Scaffold's content area already avoids the IME.
            // So we do NOT subtract IME height here; we only keep a comfortable padding margin.
            val visibleTop = containerBounds.top + paddingPx
            val visibleBottom = containerBounds.bottom - paddingPx

            var deltaPx = 0f
            if (childBounds.bottom > visibleBottom) {
                deltaPx = childBounds.bottom - visibleBottom
            } else if (childBounds.top < visibleTop) {
                deltaPx = childBounds.top - visibleTop
            }

            if (deltaPx != 0f) {
                val target = (scrollState.value + deltaPx).roundToInt()
                    .coerceIn(0, scrollState.maxValue)
                scrollState.animateScrollTo(target)
            }
        }
    }

    val titleFocus = remember { FocusRequester() }
    val artistFocus = remember { FocusRequester() }
    val yearFocus = remember { FocusRequester() }
    val labelFocus = remember { FocusRequester() }
    val catalogFocus = remember { FocusRequester() }
    val barcodeFocus = remember { FocusRequester() }
    val formatFocus = remember { FocusRequester() }

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
                modifier = Modifier
                    .navigationBarsPadding()
                    // Critical: keep CTA visible above the keyboard WITHOUT adding scrollable tail padding.
                    // This increases the bottomBar height when IME is present, and Scaffold will shrink content accordingly.
                    .imePadding(),
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
                    ) { Text("Save Another") }

                    Button(
                        onClick = onSaveAndExit,
                        modifier = Modifier.weight(1f)
                    ) { Text("Find pressings") }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
                // Intentionally no imePadding() here: we avoid the large "blank tail" in the scroll.
                .onGloballyPositioned { containerCoords = it },
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { artistFocus.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocus)
                        .onGloballyPositioned { titleCoords = it }
                        .onFocusChanged { if (it.isFocused) scheduleScrollIntoView { titleCoords } }
                )

                OutlinedTextField(
                    value = state.artist,
                    onValueChange = effectiveOnArtistChange,
                    label = { Text("Artist *") },
                    isError = showValidationErrors && artistError,
                    supportingText = { if (showValidationErrors && artistError) Text("Required") },
                    colors = textFieldColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { yearFocus.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(artistFocus)
                        .onGloballyPositioned { artistCoords = it }
                        .onFocusChanged { if (it.isFocused) scheduleScrollIntoView { artistCoords } }
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
                                            yearFocus.requestFocus()
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
                        keyboardActions = KeyboardActions(onNext = { labelFocus.requestFocus() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(yearFocus)
                            .onGloballyPositioned { yearCoords = it }
                            .onFocusChanged { if (it.isFocused) scheduleScrollIntoView { yearCoords } }
                    )

                    OutlinedTextField(
                        value = state.label,
                        onValueChange = { onStateChange(state.copy(label = it)) },
                        label = { Text("Label") },
                        colors = textFieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { catalogFocus.requestFocus() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(labelFocus)
                            .onGloballyPositioned { labelCoords = it }
                            .onFocusChanged { if (it.isFocused) scheduleScrollIntoView { labelCoords } }
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { barcodeFocus.requestFocus() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(catalogFocus)
                            .onGloballyPositioned { catalogCoords = it }
                            .onFocusChanged { if (it.isFocused) scheduleScrollIntoView { catalogCoords } }
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
                        keyboardActions = KeyboardActions(onNext = { formatFocus.requestFocus() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(barcodeFocus)
                            .onGloballyPositioned { barcodeCoords = it }
                            .onFocusChanged { if (it.isFocused) scheduleScrollIntoView { barcodeCoords } }
                    )
                }

                OutlinedTextField(
                    value = state.format,
                    onValueChange = { onStateChange(state.copy(format = it)) },
                    label = { Text("Format") },
                    colors = textFieldColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(formatFocus)
                        .onGloballyPositioned { formatCoords = it }
                        .onFocusChanged { if (it.isFocused) scheduleScrollIntoView { formatCoords } }
                )
            }

            // Minimal tail so last field never feels cramped.
            Spacer(Modifier.height(8.dp))
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
                Text(text = title, style = MaterialTheme.typography.titleMedium)
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
