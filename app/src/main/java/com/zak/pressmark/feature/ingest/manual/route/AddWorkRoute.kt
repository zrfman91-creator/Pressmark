package com.zak.pressmark.feature.ingest.manual.route

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.zak.pressmark.feature.ingest.manual.vm.AddWorkViewModel
import com.zak.pressmark.feature.ingest.manual.vm.DiscogsCandidateUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkRoute(
    onDone: () -> Unit,
    vm: AddWorkViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add album") },
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
            OutlinedTextField(
                value = state.artist,
                onValueChange = vm::onArtistChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Artist") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.year,
                onValueChange = vm::onYearChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Year (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { vm.searchDiscogs() },
                enabled = !state.isLoading && state.artist.isNotBlank() && state.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Search Discogs")
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

            if (state.results.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Matches")
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.results) { item ->
                        CandidateRow(
                            item = item,
                            onClick = { vm.addToLibrary(item, onDone) },
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
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
            if (!item.thumbUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.size(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(item.displayTitle)
                item.subtitle?.let { Text(it) }
            }
            if (item.year != null) {
                Spacer(modifier = Modifier.size(12.dp))
                Text(item.year.toString())
            }
        }
    }
}
