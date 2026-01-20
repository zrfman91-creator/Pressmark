// FILE: app/src/main/java/com/zak/pressmark/core/util/ArtistNameFormatter.kt
package com.zak.pressmark.core.util

import java.util.Locale

/**
 * List-only artist formatting/sorting helpers.
 *
 * Rules:
 * - Trim and collapse whitespace.
 * - If artist starts with English article (The/A/An), list-display becomes "{Rest}, {Article}"
 *   preserving the original casing of the article as typed.
 * - Sort key ignores the leading article.
 * - If artist is exactly "The"/"A"/"An" (after normalization), leave unchanged.
 */
object ArtistNameFormatter {
    private val articles = setOf("the", "a", "an")

    fun normalizeWhitespace(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed.split(Regex("\\s+")).joinToString(" ")
    }

    fun displayForList(input: String): String {
        val normalized = normalizeWhitespace(input)
        val (articleRaw, rest) = splitLeadingArticle(normalized) ?: return normalized
        if (rest.isBlank()) return normalized
        return "${rest}, $articleRaw"
    }

    fun sortKeyForList(input: String): String {
        val normalized = normalizeWhitespace(input)
        val split = splitLeadingArticle(normalized)
        val rest = split?.second?.takeIf { it.isNotBlank() } ?: normalized
        return rest.lowercase(Locale.ROOT)
    }

    private fun splitLeadingArticle(normalized: String): Pair<String, String>? {
        val parts = normalized.split(" ", limit = 2)
        if (parts.isEmpty()) return null
        val first = parts[0]
        if (first.lowercase(Locale.ROOT) !in articles) return null
        val rest = if (parts.size > 1) parts[1] else ""
        return first to rest
    }
}