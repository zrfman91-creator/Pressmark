// FILE: app/src/main/java/com/zak/pressmark/core/util/Normalizer.kt
package com.zak.pressmark.core.util

import java.util.Locale

object Normalizer {

    // English articles we want to ignore for sorting (but keep for display).
    private val leadingArticles = setOf("the", "a", "an")

    // Matches: "Four Tops, The" (case-insensitive, flexible whitespace)
    // Groups: 1 = name, 2 = article
    private val trailingCommaArticleRegex = Regex(
        pattern = "^(.+?),\\s*(the|a|an)\\s*$",
        option = RegexOption.IGNORE_CASE
    )

    //Stable key used for dedupe/lookup.
    // - collapses whitespace
    // - normalizes "X, The" -> "the X" so both forms dedupe to the same key
    fun artistKey(input: String): String {
        val cleaned = input
            .trim()
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isBlank()) return ""

        val reordered = flipTrailingCommaArticleToLeading(cleaned) ?: cleaned
        // Keep punctuation mostly intact to avoid over-normalizing, but commas used for article
        // reordering should not affect dedupe.
        val noComma = reordered.replace(",", "")
        return noComma
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    //Canonical display casing for storage/UI
    // - collapses whitespace
    // - normalizes "X, The" -> "The X" (display preference)
    // - title-cases "plain names" only
    fun artistDisplay(input: String): String {
        val cleaned = input
            .trim()
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isBlank()) return ""

        val reordered = flipTrailingCommaArticleToLeading(cleaned) ?: cleaned

        //Only auto-title-case "plain names"
        val isPlainName = reordered.matches(Regex("^[A-Za-z][A-Za-z' ]*$"))
        if (!isPlainName) return reordered

        return reordered
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                val isAllCaps = word.all { it.isLetter() && it.isUpperCase() }
                val hasVowel = word.any { it in "AEIOUaeiou" }
                val keepAllCaps = isAllCaps && !hasVowel && word.length <= 5

                when {
                    keepAllCaps -> word
                    else -> word.lowercase(Locale.US)
                        .replaceFirstChar { ch ->
                            if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
                        }
                }
            }
    }

    /**
     * Sort name used for A–Z sorting/bucketing.
     * - Keeps displayName intact ("The Four Tops")
     * - Sorts under the significant word ("Four Tops")
     */
    fun artistSortName(input: String): String {
        val display = artistDisplay(input)
        return artistSortNameFromDisplay(display)
    }

    fun artistSortNameFromDisplay(displayName: String): String {
        val normalized = displayName.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return ""

        val parts = normalized.split(" ", limit = 2)
        if (parts.isEmpty()) return normalized
        val first = parts[0]
        val rest = if (parts.size > 1) parts[1].trim() else ""

        // If the artist is literally "The" / "A" / "An", keep it as-is.
        if (rest.isBlank()) return normalized

        return if (first.lowercase(Locale.ROOT) in leadingArticles) rest else normalized
    }

    /**
     * Discogs can be inconsistent about whether it expects "The X" or "X".
     * We search a small set of variants to improve hit rate.
     */
    fun artistSearchVariants(input: String): List<String> {
        val base = input.trim().replace(Regex("\\s+"), " ")
        if (base.isBlank()) return emptyList()

        val variants = linkedSetOf<String>()
        variants += base

        // If user typed "X, The" style, include flipped form.
        flipTrailingCommaArticleToLeading(base)?.let { variants += it }

        // Include canonical display + sort variants.
        val display = artistDisplay(base)
        if (display.isNotBlank()) variants += display

        val sort = artistSortNameFromDisplay(display)
        if (sort.isNotBlank() && sort != display) variants += sort

        // If it DOESN'T start with an article, try adding "The" as a fallback.
        val startsWithArticle = display
            .split(" ", limit = 2)
            .firstOrNull()
            ?.lowercase(Locale.ROOT)
            ?.let { it in leadingArticles } == true

        if (!startsWithArticle) {
            variants += "The $display".trim()
        }

        // If it DOES start with an article, also try without it (already covered by sort, but keep raw).
        val withoutLeading = artistSortNameFromDisplay(display)
        if (withoutLeading.isNotBlank()) variants += withoutLeading

        return variants
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun flipTrailingCommaArticleToLeading(cleaned: String): String? {
        val match = trailingCommaArticleRegex.matchEntire(cleaned) ?: return null
        val name = match.groupValues.getOrNull(1)?.trim().orEmpty()
        val article = match.groupValues.getOrNull(2)?.trim().orEmpty()
        if (name.isBlank() || article.isBlank()) return null
        val articleDisplay = article.lowercase(Locale.US)
            .replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString() }
        return "$articleDisplay $name".trim()
    }
}