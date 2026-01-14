package com.zak.pressmark.feature.albumdetails.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.zak.pressmark.feature.albumdetails.components.AlbumArtworkOverflowMenu
import com.zak.pressmark.feature.albumdetails.components.AlbumDetailsHero
import com.zak.pressmark.feature.albumdetails.components.AlbumDetailsInfoSection
import com.zak.pressmark.feature.albumdetails.components.AlbumDetailsMetaRow
import com.zak.pressmark.feature.albumdetails.components.AlbumDetailsNotesSection
import com.zak.pressmark.data.local.model.AlbumWithArtistName


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailsScreen(
    row: AlbumWithArtistName?,
    snackMessage: String?,
    onSnackShown: () -> Unit,
    didDelete: Boolean,
    onDeletedNavigatedAway: () -> Unit,
    onBack: () -> Unit,
    onOpenArtist: (artistId: Long) -> Unit,
    onOpenEdit: () -> Unit,
    onOpenDeleteConfirm: () -> Unit,
    onRefreshCover: () -> Unit,
    onClearCover: () -> Unit,
    onSetDiscogsCover: (coverUrl: String?, discogsReleaseId: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var topBarMenuOpen by rememberSaveable { mutableStateOf(false) }

    // Local cover URL dialog UI state (belongs to Screen)
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
                    title = { Text("Album") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { topBarMenuOpen = true },
                            enabled = row != null
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
            if (row == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Album not found",
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

            val a = row.album
            val artistName = row.artist
            val artistId = a.artistId

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AlbumDetailsHero(
                    title = a.title,
                    artist = artistName,
                    onArtistClick = artistId?.let { { onOpenArtist(it) } },
                ) {
                    AlbumArtworkOverflowMenu(
                        artworkUrl = a.coverUri,
                        contentDescription = "$artistName — ${a.title}",
                        size = 260.dp,
                        cornerRadius = 10.dp,
                        onFindCover = { showCoverUrlDialog = true },
                        onRefreshCover = onRefreshCover,
                        onClearCover = onClearCover,
                    )
                }

                AlbumDetailsMetaRow(
                    year = a.releaseYear,
                    label = a.label,
                    catalogNo = a.catalogNo,
                )

                HorizontalDivider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )

                AlbumDetailsInfoSection(
                    year = a.releaseYear,
                    label = a.label,
                    catalogNo = a.catalogNo,
                    format = a.format,
                )

                AlbumDetailsNotesSection(notes = null)
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
