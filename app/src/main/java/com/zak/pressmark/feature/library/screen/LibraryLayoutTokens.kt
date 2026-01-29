package com.zak.pressmark.feature.library.screen

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for Library screen layout spacing.
 *
 * Keep these values stable and adjust intentionally to avoid padding/inset regressions.
 */
object LibraryLayoutTokens {
    /** Horizontal "paper margin" for main content. */
    val HorizontalGutter: Dp = 16.dp

    /** Default vertical rhythm between major content blocks. */
    val ContentVerticalSpacing: Dp = 12.dp

    /** Vertical spacing between rows inside the LazyColumn. */
    val ListItemSpacing: Dp = 10.dp

    /** Indent step per nesting level (headers/rows). */
    val NestIndentStep: Dp = 12.dp

    /** Desired gap above the keyboard when the search bar is expanded. */
    val SearchExpandedKeyboardGap: Dp = 6.dp
}