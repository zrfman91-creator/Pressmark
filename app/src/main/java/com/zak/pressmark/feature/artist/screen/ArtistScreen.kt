// FILE: app/src/main/java/com/zak/pressmark/feature/artist/screen/ArtistScreen.kt
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
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.feature.artist.components.ArtistAlbumList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artistName: String,
    albums: List<AlbumEntity>,
    onBack: () -> Unit,
) {
    val container = MaterialTheme.colorScheme.background
    val topBarColor = MaterialTheme.colorScheme.primaryContainer

    Surface(color = container) {
        Scaffold(
            containerColor = container,
            topBar = {
                TopAppBar(
                    title = { Text(artistName.ifBlank { "Artist" }) },
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
