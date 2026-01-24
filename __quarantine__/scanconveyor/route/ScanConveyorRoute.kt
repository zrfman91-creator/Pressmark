package com.zak.pressmark.feature.scanconveyor.route

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zak.pressmark.core.util.CsvParser
import com.zak.pressmark.data.model.inbox.CsvImportRow
import com.zak.pressmark.data.work.InboxPipelineScheduler
import com.zak.pressmark.feature.scanconveyor.screen.ScanConveyorScreen
import com.zak.pressmark.feature.scanconveyor.vm.ScanConveyorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun ScanConveyorRoute(
    vm: ScanConveyorViewModel,
    onCaptureCover: () -> Unit,
    onOpenInbox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inboxCount = vm.inboxCount.collectAsState().value
    val libraryCount = vm.libraryCount.collectAsState().value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.orEmpty()
                val parsed = CsvParser.parse(content)
                val rows = buildCsvRows(parsed.headers, parsed.rows)
                val summary = vm.importCsv(rows)
                if (summary.importedRows > 0) {
                    InboxPipelineScheduler.enqueueLookupDrain(context)
                }
                summary
            }.onSuccess { summary ->
                Toast.makeText(
                    context,
                    "Imported ${summary.importedRows}/${summary.totalRows} rows",
                    Toast.LENGTH_SHORT,
                ).show()
            }.onFailure {
                Toast.makeText(context, "Failed to import CSV.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ScanConveyorScreen(
        inboxCount = inboxCount,
        libraryCount = libraryCount,
        onScanBarcode = { barcode ->
            vm.addBarcode(barcode) { _, autoCommitted ->
                InboxPipelineScheduler.enqueueLookupDrain(context)
                val message = if (autoCommitted) {
                    "Strong match record committed"
                } else {
                    "Sent to Inbox for review"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        },
        onCaptureCover = onCaptureCover,
        onQuickAdd = { title, artist ->
            vm.quickAdd(title, artist) {
                InboxPipelineScheduler.enqueueLookupDrain(context)
            }
        },
        onImportCsv = {
            csvLauncher.launch(arrayOf("text/csv", "text/plain"))
        },
        onOpenInbox = onOpenInbox,
        modifier = modifier,
    )
}

private fun buildCsvRows(headers: List<String>, rows: List<List<String>>): List<CsvImportRow> {
    if (headers.isEmpty()) return emptyList()
    val headerMap = headers.mapIndexed { index, header ->
        normalizeHeader(header) to index
    }.toMap()

    fun get(row: List<String>, key: String): String? {
        val idx = headerMap[key] ?: return null
        return row.getOrNull(idx)?.trim()?.takeIf { it.isNotBlank() }
    }

    return rows.map { row ->
        val raw = JSONObject()
        headers.forEachIndexed { index, header ->
            raw.put(header, row.getOrNull(index))
        }
        CsvImportRow(
            title = get(row, "title"),
            artist = get(row, "artist"),
            barcode = get(row, "barcode"),
            catalogNo = get(row, "catno"),
            label = get(row, "label"),
            year = get(row, "year"),
            format = get(row, "format"),
            rawJson = raw.toString(),
        )
    }
}

private fun normalizeHeader(header: String): String {
    val clean = header.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
    return when {
        clean in setOf("artist", "artists") -> "artist"
        clean in setOf("title", "release", "album") -> "title"
        clean in setOf("barcode", "upc", "ean") -> "barcode"
        clean in setOf("catno", "catalogno", "catalognumber", "catalog") -> "catno"
        clean in setOf("label", "labels") -> "label"
        clean in setOf("year", "releaseyear") -> "year"
        clean in setOf("format", "formats") -> "format"
        else -> clean
    }
}
