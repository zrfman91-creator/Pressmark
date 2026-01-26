package com.zak.pressmark.feature.workdetails.route

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.zak.pressmark.feature.workdetails.vm.WorkDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDetailsRoute(
    onBack: () -> Unit,
    vm: WorkDetailsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Work details") },
                navigationIcon = {
                    Text(
                        text = "Back",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { onBack() },
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
            if (state.isMissing) {
                Text("Work not found.")
                return@Column
            }

            if (!state.artworkUri.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AsyncImage(
                        model = state.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = state.title, style = MaterialTheme.typography.titleLarge)
            Text(text = state.artistLine, style = MaterialTheme.typography.bodyLarge)
            state.year?.let { Text(text = it.toString(), style = MaterialTheme.typography.bodyMedium) }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.genres.isNotEmpty()) {
                Text("Genres: ${state.genres.joinToString(", ")}")
            }
            if (state.styles.isNotEmpty()) {
                Text("Styles: ${state.styles.joinToString(", ")}")
            }

            state.discogsMasterId?.let { masterId ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Discogs master: $masterId",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove from library?") },
            text = { Text("This will remove the work and any related entries.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteWork()
                        showDeleteConfirm = false
                        onBack()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
