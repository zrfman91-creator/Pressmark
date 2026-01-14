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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.albumdetails.components.AlbumArtworkOverflowMenu
import com.zak.pressmark.feature.albumdetails.components.AlbumDetailsHero
import com.zak.pressmark.feature.albumdetails.components.AlbumDetailsInfoSection
import com.zak.pressmark.feature.albumdetails.components.AlbumDetailsMetaRow
import com.zak.pressmark.feature.albumdetails.components.AlbumDetailsNotesSection
import com.zak.pressmark.feature.albumdetails.components.EditAlbumDialog
import com.zak.pressmark.feature.albumdetails.vm.AlbumDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailsRoute(
    vm: AlbumDetailsViewModel,
    onBack: () -> Unit,
    onOpenArtist: (artistId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val row by vm.album.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var topBarMenuOpen by rememberSaveable { mutableStateOf(false) }

    var showCoverUrlDialog by rememberSaveable { mutableStateOf(false) }
    var coverUrlText by rememberSaveable { mutableStateOf("") }
    var releaseIdText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(ui.snackMessage) {
        val msg = ui.snackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        vm.dismissSnack()
    }

    LaunchedEffect(ui.didDelete) {
        if (ui.didDelete) onBack()
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
                                    vm.openEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    topBarMenuOpen = false
                                    vm.openDeleteConfirm()
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

            val a = row!!.album
            val artistName = row!!.artist

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val artistId = a.artistId

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
                        onRefreshCover = { vm.refreshDiscogsCover() },
                        onClearCover = { vm.clearCover() },
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
                    format = a.format
                )

                AlbumDetailsNotesSection(notes = null)
            }
        }
    }

    if (ui.editOpen && row != null) {
        val a = row!!.album
        val artistName = row!!.artist

        EditAlbumDialog(
            album = a,
            artistDisplayName = artistName,
            format = a.format, // ✅ FIX: pass format
            onDismiss = { vm.closeEdit() },
            onSave = { title, artistNameEdited, year, catalogNo, label, format ->
                vm.saveEdits(
                    title = title,
                    artist = artistNameEdited,
                    releaseYear = year,
                    catalogNo = catalogNo,
                    label = label,
                    format = format,
                )
            },
        )
    }

    if (ui.deleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = { vm.closeDeleteConfirm() },
            title = { Text("Delete album?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteAlbum() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { vm.closeDeleteConfirm() }) { Text("Cancel") }
            },
        )
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

                        vm.setDiscogsCover(
                            coverUrl = url,
                            discogsReleaseId = rid,
                        )

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
