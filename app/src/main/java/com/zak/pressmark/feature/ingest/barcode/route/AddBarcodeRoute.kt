// FILE: app/src/main/java/com/zak/pressmark/feature/ingest/barcode/route/AddBarcodeRoute.kt
package com.zak.pressmark.feature.ingest.barcode.route

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zak.pressmark.feature.ingest.barcode.vm.AddBarcodeViewModel
import com.zak.pressmark.feature.ingest.barcode.vm.BarcodeMasterCandidateUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBarcodeRoute(
    vm: AddBarcodeViewModel,
    onDone: () -> Unit,
    onScan: () -> Unit,
    onAdded: (String) -> Unit,
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add by barcode") },
                navigationIcon = {
                    Text(
                        text = "Back",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { onDone() },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Button(
                onClick = onScan,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scan with camera")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.barcode,
                onValueChange = vm::onBarcodeChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Barcode (UPC/EAN)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { vm.searchByBarcode() },
                enabled = !state.isLoading && state.barcode.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Lookup master on Discogs")
            }

            if (state.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = msg)
            }

            state.infoMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = msg)
            }

            state.masterCandidate?.let { candidate ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Master match")
                Spacer(modifier = Modifier.height(8.dp))
                MasterCandidateRow(
                    item = candidate,
                    onClick = { vm.addMasterToLibrary(candidate, onAdded) },
                )
            }
        }
    }
}

@Composable
private fun MasterCandidateRow(
    item: BarcodeMasterCandidateUi,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumb = item.thumbUrl ?: item.coverUrl
            if (!thumb.isNullOrBlank()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.size(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(item.artistLine)
                Text(item.releaseTitle)
                item.year?.let { Text(it.toString()) }
            }
        }
    }
}
