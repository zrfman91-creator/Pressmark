// FILE: app/src/main/java/com/zak/pressmark/feature/artist/route/ArtistRoute.kt
package com.zak.pressmark.feature.artist.route

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.artist.screen.ArtistScreen
import com.zak.pressmark.feature.artist.vm.ArtistViewModel

@Composable
fun ArtistRoute(
    vm: ArtistViewModel,
    onBack: () -> Unit,
) {
    // vm.albums is canonical (AlbumWithArtistName)
    val albumsWithArtist = vm.albums.collectAsStateWithLifecycle().value

    // Adapter: keep existing ArtistAlbumList working (it expects AlbumEntity)
    val albums = albumsWithArtist.map { it.album }

    val artistName = vm.artistName.collectAsStateWithLifecycle().value ?: "Artist"

    ArtistScreen(
        artistName = artistName,
        albums = albums,
        onBack = onBack,
    )
}
