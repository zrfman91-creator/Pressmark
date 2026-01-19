// file: app/src/main/java/com/zak/pressmark/feature/albumlist/screen/AlbumListScreen.kt
package com.zak.pressmark.feature.albumlist.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zak.pressmark.data.local.model.ReleaseListItem
import com.zak.pressmark.feature.albumlist.components.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    releases: List<ReleaseListItem>,
    snackMessage: String?,
    onSnackShown: () -> Unit,
    onAddAlbum: () -> Unit,
    onOpenRelease: (releaseId: String) -> Unit,
    onDelete: (ReleaseListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenContainerColor = MaterialTheme.colorScheme.background
    val topBarContainerColor = MaterialTheme.colorScheme.background

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMessage) {
        val msg = snackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg)
        onSnackShown()
    }

    Surface(
        modifier = modifier,
        color = screenContainerColor,
    ) {
        Scaffold(
            containerColor = screenContainerColor,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = "Catalog",
                    containerColor = topBarContainerColor,
                    actions = {
                        TextButton(onClick = onAddAlbum) {
                            Text("Add Album")
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding),
            ) {
                items(
                    items = releases,
                    key = { it.release.id },
                ) { item ->
                    ReleaseRow(
                        item = item,
                        onClick = { onOpenRelease(item.release.id) },
                        onDelete = { onDelete(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseRow(
    item: ReleaseListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    // Minimal row: title + artist line + year; delete as a trailing action.
    // (Artwork rendering can be added once the list row visual spec is finalized.)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.release.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

            val artistLine = item.artistLine.takeIf { it.isNotBlank() } ?: "—"
            Text(
                text = artistLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            val yearText = item.release.releaseYear?.toString()
            if (!yearText.isNullOrBlank()) {
                Text(
                    text = yearText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        TextButton(onClick = onDelete) {
            Text("Delete")
        }
    }
}
