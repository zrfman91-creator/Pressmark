// FILE: app/src/main/java/com/zak/pressmark/feature/albumdetails/components/AlbumDetailsActionsRow.kt
package com.zak.pressmark.feature.albumdetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment

@Composable
fun AlbumDetailsActionsRow(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        Button(onClick = onEdit) { Text("Edit") }
        OutlinedButton(onClick = onDelete) { Text("Delete") }
    }
}
