package com.zak.pressmark.feature.workdetails.screen

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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zak.pressmark.feature.workdetails.components.AboutThisAlbumSection
import com.zak.pressmark.feature.workdetails.ui.PressingManager
import com.zak.pressmark.feature.workdetails.ui.PressingUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDetailsScreen(
    isMissing: Boolean,

    // ✅ Split
    masterArtworkUri: String?,
    selectedPressingArtworkUri: String?,

    title: String,
    artistLine: String,
    year: Int?,
    genres: List<String>,
    styles: List<String>,
    discogsMasterId: Long?,

    // Selected pressing refinement details
    selectedPressingLabel: String?,
    selectedPressingCatalogNo: String?,
    selectedPressingCountry: String?,
    selectedPressingYear: Int?,
    selectedPressingFormat: String?,
    selectedDiscogsReleaseId: Long?,

    onBack: () -> Unit,
    onRefinePressing: () -> Unit,
    onDeleteConfirmed: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // ✅ OWNED WHEN AVAILABLE for the header
    val headerArtworkUri: String? = selectedPressingArtworkUri ?: masterArtworkUri

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
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
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

            // Header artwork (owned-first)
            if (!headerArtworkUri.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(256.dp)
                        .clip(RoundedCornerShape(4.dp)),
                ) {
                    AsyncImage(
                        model = headerArtworkUri,
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

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            val displayYear: Int? = (selectedPressingYear ?: year)?.takeIf { it > 0 }
            val artistYearLine = buildString {
                append(artistLine)
                displayYear?.let {
                    append(" \u00B7 ")
                    append(it)
                }
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = artistYearLine,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (genres.isNotEmpty()) Text("Genres: ${genres.joinToString(", ")}")
            if (styles.isNotEmpty()) Text("Styles: ${styles.joinToString(", ")}")

            Spacer(modifier = Modifier.height(24.dp))

            val hasPressingDetails =
                !selectedPressingLabel.isNullOrBlank() ||
                        !selectedPressingCatalogNo.isNullOrBlank() ||
                        !selectedPressingCountry.isNullOrBlank() ||
                        selectedPressingYear != null ||
                        !selectedPressingFormat.isNullOrBlank() ||
                        selectedDiscogsReleaseId != null

            val pressingsUi: List<PressingUi> =
                if (hasPressingDetails) {
                    listOf(
                        PressingUi(
                            id = selectedDiscogsReleaseId?.toString() ?: "selected",
                            title = buildString {
                                selectedPressingYear?.let { append(it).append(" ") }
                                selectedPressingLabel?.takeIf { it.isNotBlank() }?.let {
                                    if (isNotEmpty()) append("• ")
                                    append(it).append(" ")
                                }
                                selectedPressingCatalogNo?.takeIf { it.isNotBlank() }?.let {
                                    if (isNotEmpty()) append("• ")
                                    append(it)
                                }
                                if (isEmpty()) append("Selected pressing")
                            },
                            year = selectedPressingYear,
                            label = selectedPressingLabel,
                            catalogNo = selectedPressingCatalogNo
                        )
                    )
                } else emptyList()

            // ✅ About section must show MASTER artwork only
            AboutThisAlbumSection(
                title = title,
                artist = artistLine,
                masterYear = year,
                genres = genres,
                styles = styles,
                discogsMasterId = discogsMasterId,
                masterArtworkUri = masterArtworkUri,
                modifier = Modifier.padding(top = 8.dp),
            )

            PressingManager(
                pressings = pressingsUi,
                selectedPressingId = pressingsUi.firstOrNull()?.id,
                onSelectPressing = { /* TODO */ },
                onRefinePressing = onRefinePressing,
                onAddAdditionalPressing = onRefinePressing,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete") }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove “$title”?") },
            text = { Text("This will remove it from your library.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConfirmed()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}
