package com.zak.pressmark.core.ocr

import android.net.Uri

data class OcrHint(
    val fallbackTitle: String? = null,
    val fallbackArtist: String? = null,
)

data class OcrResult(
    val rawText: String,
    val lines: List<String>,
)

interface TextExtractor {
    suspend fun extract(imageUri: Uri, hint: OcrHint? = null): Result<OcrResult>
}
