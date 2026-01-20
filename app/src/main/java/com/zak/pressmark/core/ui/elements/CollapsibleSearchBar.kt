// FILE: app/src/main/java/com/zak/pressmark/core/ui/elements/search/CollapsibleSearchBar.kt
package com.zak.pressmark.core.ui.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * A reusable search bar that can be shown/hidden with a "down + unroll-left" style animation.
 *
 * Contract (as agreed):
 * - Close collapses UI but DOES NOT clear [query]
 * - Clear only clears [query] but keeps UI open
 * - Query ownership is external (typically VM)
 * - Expanded ownership is external (typically screen UI state)
 */
@Composable
fun CollapsibleSearchBar(
    expanded: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search title, artist, cat#…",
    requestFocusOnExpand: Boolean = true,
    onImeSearch: (() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Focus + keyboard polish (kept simple, tweak later if needed)
    LaunchedEffect(expanded, requestFocusOnExpand) {
        if (expanded && requestFocusOnExpand) {
            focusRequester.requestFocus()
        } else if (!expanded) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    AnimatedVisibility(
        visible = expanded,
        modifier = modifier,
        enter = slideInVertically(
            // Starts slightly above, then settles down
            initialOffsetY = { -it / 3 }
        ) + expandHorizontally(
            // "Unroll" from right to left
            expandFrom = Alignment.End
        ) + fadeIn(),
        exit = shrinkHorizontally(
            shrinkTowards = Alignment.End
        ) + fadeOut() + slideOutVertically(
            targetOffsetY = { -it / 4 }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text(placeholder) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search text",
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        // If the caller wants to treat IME Search specially, let them.
                        onImeSearch?.invoke()
                        // Keep keyboard; caller can close if desired.
                    }
                ),
            )

            // Close-search button (collapses UI, preserves query)
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close search",
                )
            }
        }
    }
}
