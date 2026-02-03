package com.zak.pressmark.feature.ingest.manual.route

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.zak.pressmark.core.ui.InlineStatusCard
import com.zak.pressmark.feature.ingest.vm.DiscogsCandidateUi
import com.zak.pressmark.feature.ingest.vm.IngestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkRoute(
    onDone: () -> Unit,
    onAdded: (String) -> Unit,
    vm: IngestViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val manualEntryEnabled = !state.isLoading && state.artist.isNotBlank() && state.title.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add album") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            item {
                OutlinedTextField(
                    value = state.artist,
                    onValueChange = vm::onArtistChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Artist") },
                    singleLine = true,
                )
            }
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = vm::onTitleChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true,
                )
            }
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item {
                OutlinedTextField(
                    value = state.year,
                    onValueChange = vm::onYearChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Year (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            item { Spacer(modifier = Modifier.size(16.dp)) }

            item {
                Button(
                    onClick = { vm.searchDiscogs() },
                    enabled = manualEntryEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Search Discogs")
                }
            }

            if (state.isLoading) {
                item {
                    Spacer(modifier = Modifier.size(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.errorMessage?.let { msg ->
                item {
                    Spacer(modifier = Modifier.size(12.dp))
                    InlineStatusCard(
                        message = msg,
                        actionLabel = if (manualEntryEnabled) "Retry" else null,
                        onAction = if (manualEntryEnabled) vm::searchDiscogs else null,
                    )
                }
            }

            state.infoMessage?.let { msg ->
                item {
                    Spacer(modifier = Modifier.size(12.dp))
                    InlineStatusCard(message = msg)
                }
            }

            if (state.results.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.size(16.dp))
                    Text("Matches")
                    Spacer(modifier = Modifier.size(8.dp))
                }

                items(state.results, key = { it.masterId }) { item ->
                    CandidateRow(
                        item = item,
                        onClick = {
                            vm.addDiscogsCandidateToLibrary(item) { workId, _ ->
                                onAdded(workId)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }

            item { Spacer(modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun CandidateRow(
    item: DiscogsCandidateUi,
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
                Text(item.title)
                item.subtitle?.let { Text(it) }
            }
            item.year?.let {
                Spacer(modifier = Modifier.size(12.dp))
                Text(it.toString())
            }
        }
    }
}
