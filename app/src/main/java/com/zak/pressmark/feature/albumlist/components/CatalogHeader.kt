// File: app/src/main/java/com/zak/pressmark/feature/albumlist/components/CatalogHeader.kt
package com.zak.pressmark.feature.albumlist.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zak.pressmark.core.ui.elements.CollapsibleSearchBar

/**
 * Catalog header (TopAppBar + CollapsibleSearchBar).
 *
 * Contract:
 * - Search icon toggles expanded/collapsed.
 * - Back collapses search first when expanded.
 * - Closing search does NOT clear query.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogHeader(
    title: String,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onOpenControls: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search title, artist, cat#...",
) {
    BackHandler(enabled = searchExpanded) {
        onSearchExpandedChange(false)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = { Text(title) },
            actions = {
                IconButton(onClick = { onSearchExpandedChange(!searchExpanded) }) {
                    AnimatedContent(
                        targetState = searchExpanded,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "CatalogSearchIcon",
                    ) { expanded ->
                        if (expanded) {
                            Icon(Icons.Filled.Close, contentDescription = "Close search")
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                }

                IconButton(onClick = onOpenControls) {
                    Icon(Icons.Filled.FilterList, contentDescription = "Sort and filter")
                }

                IconButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = "Add album")
                }
            }
        )

        CollapsibleSearchBar(
            expanded = searchExpanded,
            query = query,
            onQueryChange = onQueryChange,
            onClearQuery = onClearQuery,
            onClose = { onSearchExpandedChange(false) },
            placeholder = placeholder,
        )
    }
}
