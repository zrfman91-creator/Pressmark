package com.zak.pressmark.data.util

import java.text.Normalizer
import java.util.Locale

/**
 * Stable normalization utilities for sort keys and evidence matching.
 *
 * Design goals:
 * - deterministic
 * - reasonably tolerant of punctuation/diacritics/whitespace differences
 * - safe for prefix search (LIKE 'prefix%')
 */
object Normalization {

    fun sortKey(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return basicKey(input)
    }

    fun evidenceKey(input: String?): String {
        if (input.isNullOrBlank()) return ""
        // Evidence is typically more "token-like" (barcode/catno/runout),
        // so we strip more aggressively.
        return basicKey(input)
            .replace(" ", "")
    }

    private fun basicKey(input: String): String {
        val trimmed = input.trim()
        val normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
        val noDiacritics = normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        val lower = noDiacritics.lowercase(Locale.US)

        // Keep letters, numbers, and spaces. Everything else becomes a space.
        val keep = lower.replace(Regex("[^a-z0-9 ]+"), " ")
        // Collapse repeated whitespace.
        return keep.replace(Regex("\\s+"), " ").trim()
    }
}
