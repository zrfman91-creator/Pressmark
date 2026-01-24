// FILE: app/src/main/java/com/zak/pressmark/feature/library/route/LibraryRoute.kt
package com.zak.pressmark.feature.library.route

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zak.pressmark.feature.library.vm.LibraryItemUi
import com.zak.pressmark.feature.library.vm.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryRoute(
    vm: LibraryViewModel,
    onOpenWork: (String) -> Unit,
    onAddManual: () -> Unit,
    onAddBarcode: () -> Unit,
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onAddManual,
                    modifier = Modifier.weight(1f),
                ) { Text("Add manually") }

                Button(
                    onClick = onAddBarcode,
                    modifier = Modifier.weight(1f),
                ) { Text("Add barcode") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.items.isEmpty()) {
                Text("No works yet. Add one to get started.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items) { item ->
                        WorkRow(
                            item = item,
                            onClick = { onOpenWork(item.workId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkRow(
    item: LibraryItemUi,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.title)
            Text(item.artistLine)
            item.year?.let { Text(it.toString()) }
        }
    }
}
