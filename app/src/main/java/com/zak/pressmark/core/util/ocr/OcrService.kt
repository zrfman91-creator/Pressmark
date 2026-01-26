package com.zak.pressmark.core.util.ocr

import android.net.Uri
import com.zak.pressmark.data.util.Normalization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrService @Inject constructor(
    private val textExtractor: TextExtractor,
) {
    suspend fun extractAnchors(imageUri: Uri, hint: OcrHint? = null): Result<OcrAnchors> {
        return withContext(Dispatchers.Default) {
            textExtractor.extract(imageUri, hint).map { result ->
                val cleanedLines = result.lines.mapNotNull { line ->
                    val cleaned = line.trim().replace(Regex("\\s+"), " ")
                    cleaned.takeIf { it.length >= 3 }
                }

                val candidates = cleanedLines
                    .filterNot { looksLikeNoise(it) }
                    .distinct()

                val titleCandidates = candidates
                    .filterNot { looksLikeArtistQualifier(it) }
                    .sortedByDescending { it.length }
                    .take(MAX_CANDIDATES)

                val artistCandidates = candidates
                    .filterNot { looksLikeReleaseQualifier(it) }
                    .sortedByDescending { it.length }
                    .take(MAX_CANDIDATES)

                val normalizedTitle = (titleCandidates.firstOrNull() ?: hint?.fallbackTitle).orEmpty()
                val normalizedArtist = (artistCandidates.firstOrNull() ?: hint?.fallbackArtist).orEmpty()

                val mergedTitleCandidates = listOfNotNull(normalizedTitle.ifBlank { null })
                    .plus(titleCandidates)
                    .distinct()
                    .take(MAX_CANDIDATES)

                val mergedArtistCandidates = listOfNotNull(normalizedArtist.ifBlank { null })
                    .plus(artistCandidates)
                    .distinct()
                    .take(MAX_CANDIDATES)

                OcrAnchors(
                    rawText = result.rawText,
                    lines = cleanedLines,
                    titleCandidates = mergedTitleCandidates,
                    artistCandidates = mergedArtistCandidates,
                )
            }
        }
    }

    private fun looksLikeNoise(line: String): Boolean {
        val normalized = Normalization.sortKey(line)
        if (normalized.isBlank()) return true
        val digitCount = line.count { it.isDigit() }
        if (digitCount >= line.length / 2) return true
        return false
    }

    private fun looksLikeReleaseQualifier(line: String): Boolean {
        val normalized = Normalization.sortKey(line)
        return QUALIFIERS.any { normalized.contains(it) }
    }

    private fun looksLikeArtistQualifier(line: String): Boolean {
        val normalized = Normalization.sortKey(line)
        return ARTIST_QUALIFIERS.any { normalized.contains(it) }
    }

    private companion object {
        private const val MAX_CANDIDATES = 3
        private val QUALIFIERS = listOf(
            "stereo",
            "mono",
            "side a",
            "side b",
            "side",
            "track",
            "lp",
            "vinyl",
            "rpm",
            "record",
            "catalog",
            "label",
        )
        private val ARTIST_QUALIFIERS = listOf(
            "featuring",
            "feat",
            "with",
            "and",
        )
    }
}
