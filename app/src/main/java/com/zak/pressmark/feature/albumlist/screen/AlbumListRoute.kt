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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import com.zak.pressmark.feature.albumdetails.components.EditAlbumDialog
import com.zak.pressmark.feature.albumdetails.vm.AlbumDetailsViewModel
import com.zak.pressmark.feature.albumdetails.vm.AlbumDetailsViewModelFactory
import com.zak.pressmark.feature.albumlist.components.AlbumList
import com.zak.pressmark.feature.albumlist.components.TopAppBar
import com.zak.pressmark.feature.albumlist.coversearch.DiscogsCoverSearchDialog
import com.zak.pressmark.feature.albumlist.coversearch.vm.DiscogsCoverSearchViewModel
import com.zak.pressmark.feature.albumlist.coversearch.vm.DiscogsCoverSearchViewModelFactory
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListRoute(
    vm: AlbumListViewModel,
    graph: AppGraph,
    onOpenAlbum: (albumId: String) -> Unit,
    onAddAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ✅ Use the canonical read model (JOINed artist name)
    // Rename this property if your VM uses a different name.
    val albums by vm.albumsWithArtistName.collectAsStateWithLifecycle()

    val uiState by vm.ui.collectAsStateWithLifecycle()

    var rowToSearch by remember { mutableStateOf<AlbumWithArtistName?>(null) }
    var showCoverSearchDialog by remember { mutableStateOf(false) }

    var rowToEdit by remember { mutableStateOf<AlbumWithArtistName?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

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
                onAlbumClick = { row -> onOpenAlbum(row.album.id) },
                onDelete = { row -> vm.deleteAlbum(row.album) },
                onFindCover = { row ->
                    rowToSearch = row
                    showCoverSearchDialog = true},
                onEdit = { row ->
                    rowToEdit = row
                    showEditDialog = true
                },
            )
        }
    }

    if (showCoverSearchDialog) {
        val row = rowToSearch ?: return
        val album = row.album
        val artist = row.artistDisplayName?.trim().orEmpty()

        val factory = remember(graph, album.id, artist, album.title) {
            DiscogsCoverSearchViewModelFactory(graph, album.id, artist, album.title)
        }
        val searchVm: DiscogsCoverSearchViewModel = viewModel(factory = factory)
        val searchState by searchVm.uiState.collectAsStateWithLifecycle()

        DiscogsCoverSearchDialog(
            artist = artist,
            title = album.title,
            results = searchState.results,
            onPick = { result ->
                searchVm.pickResult(result)
                showCoverSearchDialog = false
            },
            onDismiss = {
                showCoverSearchDialog = false
            }
        )
    }
    if (showEditDialog) {
        val row = rowToEdit ?: return
        val albumId = row.album.id

        val factory = remember(graph, albumId) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlbumDetailsViewModel(
                        albumId = albumId,
                        repo = graph.albumRepository,
                        artistRepo = graph.artistRepository,
                    ) as T
                }
            }
        }

        val editVm: AlbumDetailsViewModel = viewModel(
            key = "edit_$albumId",
            factory = factory
        )

        val joined by editVm.album.collectAsStateWithLifecycle()
        val joinedRow = joined ?: return
        val album = joinedRow.album
        val artistDisplay = joinedRow.artistDisplayName?.trim().orEmpty()

        EditAlbumDialog(
            album = album,
            artistDisplayName = artistDisplay,
            format = album.format,
            onDismiss = { showEditDialog = false },
            onSave = { title, artist, year, catalogNo, label, format ->
                // ✅ call the canonical saver (includes artistId resolve + format)
                editVm.saveEdits(title, artist, year, catalogNo, label, format)
                showEditDialog = false
            }
        )
    }
}
