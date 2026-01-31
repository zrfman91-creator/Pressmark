@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.feature.ingest.barcode.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


data class ManualIngestInputs(
    val barcode: String,
    val artist: String,
    val title: String,
)

@Composable
fun ManualEntryOverlay(
    expanded: Boolean,
    barcode: String,
    onBarcodeChange: (String) -> Unit,
    artist: String,
    onArtistChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    year: String,
    onYearChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (ManualIngestInputs) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Enter barcode…",
    horizontalPadding: Dp = 2.dp,
    expandedKeyboardGap: Dp = 0.dp,
    imeOverlapDp: Dp = 78.dp,
) {
    if (!expanded) return

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val barsBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val keyboardLift = (imeBottom - barsBottom).coerceAtLeast(0.dp)
    val overlappedLift = (keyboardLift - imeOverlapDp).coerceAtLeast(0.dp)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface,
        errorContainerColor = MaterialTheme.colorScheme.surface,
        // borders:
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        // text / cursor:
        cursorColor = MaterialTheme.colorScheme.primary,
    )

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
                    .wrapContentHeight()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { },
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
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
                            placeholder = { Text(text = placeholder) },
                            singleLine = true,
                            colors = fieldColors,
                            supportingText = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 0.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("OR\u2026")
                                }
                            }

                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = artist,
                            onValueChange = onArtistChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Artist") },
                            singleLine = true,
                            colors = fieldColors,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Title") },
                            singleLine = true,
                            colors = fieldColors,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = year,
                            onValueChange = onYearChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Year") },
                            singleLine = true,
                            colors = fieldColors,
                        )
                    }


                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(4.dp),
                            onClick = {
                                onSubmit(
                                    ManualIngestInputs(
                                        barcode = barcode,
                                        artist = artist,
                                        title = title,
                                    ),
                                )
                            },
                            content = { Text("Lookup Release") },
                        )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
