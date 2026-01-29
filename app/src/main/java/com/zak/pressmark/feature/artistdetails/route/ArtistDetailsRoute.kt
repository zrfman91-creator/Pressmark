package com.zak.pressmark.feature.artistdetails.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zak.pressmark.feature.artistdetails.vm.ArtistDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailsRoute(
    onBack: () -> Unit,
    vm: ArtistDetailsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    var showMissing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.artistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
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
            if (state.isLoading) {
                Text("Loading artist...")
                return@Column
            }

            val completionLabel = "Studio: ${state.ownedCount}/${state.totalCount}"
            val statusLabel = if (state.isComplete) "Complete" else "Incomplete"

            Text(text = completionLabel, style = MaterialTheme.typography.titleMedium)
            Text(text = statusLabel, style = MaterialTheme.typography.bodyMedium)

            if (!state.isComplete) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showMissing = !showMissing }) {
                    Text(if (showMissing) "Hide missing" else "What am I missing?")
                }
            }

            if (showMissing) {
                Spacer(modifier = Modifier.height(12.dp))
                state.missing.forEach { work ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = work.title, style = MaterialTheme.typography.bodyLarge)
                            work.year?.let { year ->
                                Text(text = year.toString(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        work.formatType?.let { format ->
                            AssistChip(
                                onClick = {},
                                label = { Text(format) },
                            )
                        }
                    }
                }
            }
        }
    }
}
