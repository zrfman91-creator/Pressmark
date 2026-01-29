@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.feature.library.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
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
 * Expanded-only search field overlay.
 *
 * Overlap behavior:
 * - Android/Compose treats the predictive strip as part of the IME inset.
 * - If you want the field closer to the keys, we "overlap" the IME by subtracting [imeOverlapDp]
 *   from the computed keyboard lift.
 *
 * Tune [imeOverlapDp] for the device/keyboard (Gboard typically ~32–48dp).
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
    height: Dp = 56.dp,
    horizontalPadding: Dp = 6.dp,
    expandedKeyboardGap: Dp = 6.dp,

    // ✅ New: how much to "push down" into the IME (overlap predictive strip)
    imeOverlapDp: Dp = 76.dp,
) {
    if (!expanded) return

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Manual inset math (tracks keyboard in adjustNothing)
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val barsBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val keyboardLift = (imeBottom - barsBottom).coerceAtLeast(0.dp)

    // ✅ Overlap: reduce the lift so the field sits lower (closer to keys)
    val overlappedLift = (keyboardLift - imeOverlapDp).coerceAtLeast(0.dp)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val targetWidth = (maxWidth - (horizontalPadding * 2)).coerceAtLeast(240.dp)
        val animatedWidth by animateDpAsState(targetValue = targetWidth, label = "searchWidth")

        // Outside-tap catcher (collapses only; does NOT clear query)
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = overlappedLift + expandedKeyboardGap,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Slight left shift so it reads as "sliding out" from the right edge.

            Surface(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(height)
                    // Consume clicks so outside layer doesn't collapse while interacting inside.
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { },
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                        interactionSource = remember { MutableInteractionSource() },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                        ),
                    )

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (query.isNotEmpty()) onClear()
                            else {
                                focusManager.clearFocus(force = true)
                                onExpandedChange(false)
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}
