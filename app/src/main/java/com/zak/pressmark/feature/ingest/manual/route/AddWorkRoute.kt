package com.zak.pressmark.feature.ingest.manual.route

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.zak.pressmark.feature.ingest.manual.vm.AddWorkViewModel
import com.zak.pressmark.feature.ingest.manual.vm.DiscogsCandidateUi
import com.zak.pressmark.feature.ingest.manual.vm.OcrCaptureSource
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkRoute(
    onDone: () -> Unit,
    vm: AddWorkViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    var showOcrSheet by remember { mutableStateOf(false) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var pendingSource by remember { mutableStateOf(OcrCaptureSource.COVER) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            vm.onOcrImageCaptured(uri, pendingSource)
        }
    }

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
            OutlinedButton(
                onClick = { showOcrSheet = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("No barcode? Capture cover/label")
            }

            if (state.isOcrProcessing) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.ocrMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = message)
            }

            OcrSuggestionRow(
                title = "Suggested titles",
                suggestions = state.ocrTitleCandidates,
                onSelect = vm::applyTitleCandidate,
            )
            OcrSuggestionRow(
                title = "Suggested artists",
                suggestions = state.ocrArtistCandidates,
                onSelect = vm::applyArtistCandidate,
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { vm.addManualWork() },
                enabled = !state.isLoading && state.artist.isNotBlank() && state.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add without Discogs")
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
                            onClick = { vm.addToLibrary(item) },
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    if (showOcrSheet) {
        OcrSourceSheet(
            onDismiss = { showOcrSheet = false },
            onPick = { source ->
                pendingSource = source
                pendingCaptureUri = createImageUri(context)
                pendingCaptureUri?.let { takePictureLauncher.launch(it) }
                showOcrSheet = false
            },
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrSourceSheet(
    onDismiss: () -> Unit,
    onPick: (OcrCaptureSource) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Capture photo for OCR")
            OutlinedButton(
                onClick = { onPick(OcrCaptureSource.COVER) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Capture cover")
            }
            OutlinedButton(
                onClick = { onPick(OcrCaptureSource.LABEL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Capture label")
            }
        }
    }
}

@Composable
private fun OcrSuggestionRow(
    title: String,
    suggestions: List<String>,
    onSelect: (String) -> Unit,
) {
    if (suggestions.isEmpty()) return
    Spacer(modifier = Modifier.height(12.dp))
    Text(title)
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach { suggestion ->
            AssistChip(
                onClick = { onSelect(suggestion) },
                label = { Text(suggestion) },
            )
        }
    }
}

private fun createImageUri(context: Context): Uri? {
    return runCatching {
        val file = File.createTempFile("ocr_", ".jpg", context.cacheDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }.getOrNull()
}
