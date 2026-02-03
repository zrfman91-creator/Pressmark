@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.feature.workdetails.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.ui.theme.PressmarkTheme

/**
 * UI model for a pressing the user owns / has selected.
 * Keep this lightweight; map from your domain model in the screen/viewmodel.
 */
data class PressingUi(
    val id: String,
    val year: Int?,
    val label: String?,
    val catalogNo: String?,
    val title: String, // e.g. "2014 Reissue" or "US 1st Press"
)

/**
 * Reusable pressing manager UI.
 *
 * - If pressings is empty: shows an empty state + "Refine pressing" CTA.
 * - If pressings is non-empty: shows "Your pressing" dropdown + "Add another pressing" CTA.
 */
@Composable
fun PressingManager(
    pressings: List<PressingUi>,
    selectedPressingId: String?,
    onSelectPressing: (String) -> Unit,
    onRefinePressing: () -> Unit,
    onAddAdditionalPressing: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Your pressing",
) {
    val selected = pressings.firstOrNull { it.id == selectedPressingId } ?: pressings.firstOrNull()

    // If list becomes non-empty and nothing selected yet, auto-select the first
    LaunchedEffect(pressings, selectedPressingId) {
        if (pressings.isNotEmpty() && selectedPressingId == null) {
            onSelectPressing(pressings.first().id)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            if (pressings.isEmpty()) {
              Box(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(top = 10.dp)
              ) {  // Empty state: "Your pressing" exists, but no selection yet
                  Text(
                      modifier = Modifier.fillMaxWidth(),
                      text = "No pressing selected yet.\nConfirming your exact pressing improves accuracy\n(label, catalog number, year).",
                      style = MaterialTheme.typography.bodyMedium,
                      textAlign = Center,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
              }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRefinePressing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Refine pressing")
                }
                // Optional: subtle alternate action (if you want)
                TextButton(onClick = onAddAdditionalPressing) {
                    Text("Browse pressings")
                }

            } else {
                // Non-empty: dropdown picker + add another
                PressingDropdown(
                    pressings = pressings,
                    selected = selected,
                    onSelect = { onSelectPressing(it.id) },
                )

                // Optional detail line for confidence
                selected?.let { s ->
                    Text(
                        text = formatPressingMeta(s),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // If you still want "Refine" available when one exists:
                    OutlinedButton(
                        shape = RoundedCornerShape(4.dp),
                        onClick = onRefinePressing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refine")
                    }

                    Button(
                        shape = RoundedCornerShape(4.dp),
                        onClick = onAddAdditionalPressing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add another")
                    }
                }
            }
        }
    }
}

@Composable
private fun PressingDropdown(
    pressings: List<PressingUi>,
    selected: PressingUi?,
    onSelect: (PressingUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.title.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Selected pressing") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            pressings.forEach { pressing ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(pressing.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = formatPressingMeta(pressing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(pressing)
                    }
                )
            }
        }
    }
}

private fun formatPressingMeta(p: PressingUi): String {
    val parts = buildList {
        p.year?.let { add(it.toString()) }
        p.label?.takeIf { it.isNotBlank() }?.let { add(it) }
        p.catalogNo?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    return if (parts.isEmpty()) "Details unavailable" else parts.joinToString(" • ")
}

@Preview(showBackground = true)
@Composable
private fun PressingManagerPreview_Empty() {
    PressmarkTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.padding(16.dp)) {
                PressingManager(
                    pressings = emptyList(),
                    selectedPressingId = null,
                    onSelectPressing = {},
                    onRefinePressing = {},
                    onAddAdditionalPressing = {},
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PressingManagerPreview_WithPressings() {
    PressmarkTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            val demo = listOf(
                PressingUi(
                    id = "p1",
                    year = 1988,
                    label = "Capitol Records",
                    catalogNo = "C1-48076",
                    title = "US 1988 • Capitol • C1-48076"
                ),
                PressingUi(
                    id = "p2",
                    year = 2014,
                    label = "Capitol Records",
                    catalogNo = "B0021234-01",
                    title = "2014 Reissue • Capitol"
                )
            )

            Column(Modifier.padding(16.dp)) {
                PressingManager(
                    pressings = demo,
                    selectedPressingId = "p1",
                    onSelectPressing = {},
                    onRefinePressing = {},
                    onAddAdditionalPressing = {},
                )
            }
        }
    }
}