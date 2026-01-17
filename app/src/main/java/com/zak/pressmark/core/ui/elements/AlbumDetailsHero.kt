// file: app/src/main/java/com/zak/pressmark/feature/albumdetails/components/AlbumDetailsHero.kt
package com.zak.pressmark.core.ui.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlbumDetailsHero(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    artworkSize: Dp = 220.dp,
    onArtistClick: (() -> Unit)? = null,
    artwork: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Artwork slot: Caller provides the composable to render the image.
        Box(
            modifier = Modifier.size(artworkSize),
            contentAlignment = Alignment.Center,
        ) {
            artwork()
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(4.dp))

        // Applying a clickable modifier only if the callback is provided.
        val artistModifier = if (onArtistClick != null) {
            Modifier.clickable(onClick = onArtistClick)
        } else {
            Modifier
        }

        Text(
            text = artist,
            // Chain the base modifier with the conditional one
            modifier = Modifier
                .fillMaxWidth()
                .then(artistModifier),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp), // titleMedium can be a better semantic fit
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
