// File: app/src/main/java/com/zak/pressmark/ui/albumlist/components/AlbumArtwork.kt
package com.zak.pressmark.core.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import com.zak.pressmark.core.util.AppImageLoader

private const val DISCOGS_NOT_FOUND_COVER_URI: String = "discogs:not_found"

@Composable
fun AlbumArtwork(
    artworkUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 6.dp,
) {
    val context = LocalContext.current.applicationContext
    val imageLoader = remember(context) { AppImageLoader.get(context) }

    val normalized = artworkUrl?.trim()
    val shape = RoundedCornerShape(cornerRadius)

    val sizePx = with(LocalDensity.current) { size.roundToPx() }

    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant

    if (normalized.isNullOrBlank() || normalized == DISCOGS_NOT_FOUND_COVER_URI) {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(placeholderColor)
        )
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(normalized)
            .size(sizePx, sizePx)
            .precision(Precision.INEXACT)
            .build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(placeholderColor),
    )
}
