// =======================================================
// file: app/src/main/java/com/zak/pressmark/feature/albumlist/screen/AlbumListRoute.kt
// =======================================================
package com.zak.pressmark.feature.albumlist.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.albumlist.components.AlbumList
import com.zak.pressmark.feature.albumlist.components.TopAppBar
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.feature.albumlist.coversearch.DiscogsCoverSearchDialog
import com.zak.pressmark.feature.albumlist.coversearch.vm.DiscogsCoverSearchViewModel
import com.zak.pressmark.feature.albumlist.coversearch.vm.DiscogsCoverSearchViewModelFactory
import com.zak.pressmark.feature.albumdetails.components.EditAlbumDialog



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListRoute(
    vm: AlbumListViewModel,
    graph: AppGraph,
    onOpenAlbum: (albumId: String) -> Unit,
    onAddAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val albums by vm.albums.collectAsStateWithLifecycle()
    val uiState by vm.ui.collectAsStateWithLifecycle()

    var albumToSearch by remember { mutableStateOf<AlbumEntity?>(null) }
    var showCoverSearchDialog by remember { mutableStateOf(false) }
    var albumToEdit by remember { mutableStateOf<AlbumEntity?>(null) }

    val screenContainerColor = MaterialTheme.colorScheme.background
    val topBarContainerColor = MaterialTheme.colorScheme.primaryContainer

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.snackMessage) {
        val msg = uiState.snackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg)
        vm.dismissSnack()
    }

    Surface(
        modifier = modifier,
        color = screenContainerColor,
    ) {
        Scaffold(
            containerColor = screenContainerColor,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = "Catalog",
                    containerColor = topBarContainerColor,
                    actions = {
                        IconButton(onClick = onAddAlbum) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add album",
                            )
                        }
                    },
                )
            },
        ) { padding ->
            AlbumList(
                contentPadding = padding,
                albums = albums,
                onAlbumClick = { album -> onOpenAlbum(album.id) },
                onDelete = { album -> vm.deleteAlbum(album) },
                onEdit = { album -> albumToEdit = album },
                onFindCover = { album ->
                    showCoverSearchDialog = false
                    albumToSearch = null

                    albumToSearch = album
                    showCoverSearchDialog = true }
            )
        }
    }
    // The dialog is now only shown if the boolean flag is true.
    if (showCoverSearchDialog) {
        val album = albumToSearch!!
        val factory = remember(graph, album) {
            DiscogsCoverSearchViewModelFactory(graph, album.id, album.artist, album.title)
        }
        val searchVm: DiscogsCoverSearchViewModel = viewModel(key = "cover_search_${album.id}", factory = factory)
        val searchState by searchVm.uiState.collectAsStateWithLifecycle()

        DiscogsCoverSearchDialog(
            artist = album.artist,
            title = album.title,
            results = searchState.results,
            onPick = { result ->
                searchVm.pickResult(result)
                showCoverSearchDialog = false // Close dialog
                albumToSearch = null
            },
            onDismiss = {
                showCoverSearchDialog = false // Close dialog
                albumToSearch = null
            }
        )
    }
    val editing = albumToEdit
    if (editing != null) {
        EditAlbumDialog(
            album = editing,
            onDismiss = { albumToEdit = null },
            onSave = { title, artist, year, catalogNo, label ->
                vm.updateAlbum(
                    albumId = editing.id,
                    title = title,
                    artist = artist,
                    releaseYear = year,
                    catalogNo = catalogNo,
                    label = label,
                    //tracklist = tracklist,
                    //notes = notes,
                    onError = { /* optional: vm could set snack, or call snackbar directly */ },
                )
                albumToEdit = null
            }
        )
    }
}
