package com.zak.pressmark.core.util.ocr

data class OcrAnchors(
    val rawText: String,
    val lines: List<String>,
    val titleCandidates: List<String>,
    val artistCandidates: List<String>,
)
