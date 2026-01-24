package com.zak.pressmark.data.model.inbox

data class CsvImportRow(
    val title: String?,
    val artist: String?,
    val barcode: String?,
    val catalogNo: String?,
    val label: String?,
    val year: String?,
    val format: String?,
    val rawJson: String,
)

data class CsvImportSummary(
    val totalRows: Int,
    val importedRows: Int,
    val skippedRows: Int,
)
