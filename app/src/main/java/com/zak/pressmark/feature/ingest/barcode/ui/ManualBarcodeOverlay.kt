@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.feature.ingest.barcode.ui

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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
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

@Composable
fun ManualBarcodeOverlay(
    expanded: Boolean,
    barcode: String,
    onBarcodeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Enter barcode…",
    height: Dp = 56.dp,
    horizontalPadding: Dp = 6.dp,
    expandedKeyboardGap: Dp = 6.dp,
    imeOverlapDp: Dp = 76.dp,
) {
    if (!expanded) return

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val barsBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val keyboardLift = (imeBottom - barsBottom).coerceAtLeast(0.dp)
    val overlappedLift = (keyboardLift - imeOverlapDp).coerceAtLeast(0.dp)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val targetWidth = (maxWidth - (horizontalPadding * 2)).coerceAtLeast(240.dp)
        val animatedWidth by animateDpAsState(targetValue = targetWidth, label = "barcodeWidth")

        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    focusManager.clearFocus(force = true)
                    onDismiss()
                },
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
            Surface(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(height)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
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
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))

                    TextField(
                        value = barcode,
                        onValueChange = { value ->
                            val filtered = value
                                .uppercase()
                                .filter { it.isDigit() || it == 'X' || it == '-' }
                            onBarcodeChange(filtered)
                        },
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
                            if (barcode.isNotBlank()) {
                                onSubmit(barcode.trim())
                            } else {
                                focusManager.clearFocus(force = true)
                                onDismiss()
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Done, contentDescription = "Submit")
                    }
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}
