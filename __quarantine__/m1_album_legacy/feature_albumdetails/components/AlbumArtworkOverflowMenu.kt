// FILE: app/src/main/java/com/zak/pressmark/feature/albumdetails/components/AlbumArtworkOverflowMenu.kt
package com.zak.pressmark.feature.albumdetails.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.ui.elements.AlbumArtwork

@Composable
fun AlbumArtworkOverflowMenu(
    artworkUrl: String?,
    contentDescription: String?,
    onFindCover: () -> Unit,
    onRefreshCover: () -> Unit,
    onClearCover: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    cornerRadius: Dp = 10.dp,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AlbumArtwork(
            artworkUrl = artworkUrl,
            contentDescription = contentDescription,
            size = size,
            cornerRadius = cornerRadius,
        )

        // little "pill" background so the icon stays readable on busy covers
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
            shadowElevation = 2.dp,
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Artwork menu",
                    )
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Find cover") },
                        onClick = {
                            menuOpen = false
                            onFindCover()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Refresh cover") },
                        onClick = {
                            menuOpen = false
                            onRefreshCover()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear cover") },
                        onClick = {
                            menuOpen = false
                            onClearCover()
                        }
                    )
                }
            }
        }
    }
}
