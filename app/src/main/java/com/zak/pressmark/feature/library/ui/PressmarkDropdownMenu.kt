package com.zak.pressmark.feature.library.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> PressmarkDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<T>,
    itemText: (T) -> String,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: ((T) -> Boolean)? = null,
) {
    val dropdownContainerColor = MaterialTheme.colorScheme.surface
    val dropdownBorderColor = MaterialTheme.colorScheme.outline
    val dropdownBorderWidth = 1.dp
    val dropdownContainerShape = RoundedCornerShape(4.dp)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = dropdownContainerShape,
        containerColor = dropdownContainerColor,
        border = BorderStroke(dropdownBorderWidth, dropdownBorderColor),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp

    ){
        items.forEach { item ->
            val selected = isSelected?.invoke(item) == true
            DropdownMenuItem(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                text = { Text(itemText(item)) },
                onClick = { onItemSelected(item) },
                trailingIcon = if (selected) ({ Text("✓") }) else null
            )
        }
    }
}
