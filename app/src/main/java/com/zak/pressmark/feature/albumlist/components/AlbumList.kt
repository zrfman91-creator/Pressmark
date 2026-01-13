// =======================================================
// file: app/src/main/java/com/zak/pressmark/ui/albumlist/components/AlbumList.kt
// =======================================================
package com.zak.pressmark.feature.albumlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.theme.AppTypography
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.feature.albumlist.format.ArtistNameFormatter
import kotlinx.coroutines.delay
import java.util.Locale

private enum class AlbumSort { ARTIST_AZ, TITLE_AZ, YEAR_DESC }

private fun String.norm(): String = trim().lowercase(Locale.ROOT)

private val SortOptions = listOf(
    CommandOption(id = "artist", label = "Artist (A–Z)"),
    CommandOption(id = "title", label = "Title (A–Z)"),
    CommandOption(id = "year", label = "Year (desc)"),
)

private val DensityOptions = listOf(
    CommandOption(id = "spacious", label = "Spacious"),
    CommandOption(id = "standard", label = "Standard"),
    CommandOption(id = "compact", label = "Compact"),
    CommandOption(id = "text_only", label = "Text Only"),
)

@Composable
fun AlbumList(
    contentPadding: PaddingValues,
    albums: List<AlbumEntity>,
    onAlbumClick: (AlbumEntity) -> Unit,
    onDelete: (AlbumEntity) -> Unit,
    onFindCover: (AlbumEntity) -> Unit,
    onEdit: (AlbumEntity) -> Unit,
    modifier: Modifier = Modifier,
    selectedAlbumId: String? = null,
    selectionEnabled: Boolean = true,
    dividerInset: Dp = 8.dp,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(AlbumSort.ARTIST_AZ) }
    var density by rememberSaveable { mutableStateOf(AlbumRowDensity.STANDARD) }

    var selectedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    val selectionActive = selectionEnabled && selectedIds.isNotEmpty()

    val filteredSorted = remember(albums, query, sort) {
        val q = query.trim()
        val filtered = if (q.isBlank()) {
            albums
        } else {
            albums.filter { a ->
                a.title.contains(q, ignoreCase = true) || a.artist.contains(q, ignoreCase = true)
            }
        }

        when (sort) {
            AlbumSort.ARTIST_AZ ->
                filtered.sortedWith(
                    compareBy<AlbumEntity> { ArtistNameFormatter.sortKeyForList(it.artist) }
                        .thenBy { it.title.norm() }
                )

            AlbumSort.TITLE_AZ ->
                filtered.sortedWith(
                    compareBy<AlbumEntity> { it.title.norm() }
                        .thenBy { ArtistNameFormatter.sortKeyForList(it.artist) }
                )

            AlbumSort.YEAR_DESC ->
                filtered.sortedWith(
                    compareByDescending<AlbumEntity> { it.releaseYear ?: Int.MIN_VALUE }
                        .thenBy { ArtistNameFormatter.sortKeyForList(it.artist) }
                        .thenBy { it.title.norm() }
                )
        }
    }

    val countText = if (query.isBlank()) "${albums.size}" else "${filteredSorted.size} results"

    val sortText = when (sort) {
        AlbumSort.ARTIST_AZ -> "Artist"
        AlbumSort.TITLE_AZ -> "Title"
        AlbumSort.YEAR_DESC -> "Year"
    }

    val densityText = when (density) {
        AlbumRowDensity.SPACIOUS -> "Spacious"
        AlbumRowDensity.STANDARD -> "Standard"
        AlbumRowDensity.COMPACT -> "Compact"
        AlbumRowDensity.TEXT_ONLY -> "Text Only"
    }

    val selectedSortId = when (sort) {
        AlbumSort.ARTIST_AZ -> "artist"
        AlbumSort.TITLE_AZ -> "title"
        AlbumSort.YEAR_DESC -> "year"
    }

    val selectedDensityId = when (density) {
        AlbumRowDensity.SPACIOUS -> "spacious"
        AlbumRowDensity.STANDARD -> "standard"
        AlbumRowDensity.COMPACT -> "compact"
        AlbumRowDensity.TEXT_ONLY -> "text_only"
    }

    // ✅ Styling knobs (paper-safe, no tint overlays here)
    val screenBg = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(top = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AlbumCommandBar(
                query = query,
                onQueryChange = { query = it },
                countText = countText,
                sortText = sortText,
                densityText = densityText,
                sortOptions = SortOptions,
                selectedSortId = selectedSortId,
                onSortSelected = { id ->
                    sort = when (id) {
                        "artist" -> AlbumSort.ARTIST_AZ
                        "title" -> AlbumSort.TITLE_AZ
                        "year" -> AlbumSort.YEAR_DESC
                        else -> sort
                    }
                },
                densityOptions = DensityOptions,
                selectedDensityId = selectedDensityId,
                onDensitySelected = { id ->
                    density = when (id) {
                        "spacious" -> AlbumRowDensity.SPACIOUS
                        "standard" -> AlbumRowDensity.STANDARD
                        "compact" -> AlbumRowDensity.COMPACT
                        "text_only" -> AlbumRowDensity.TEXT_ONLY
                        else -> density
                    }
                },
                selectionActive = selectionActive,
                selectionBar = {
                    if (!selectionActive) return@AlbumCommandBar
                    SelectionBar(
                        selectedCount = selectedIds.size,
                        onClear = { selectedIds = emptySet() },
                        onSelectAll = { selectedIds = filteredSorted.map { it.id }.toSet() },
                        onDeleteSelected = {
                            val byId = albums.associateBy { it.id }
                            selectedIds.forEach { id -> byId[id]?.let(onDelete) }
                            selectedIds = emptySet()
                        },
                        onRefreshArtworkSelected = {
                            val byId = albums.associateBy { it.id }
                            selectedIds.forEach { id -> byId[id]?.let{/*   */} }
                        },
                    )
                },
            )

            HorizontalDivider(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 4.dp)
                    .padding(horizontal = dividerInset),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
            )

            if (filteredSorted.isEmpty()) {
                EmptyState(
                    query = query,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    itemsIndexed(
                        items = filteredSorted,
                        key = { _, a -> a.id },
                    ) { index, album ->
                        val normalizedCover = album.coverUri?.trim().takeIf { !it.isNullOrBlank() }
                        val shouldAutoFetch = normalizedCover == null && album.discogsReleaseId == null

                       /* LaunchedEffect(album.id, shouldAutoFetch) {
                            if (!shouldAutoFetch) return@LaunchedEffect
                            delay(350)
                            onRequestArtwork(album)
                        }*/

                        val isSelected =
                            (selectionEnabled && selectedIds.contains(album.id)) || (selectedAlbumId == album.id)

                        AlbumListRow(
                            album = album,
                            isSelected = isSelected,
                            selectionActive = selectionActive,
                            onClick = {
                                if (selectionActive) selectedIds = toggleId(selectedIds, album.id)
                                else onAlbumClick(album)
                            },
                            onLongPress = {
                                if (!selectionEnabled) return@AlbumListRow
                                selectedIds = toggleId(selectedIds, album.id)
                            },
                            onDelete = { onDelete(album) },
                            onEdit = { onEdit(album) },
                            onFindCover = { onFindCover(album) },
                            showDivider = index != filteredSorted.lastIndex,
                            rowDensity = density,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onRefreshArtworkSelected: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onSelectAll) {
            Icon(Icons.Filled.SelectAll, contentDescription = "Select filtered")
        }
        IconButton(onClick = onRefreshArtworkSelected) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh artwork for selected")
        }
        IconButton(onClick = onDeleteSelected) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
        }
        IconButton(onClick = onClear) {
            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
        }
    }
}

@Composable
private fun EmptyState(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(top = 72.dp),
            text = if (query.isBlank()) "No albums" else "No results",
            style = AppTypography.titleLarge,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (query.isBlank()) "Add albums to build your library." else "Try a different search term.",
            style = AppTypography.bodyMedium,
        )
    }
}

private fun toggleId(current: Set<String>, id: String): Set<String> =
    if (current.contains(id)) current - id else current + id
