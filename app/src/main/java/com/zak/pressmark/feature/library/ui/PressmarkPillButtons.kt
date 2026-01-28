package com.zak.pressmark.feature.library.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
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

private val buttonShape = RoundedCornerShape(2.dp)
private val buttonBorderWidth = (1.dp)
private val buttonElevation = (4.dp)
private val buttonMinHeight = (40.dp)




@Composable
fun PressmarkPillButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    // Surface options
    val buttonSurfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val buttonContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val buttonBorderColor =  MaterialTheme.colorScheme.outline

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = buttonMinHeight)
            .clip(buttonShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        shape = buttonShape,
        tonalElevation = buttonElevation,
        border = BorderStroke(buttonBorderWidth,buttonBorderColor),
        color = buttonSurfaceColor,
        contentColor = buttonContentColor

    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) Icon(
                imageVector = icon,
                contentDescription = null)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge)
        }
    }
}



