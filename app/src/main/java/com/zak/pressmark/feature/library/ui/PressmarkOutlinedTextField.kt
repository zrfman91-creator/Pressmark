@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.feature.library.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.ui.theme.PressmarkTheme

@Composable
fun PressmarkOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,

    outlineWidth: Dp = 1.dp,
    outlineShape: Shape = RoundedCornerShape(1.dp),
    outlineColor: Color = MaterialTheme.colorScheme.outline,
    outlineFocusedColor: Color = MaterialTheme.colorScheme.primary,
    outlineErrorColor: Color = MaterialTheme.colorScheme.error,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val strokeColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant
        isError -> outlineErrorColor
        isFocused -> outlineFocusedColor
        else -> outlineColor
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        isError = isError,
        interactionSource = interactionSource,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        modifier = modifier
            .fillMaxWidth()
            .clip(outlineShape)
            .border(BorderStroke(outlineWidth, strokeColor), outlineShape),
        shape = outlineShape
    )
}
@Preview(showBackground = true, widthDp = 420)
@Composable
private fun PressmarkOutlinedTextField_Preview_Default() {
    PressmarkTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            val (text, setText) = remember { mutableStateOf("Daft Punk") }

            Column(Modifier.padding(16.dp)) {
                PressmarkOutlinedTextField(
                    value = text,
                    onValueChange = setText,
                    label = "Artist",
                    placeholder = "Type an artist…",
                    supportingText = "Example: Daft Punk",
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                PressmarkOutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = "Catalog #",
                    placeholder = "e.g., ST-12345",
                    supportingText = "Optional",
                    singleLine = true
                )
            }
        }
    }
}

    @Preview(showBackground = true, widthDp = 420)
    @Composable
    private fun PressmarkOutlinedTextField_Preview_Error() {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                PressmarkOutlinedTextField(
                    value = "   ",
                    onValueChange = {},
                    label = "Title",
                    placeholder = "Required",
                    isError = true,
                    supportingText = "Title is required",
                    singleLine = true
                )
            }
        }
    }
