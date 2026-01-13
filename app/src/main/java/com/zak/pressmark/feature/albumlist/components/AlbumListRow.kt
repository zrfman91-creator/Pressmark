// =======================================================
// file: app/src/main/java/com/zak/pressmark/ui/albumlist/components/AlbumListRow.kt
// =======================================================
package com.zak.pressmark.feature.albumlist.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.feature.albumlist.format.ArtistNameFormatter

enum class AlbumRowDensity { SPACIOUS, STANDARD, COMPACT, TEXT_ONLY }

@Composable
fun AlbumListRow(
    album: AlbumEntity,
    isSelected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    showDivider: Boolean,
    onFindCover: () -> Unit,
    modifier: Modifier = Modifier,
    rowDensity: AlbumRowDensity = AlbumRowDensity.SPACIOUS,
) {
    val spec = remember(rowDensity) { RowSpec.from(rowDensity) }
    var menuOpen by remember { mutableStateOf(false) }

    val hasArtwork = !album.persistedArtworkUrl.isNullOrBlank()
    val showRefreshAction = !hasArtwork && album.discogsReleaseId != null

    val artistDisplay = remember(album.artist) { ArtistNameFormatter.displayForList(album.artist) }

    // ✅ Row background colors (change these to match your “paper” look)
    val rowContainerColor =
        if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer

    // ✅ Dropdown menu background
    val menuContainerColor = MaterialTheme.colorScheme.secondaryContainer

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Surface(
            color = rowContainerColor,
            shape = RoundedCornerShape(6.dp),
            shadowElevation = 1.dp,  // adjust if you want subtle depth
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spec.outerVerticalPadding)
                .combinedClickableRow(onClick = onClick, onLongPress = onLongPress)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spec.horizontalPadding, vertical = spec.verticalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rowDensity != AlbumRowDensity.TEXT_ONLY) {
                    AlbumArtwork(
                        artworkUrl = album.persistedArtworkUrl,
                        contentDescription = "${album.artist} — ${album.title}",
                        modifier = Modifier.size(spec.artworkSize)
                    )
                    Spacer(Modifier.width(spec.gapAfterLeading))
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (rowDensity == AlbumRowDensity.TEXT_ONLY) {
                        Text(
                            text = buildTextOnlyLine(
                                title = album.title,
                                artistDisplay = artistDisplay,
                                year = album.releaseYear
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = artistDisplay,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        buildMetaLine(album)?.let { meta ->
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!selectionActive && album.discogsReleaseId == -1L) {
                            Text(
                                text = "Artwork not found",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Row options")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = menuContainerColor, // ✅ menu background
                        shadowElevation = 6.dp,
                        shape = MaterialTheme.shapes.large)
                    {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenuItem(
                            text = { Text("Find cover") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ImageSearch,
                                    contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                onFindCover()
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant))

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                }
            }
        }
    }
        if (showDivider) {
            LeftFadeDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spec.dividerStartPadding)
                    .padding(vertical = 1.dp),
                fadeEndFraction = 0.75f,
                alpha = 0.85f,
                thickness = 1.dp
            )
        }
    }


@Immutable
private data class RowSpec(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val artworkSize: Dp,
    val gapAfterLeading: Dp,
    val dividerStartPadding: Dp,
    val outerVerticalPadding: Dp,
) {
    companion object {
        fun from(density: AlbumRowDensity): RowSpec =
            when (density) {
                AlbumRowDensity.SPACIOUS -> RowSpec(
                    horizontalPadding = 12.dp,
                    verticalPadding = 10.dp,
                    artworkSize = 72.dp,
                    gapAfterLeading = 12.dp,
                    dividerStartPadding = 12.dp,
                    outerVerticalPadding = 2.dp
                )

                AlbumRowDensity.STANDARD -> RowSpec(
                    horizontalPadding = 12.dp,
                    verticalPadding = 8.dp,
                    artworkSize = 56.dp,
                    gapAfterLeading = 12.dp,
                    dividerStartPadding = 12.dp,
                    outerVerticalPadding = 1.dp
                )

                AlbumRowDensity.COMPACT -> RowSpec(
                    horizontalPadding = 12.dp,
                    verticalPadding = 6.dp,
                    artworkSize = 36.dp,
                    gapAfterLeading = 12.dp,
                    dividerStartPadding = 12.dp,
                    outerVerticalPadding = 0.dp
                )

                AlbumRowDensity.TEXT_ONLY -> RowSpec(
                    horizontalPadding = 12.dp,
                    verticalPadding = 0.dp,
                    artworkSize = 0.dp,
                    gapAfterLeading = 0.dp,
                    dividerStartPadding = 12.dp,
                    outerVerticalPadding = 0.dp
                )
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableRow(
    onClick: () -> Unit,
    onLongPress: () -> Unit,
): Modifier = combinedClickable(onClick = onClick, onLongClick = onLongPress)

@Composable
private fun buildTextOnlyLine(
    title: String,
    artistDisplay: String,
    year: Int?
): AnnotatedString {
    val base = MaterialTheme.typography.bodyMedium
    val on = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val titleStyle = SpanStyle(
        fontWeight = FontWeight.SemiBold,
        color = on,
        fontSize = base.fontSize
    )
    val metaStyle = SpanStyle(
        color = muted,
        fontSize = base.fontSize
    )

    return buildAnnotatedString {
        withStyle(titleStyle) { append(title) }
        append(" \u2022 ")
        withStyle(metaStyle) { append(artistDisplay) }
        if (year != null) {
            append(" \u2022 ")
            withStyle(metaStyle) { append(year.toString()) }
        }
    }
}

private fun buildMetaLine(album: AlbumEntity): String? {
    val year = album.releaseYear?.takeIf { it > 0 }?.toString()
    val label = album.label?.trim()?.takeIf { it.isNotBlank() }
    val catalog = album.catalogNo?.trim()?.takeIf { it.isNotBlank() }
    val parts = listOfNotNull(year, label, catalog)
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

@Composable
private fun LeftFadeDivider(
    modifier: Modifier = Modifier,
    fadeEndFraction: Float,
    alpha: Float,
    thickness: Dp,
) {
    val c = MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)
    val end = fadeEndFraction.coerceIn(0.05f, 1f)

    Box(
        modifier = modifier
            .height(thickness)
            .background(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.00f to c,
                        end to Color.Transparent,
                        1.00f to Color.Transparent
                    )
                )
            )
    )
}
