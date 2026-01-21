// =======================================================
// FILE: app/src/main/java/com/zak/pressmark/feature/albumlist/components/AlbumListRow.kt
// =======================================================
package com.zak.pressmark.feature.catalog.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.ui.elements.AlbumArtwork
import com.zak.pressmark.data.local.model.AlbumWithArtistName

enum class AlbumRowDensity { SPACIOUS, STANDARD, COMPACT, TEXT_ONLY }

private fun AlbumWithArtistName.artistDisplaySafe(): String =
    artistDisplayName?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown Artist"

@Composable
fun AlbumListRow(
    row: AlbumWithArtistName,
    isSelected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    showDivider: Boolean,
    onFindCover: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    rowDensity: AlbumRowDensity = AlbumRowDensity.STANDARD,
) {
    val album = row.album
    val spec = remember(rowDensity) { RowSpec.from(rowDensity) }
    var menuOpen by remember { mutableStateOf(false) }

    val artistDisplay =
        remember(row.artistDisplayName, row.artistSortName) { row.artistDisplaySafe() }

    val rowContainerColor =
        if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer

    val menuContainerColor = MaterialTheme.colorScheme.primaryContainer

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Surface(
            color = rowContainerColor,
            shape = RoundedCornerShape(6.dp),
            shadowElevation = 1.dp,
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
                        contentDescription = "$artistDisplay — ${album.title}",
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

                        buildMetaLine(row)?.let { meta ->
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!selectionActive && album.artworkNotFound) {
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
                        containerColor = menuContainerColor,
                        shadowElevation = 6.dp,
                        shape = MaterialTheme.shapes.large
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.EditNote,
                                    contentDescription = null
                                )
                            },
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
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onFindCover()
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
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
                    outerVerticalPadding = 2.dp
                )

                AlbumRowDensity.COMPACT -> RowSpec(
                    horizontalPadding = 12.dp,
                    verticalPadding = 6.dp,
                    artworkSize = 36.dp,
                    gapAfterLeading = 12.dp,
                    dividerStartPadding = 12.dp,
                    outerVerticalPadding = 1.dp
                )

                AlbumRowDensity.TEXT_ONLY -> RowSpec(
                    horizontalPadding = 12.dp,
                    verticalPadding = 2.dp,
                    artworkSize = 0.dp,
                    gapAfterLeading = 0.dp,
                    dividerStartPadding = 12.dp,
                    outerVerticalPadding = 1.dp
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

private fun buildMetaLine(row: AlbumWithArtistName): String? {
    val album = row.album
    val year = album.releaseYear?.takeIf { it > 0 }?.toString()
    val label = album.label?.trim()?.takeIf { it.isNotBlank() }
    val catalog = album.catalogNo?.trim()?.takeIf { it.isNotBlank() }
    val parts = listOfNotNull(year, label, catalog)
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}
