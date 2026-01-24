package com.zak.pressmark.feature.releasedetails.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.ui.elements.AlbumArtwork
import com.zak.pressmark.core.ui.elements.AlbumDetailsHero
import com.zak.pressmark.data.model.ReleaseDetails
import com.zak.pressmark.data.model.ReleaseDiscogsExtras
import com.zak.pressmark.data.model.ReleaseMarketPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseDetailsScreen(
    details: ReleaseDetails?,
    discogsExtras: ReleaseDiscogsExtras?,
    snackMessage: String?,
    onSnackShown: () -> Unit,
    didDelete: Boolean,
    onDeletedNavigatedAway: () -> Unit,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenDeleteConfirm: () -> Unit,
    onClearCover: () -> Unit,
    onSetDiscogsCover: (coverUrl: String?, discogsReleaseId: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var topBarMenuOpen by rememberSaveable { mutableStateOf(false) }
    var showCoverUrlDialog by rememberSaveable { mutableStateOf(false) }
    var coverUrlText by rememberSaveable { mutableStateOf("") }
    var releaseIdText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(snackMessage) {
        snackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onSnackShown()
        }
    }

    LaunchedEffect(didDelete) {
        if (didDelete) onDeletedNavigatedAway()
    }

    val container = MaterialTheme.colorScheme.background
    val topBarColor = MaterialTheme.colorScheme.primaryContainer

    Surface(
        modifier = modifier.fillMaxSize(),
        color = container,
    ) {
        Scaffold(
            containerColor = container,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Release") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { topBarMenuOpen = true },
                            enabled = details != null
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = topBarMenuOpen,
                            onDismissRequest = { topBarMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    topBarMenuOpen = false
                                    onOpenEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set cover URL") },
                                onClick = {
                                    topBarMenuOpen = false
                                    showCoverUrlDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear cover") },
                                onClick = {
                                    topBarMenuOpen = false
                                    onClearCover()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    topBarMenuOpen = false
                                    onOpenDeleteConfirm()
                                }
                            )
                        }
                    }
                )
            },
        ) { padding ->
            if (details == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Release not found",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "It may have been deleted or the ID is invalid.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = onBack) { Text("Back") }
                }
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AlbumDetailsHero(
                    title = details.title,
                    artist = details.artistLine.ifBlank { "Unknown Artist" },
                ) {
                    AlbumArtwork(
                        artworkUrl = details.artwork?.uri,
                        contentDescription = "${details.artistLine} — ${details.title}",
                        size = 260.dp,
                        cornerRadius = 10.dp,
                    )
                }

                ReleaseMetaRow(details = details)

                HorizontalDivider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )

                ReleaseInfoSection(details = details, discogsExtras = discogsExtras)

                ReleaseDiscogsSection(extras = discogsExtras)

                details.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    ReleaseNotesSection(notes = notes)
                }
            }
        }
    }

    if (showCoverUrlDialog) {
        AlertDialog(
            onDismissRequest = { showCoverUrlDialog = false },
            title = { Text("Set cover from URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = coverUrlText,
                        onValueChange = { coverUrlText = it },
                        label = { Text("Cover URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = releaseIdText,
                        onValueChange = { releaseIdText = it },
                        label = { Text("Discogs Release ID (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = coverUrlText.trim().takeIf { it.isNotBlank() }
                        val rid = releaseIdText.trim().takeIf { it.isNotBlank() }?.toLongOrNull()
                        onSetDiscogsCover(url, rid)
                        showCoverUrlDialog = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCoverUrlDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ReleaseMetaRow(details: ReleaseDetails) {
    val meta = buildList {
        details.releaseYear?.let { add(it.toString()) }
        details.label?.takeIf { it.isNotBlank() }?.let { add(it) }
        details.catalogNo?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.joinToString(" • ")

    if (meta.isNotBlank()) {
        Text(
            text = meta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReleaseInfoSection(
    details: ReleaseDetails,
    discogsExtras: ReleaseDiscogsExtras?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InfoRow(label = "Format", value = details.format)
        InfoRow(label = "Barcode", value = details.barcode)
        InfoRow(label = "Country", value = details.country)
        InfoRow(label = "Release type", value = details.releaseType)
        val genres = discogsExtras?.genres?.joinToString(", ")?.takeIf { it.isNotBlank() }
        val styles = discogsExtras?.styles?.joinToString(", ")?.takeIf { it.isNotBlank() }
        InfoRow(label = "Genres", value = genres)
        InfoRow(label = "Styles", value = styles)

        if (details.credits.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Credits",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            details.credits.forEach { credit ->
                val label = credit.role.ifBlank { "Credit" }
                InfoRow(label = label, value = credit.artistName)
            }
        }
    }
}

@Composable
private fun ReleaseDiscogsSection(extras: ReleaseDiscogsExtras?) {
    val lastSoldDate = extras?.lastSoldDate?.takeIf { it.isNotBlank() }
    val lowest = extras?.lowestPrice?.formatPrice()
    val median = extras?.medianPrice?.formatPrice()
    val highest = extras?.highestPrice?.formatPrice()

    if (lastSoldDate == null && lowest == null && median == null && highest == null) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Discogs market",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InfoRow(label = "Last sold", value = lastSoldDate)
        InfoRow(label = "Low", value = lowest)
        InfoRow(label = "Median", value = median)
        InfoRow(label = "High", value = highest)
    }
}

@Composable
private fun ReleaseNotesSection(notes: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Notes",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = notes,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String?,
) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun ReleaseMarketPrice.formatPrice(): String? {
    if (currency.isBlank()) return null
    return "%s %.2f".format(currency, value)
}
