// FILE: app/src/main/java/com/zak/pressmark/feature/albumdetails/components/AlbumDetailsCoverActions.kt
package com.zak.pressmark.feature.albumdetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AlbumDetailsCoverActions(
    onFindCover: () -> Unit,
    onRefreshCover: () -> Unit,
    onClearCover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        OutlinedButton(onClick = onFindCover) { Text("Find cover") }
        OutlinedButton(onClick = onRefreshCover) { Text("Refresh") }
        OutlinedButton(onClick = onClearCover) { Text("Clear") }
    }
}
