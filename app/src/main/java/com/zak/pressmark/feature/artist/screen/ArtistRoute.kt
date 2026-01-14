package com.zak.pressmark.feature.artist.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.artist.components.ArtistAlbumList
import com.zak.pressmark.feature.artist.vm.ArtistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistRoute(
    vm: ArtistViewModel,
    onBack: () -> Unit,
) {
    // vm.albums is canonical (AlbumWithArtistName)
    val albumsWithArtist = vm.albums.collectAsStateWithLifecycle().value

    // Adapter: keep existing ArtistAlbumList working if it still expects AlbumEntity
    val albums = albumsWithArtist.map { it.album }

    val artistName = vm.artistName.collectAsStateWithLifecycle().value ?: "Artist"

    val container = MaterialTheme.colorScheme.background
    val topBarColor = MaterialTheme.colorScheme.primaryContainer

    Surface(color = container) {
        Scaffold(
            containerColor = container,
            topBar = {
                TopAppBar(
                    title = { Text(artistName) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ArtistAlbumList(
                    contentPadding = PaddingValues(0.dp),
                    albums = albums,
                )
            }
        }
    }
}
