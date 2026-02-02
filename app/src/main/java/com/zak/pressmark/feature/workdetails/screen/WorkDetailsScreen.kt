package com.zak.pressmark.feature.workdetails.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDetailsScreen(
    isMissing: Boolean,
    artworkUri: String?,
    title: String,
    artistLine: String,
    year: Int?,
    genres: List<String>,
    styles: List<String>,
    discogsMasterId: Long?,
    onBack: () -> Unit,
    onDeleteConfirmed: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Album details", style = MaterialTheme.typography.displayLarge) },
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
            if (isMissing) {
                Text("Work not found.")
                return@Column
            }

            if (!artworkUri.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(256.dp)
                        .clip(RoundedCornerShape(4.dp)),
                ) {
                    AsyncImage(
                        model = artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(256.dp),
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

            // Title
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            // artist · year
            val artistYearLine = buildString {
                append(artistLine)
                if (year != null) {
                    append(" \u00B7 ")
                    append(year.toString())
                }
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = artistYearLine,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (genres.isNotEmpty()) {
                Text("Genres: ${genres.joinToString(", ")}")
            }
            if (styles.isNotEmpty()) {
                Text("Styles: ${styles.joinToString(", ")}")
            }

            discogsMasterId?.let { masterId ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Discogs master: $masterId",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                shape = RoundedCornerShape(4.dp),
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("Refine pressing")
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                shape = RoundedCornerShape(4.dp),
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Remove “$title”",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                    },
            text = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "This will remove it from your library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                   },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Button(
                    shape = RoundedCornerShape(4.dp),
                    onClick = {
                        onDeleteConfirmed()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton (
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = RoundedCornerShape(4.dp),
                    onClick = { showDeleteConfirm = false })

                { Text("Cancel") }
            },
        )
    }
}
