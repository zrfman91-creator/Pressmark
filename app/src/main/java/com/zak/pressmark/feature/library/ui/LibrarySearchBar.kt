@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.feature.library.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Expanding FAB -> search bar anchored bottom-end.
 * Collapsing via outside-tap does NOT clear query. Only onClear() clears.
 *
 * Keyboard behavior: when expanded, the pill sits just above the IME via imePadding().
 */
@Composable
fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search library…",
    fabSize: Dp = 56.dp,
    height: Dp = 56.dp,
    horizontalPadding: Dp = 16.dp,
    bottomPadding: Dp = 16.dp,
    scaffoldBottomPadding: Dp = 16.dp,
    keyboardGap: Dp = 2.dp,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val textFieldInteractionSource = remember { MutableInteractionSource() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val targetWidth = if (expanded) {
            (maxWidth - (horizontalPadding * 2)).coerceAtLeast(fabSize)
        } else {
            fabSize
        }

        val animatedWidth by animateDpAsState(targetValue = targetWidth, label = "searchFabWidth")

        // Outside-tap catcher (collapses only; does NOT clear query)
        if (expanded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus(force = true)
                        onExpandedChange(false)
                    }
            )
        }

        // Key line: imePadding() is applied ONLY while expanded so the bar sits above the keyboard.
        val anchorModifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding)
            // When expanded: sit above IME + small gap (ignore scaffold bottom bar padding)
            .then(if (expanded) Modifier.imePadding() else Modifier)
            .then(if (expanded) Modifier.padding(bottom = keyboardGap) else Modifier.padding(bottom = scaffoldBottomPadding + bottomPadding))


        Box(
            modifier = anchorModifier,
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(height)
                    // Consume clicks so outside layer doesn't collapse while interacting inside.
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { },
                shape = RoundedCornerShape(1.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                shadowElevation = 4.dp
            ) {
                if (!expanded) {
                    val containerColor = MaterialTheme.colorScheme.primary
                    val contentColor = MaterialTheme.colorScheme.onPrimary
                    val elevation = FloatingActionButtonDefaults.elevation(4.dp)


                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FloatingActionButton(
                            onClick = { onExpandedChange(true) },
                            containerColor = containerColor,
                            contentColor = contentColor,
                            elevation = elevation,
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))

                        TextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = { Text(placeholder) },
                            singleLine = true,
                            interactionSource = textFieldInteractionSource,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                            )
                        )

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (query.isNotEmpty()) onClear()
                                else {
                                    focusManager.clearFocus(force = true)
                                    onExpandedChange(false)
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }
            }

            LaunchedEffect(expanded) {
                if (expanded) focusRequester.requestFocus()
            }
        }
    }
}
