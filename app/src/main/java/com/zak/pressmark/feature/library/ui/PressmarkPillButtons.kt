package com.zak.pressmark.feature.library.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val buttonShape = RoundedCornerShape(4.dp)
private val buttonBorderWidth = 1.dp
private val buttonElevation = 4.dp
private val buttonMinHeight = 40.dp

@Composable
fun PressmarkPillButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val buttonSurfaceColor = MaterialTheme.colorScheme.surface
    val buttonContentColor = MaterialTheme.colorScheme.onSurface
    val buttonBorderColor = MaterialTheme.colorScheme.outline

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = buttonMinHeight)
            .clip(buttonShape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                indication = LocalIndication.current,
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = buttonShape,
        shadowElevation = buttonElevation,
        border = BorderStroke(buttonBorderWidth, buttonBorderColor),
        color = buttonSurfaceColor,
        contentColor = buttonContentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
