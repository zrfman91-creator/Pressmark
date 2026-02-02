package com.zak.pressmark.feature.refinepressing.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.ui.InlineStatusCard
import com.zak.pressmark.feature.refinepressing.vm.PressingCandidateUi
import com.zak.pressmark.feature.refinepressing.vm.RefinePressingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefinePressingScreen(
    state: RefinePressingUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refine pressing", style = MaterialTheme.typography.displayLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary,
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
            Text(
                text = "Find the best pressing match.",
                style = MaterialTheme.typography.bodyLarge,
            )

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                return@Column
            }

            state.errorMessage?.let { message ->
                InlineStatusCard(
                    message = message,
                    actionLabel = "Retry",
                    onAction = onRetry,
                )
                return@Column
            }

            if (state.candidates.isEmpty()) {
                InlineStatusCard(message = "No matches yet. Try again.")
                Button(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Search Discogs")
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.candidates, key = { it.discogsReleaseId }) { candidate ->
                    PressingCandidateRow(candidate = candidate)
                }
            }
        }
    }
}

@Composable
private fun PressingCandidateRow(
    candidate: PressingCandidateUi,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = candidate.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val labelLine = listOfNotNull(candidate.label, candidate.catalogNo).joinToString(" \u00B7 ")
        if (labelLine.isNotBlank()) {
            Text(
                text = labelLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val subtitle = listOfNotNull(candidate.year?.toString(), candidate.country).joinToString(" \u00B7 ")
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
