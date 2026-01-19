package com.zak.pressmark.feature.catalog.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ChipWidth: Dp = 120.dp
private val MenuShape = RoundedCornerShape(12.dp)

data class CatalogOption(
    val id: String,
    val label: String,
)

/**
 * Display-only compaction to keep fixed-width dropdown rows single-line.
 * Selection values remain the original option strings.
 */
private fun compactOptionLabel(raw: String): String {
    var s = raw.trim()

    // Common compactions (display only).
    s = s.replace(Regex("\\bAlphabetical\\b", RegexOption.IGNORE_CASE), "A–Z")
    s = s.replace(Regex("\\bAscending\\b", RegexOption.IGNORE_CASE), "Asc")
    s = s.replace(Regex("\\bDescending\\b", RegexOption.IGNORE_CASE), "Desc")
    s = s.replace(Regex("\\bRecently Added\\b", RegexOption.IGNORE_CASE), "Recent")
    s = s.replace(Regex("\\bDate Added\\b", RegexOption.IGNORE_CASE), "Added")
    s = s.replace(Regex("\\bRelease Year\\b", RegexOption.IGNORE_CASE), "Year")
    s = s.replace(Regex("\\bArtist Name\\b", RegexOption.IGNORE_CASE), "Artist")
    s = s.replace(Regex("\\bAlbum Title\\b", RegexOption.IGNORE_CASE), "Title")

    // Normalize whitespace.
    s = s.replace(Regex("\\s+"), " ")

    return s
}

@Composable
fun CatalogTopActionBar(
    sortExpanded: Boolean,
    filterExpanded: Boolean,
    groupExpanded: Boolean,
    selectedSortId: String?,
    selectedFilterId: String?,
    selectedGroupId: String?,
    onSortToggle: () -> Unit,
    onFilterToggle: () -> Unit,
    onGroupToggle: () -> Unit,
    sortOptions: List<CommandOption>,
    filterOptions: List<CommandOption>,
    groupOptions: List<CommandOption>,
    onSortSelect: (String) -> Unit,
    onFilterSelect: (String) -> Unit,
    onGroupSelect: (String) -> Unit,
    showClear: Boolean,
    onClearSelections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val selectedSort = sortOptions.firstOrNull { it.id == selectedSortId }?.label
        val selectedFilter = filterOptions.firstOrNull { it.id == selectedFilterId }?.label
        val selectedGroup = groupOptions.firstOrNull { it.id == selectedGroupId }?.label

        // Clear action above selection labels, centered.
        AnimatedVisibility(
            visible = showClear,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(90)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextButton(
                    onClick = onClearSelections,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(24.dp)              // <- hard cap, tighten here
                        .defaultMinSize(minHeight = 0.dp),
                ) {
                    Text(
                        text = "Clear selections",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionPillDropdown(
                label = "Sort",
                selectedValue = selectedSort,
                expanded = sortExpanded,
                onToggle = onSortToggle,
                options = sortOptions,
                onSelect = onSortSelect,
                width = ChipWidth,
            )

            ActionPillDropdown(
                label = "Filter",
                selectedValue = selectedFilter,
                expanded = filterExpanded,
                onToggle = onFilterToggle,
                options = filterOptions,
                onSelect = onFilterSelect,
                width = ChipWidth,
            )

            ActionPillDropdown(
                label = "Grouping",
                selectedValue = selectedGroup,
                expanded = groupExpanded,
                onToggle = onGroupToggle,
                options = groupOptions,
                onSelect = onGroupSelect,
                width = ChipWidth,
            )
        }
    }
}

@Composable
private fun ActionPillDropdown(
    label: String,
    selectedValue: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    options: List<CommandOption>,
    onSelect: (String) -> Unit,
    width: Dp,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(140),
        label = "chevronRotation",
    )

    Column(
        modifier = Modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Current selection label (kept out of the pill so the pill stays uniform).
        Text(
            text = selectedValue?.let(::compactOptionLabel) ?: "—",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .semantics { contentDescription = "$label selection" },
            textAlign = TextAlign.Center,
        )

        // Anchor box for the menu so it appears directly below the pill.
        Box(modifier = Modifier.width(width)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = if (expanded) 2.dp else 0.dp,
                modifier = Modifier
                    .width(width)
                    .clickable(onClick = onToggle)
                    .semantics { contentDescription = label },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onToggle,
                modifier = Modifier
                    .width(width)
                    .clip(MenuShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        MenuShape,
                    ),
            ) {
                options.forEach { option ->
                    // Keep menu items single-line within fixed width.
                    val displayOption = compactOptionLabel(option.label)
                    val optionTextStyle = when {
                        displayOption.length >= 24 -> MaterialTheme.typography.labelSmall
                        displayOption.length >= 18 -> MaterialTheme.typography.labelMedium
                        else -> MaterialTheme.typography.labelLarge
                    }

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = displayOption,
                                style = optionTextStyle,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = { onSelect(option.id) },
                        // No checkmark needed; selection is shown above the pill.
                        // Keep a consistent left inset for the menu content.
                        contentPadding = PaddingValues(
                            start = 4.dp,
                            end = 12.dp,
                            top = 8.dp,
                            bottom = 8.dp,
                        ),
                    )
                }
            }
        }

        // TODO(zak): [P2] Replace "—" with meaningful per-control defaults (e.g., "Default", "All") — once option sets are finalized — done-when defaults feel natural.
    }
}
