// =======================================================
// file: app/src/main/java/com/zak/pressmark/ui/albumlist/components/AlbumCommandBar.kt
// =======================================================
package com.zak.pressmark.feature.albumlist.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Row

@Immutable
data class CommandOption(
    val id: String,
    val label: String,
)

@Composable
fun AlbumCommandBar(
    query: String,
    onQueryChange: (String) -> Unit,
    countText: String,
    sortText: String,
    densityText: String,
    sortOptions: List<CommandOption>,
    selectedSortId: String,
    onSortSelected: (String) -> Unit,
    densityOptions: List<CommandOption>,
    selectedDensityId: String,
    onDensitySelected: (String) -> Unit,
    selectionActive: Boolean,
    selectionBar: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 6.dp,
    fieldMinHeight: Dp = 44.dp,
    overflowButtonSize: Dp = 40.dp,
) {
    val focusManager = LocalFocusManager.current
    var menuOpen by remember { mutableStateOf(false) }

    // ✅ Command bar background (change this)
    val barContainerColor = MaterialTheme.colorScheme.primaryContainer

    // ✅ Dropdown menu background (change this)
    val menuContainerColor = MaterialTheme.colorScheme.secondaryContainer

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 0.dp),
        shape = RoundedCornerShape(0.dp),
        color = barContainerColor,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            if (selectionActive && selectionBar != null) {
                selectionBar()
                Spacer(Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = fieldMinHeight),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    placeholder = { Text("Search artist or title…") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    // ✅ TextField background (optional)
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )

                Spacer(Modifier.width(6.dp))

                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(overflowButtonSize)
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = menuContainerColor, // ✅ menu background
                        shadowElevation = 6.dp,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        if (sortOptions.isNotEmpty()) {
                            MenuSectionHeader(text = "Sort")
                            sortOptions.forEach { opt ->
                                MenuOptionItem(
                                    text = opt.label,
                                    selected = opt.id == selectedSortId,
                                    onClick = {
                                        onSortSelected(opt.id)
                                        menuOpen = false
                                    }
                                )
                            }
                        }

                        if (sortOptions.isNotEmpty() && densityOptions.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            )
                        }

                        if (densityOptions.isNotEmpty()) {
                            MenuSectionHeader(text = "Row Density")
                            densityOptions.forEach { opt ->
                                MenuOptionItem(
                                    text = opt.label,
                                    selected = opt.id == selectedDensityId,
                                    onClick = {
                                        onDensitySelected(opt.id)
                                        menuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            CommandInfoRow(
                countText = countText,
                sortText = sortText,
                densityText = densityText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun CommandInfoRow(
    countText: String,
    sortText: String,
    densityText: String,
    modifier: Modifier = Modifier,
) {
    val baseStyle = MaterialTheme.typography.labelLarge
    val emphasis = SpanStyle(
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Text(
        text = buildAnnotatedString {
            append("Albums: ")
            withStyle(emphasis) { append(countText) }
            append(" | Sort: ")
            withStyle(emphasis) { append(sortText) }
            append(" | View: ")
            withStyle(emphasis) { append(densityText) }
        },
        modifier = modifier,
        textAlign = TextAlign.Center,
        style = baseStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MenuSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun MenuOptionItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onClick = onClick,
        modifier = modifier,
        contentPadding = contentPadding,
        trailingIcon = { if (selected) TrailingDotIcon() },
        // ✅ item text/icon colors (optional)
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.onSurface,
            trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun TrailingDotIcon() {
    Icon(
        imageVector = Icons.Filled.FiberManualRecord,
        contentDescription = null,
        modifier = Modifier.size(8.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}