package com.zak.pressmark.feature.albumlist.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import com.zak.pressmark.feature.albumlist.components.AlbumList
import com.zak.pressmark.feature.albumlist.components.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    albums: List<AlbumWithArtistName>,
    snackMessage: String?,
    onSnackShown: () -> Unit,
    onAddAlbum: () -> Unit,
    onOpenAlbum: (albumId: String) -> Unit,
    onDelete: (AlbumWithArtistName) -> Unit,
    onFindCover: (AlbumWithArtistName) -> Unit,
    onEdit: (AlbumWithArtistName) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenContainerColor = MaterialTheme.colorScheme.background
    val topBarContainerColor = MaterialTheme.colorScheme.background

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        val msg = snackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg)
        onSnackShown()
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
                        TextButton(onClick = onAddAlbum) {
                            Text("Add Album")
                        }
                    },
                )
            },
        ) { padding ->
            AlbumList(
                contentPadding = padding,
                albums = albums,
                onAlbumClick = { row -> onOpenAlbum(row.album.id) },
                onDelete = { row -> onDelete(row) },
                onFindCover = { row -> onFindCover(row) },
                onEdit = { row -> onEdit(row) },
            )
        }
    }
}
