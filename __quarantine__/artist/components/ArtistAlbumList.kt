// FILE: app/src/main/java/com/zak/pressmark/feature/artist/components/ArtistAlbumList.kt
package com.zak.pressmark.feature.artist.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Artist-screen list:
 * - no command bar
 * - no click
 * - simple, fast, scrollable
 */
@Composable
fun ArtistAlbumList(
    albums: List<AlbumEntity>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        items(
            items = albums,
            key = { it.id },
        ) { album ->
            ArtistAlbumListRow(album = album)
        }
    }
}
