package com.zak.pressmark.data.util

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight fuzzy matching utilities (no deps).
 *
 * Intended use:
 * - Only as a fallback when a strict search yields no (or very few) results.
 * - Or to rank a set of candidate results.
 */
object FuzzyMatch {

    /**
     * Normalize text for matching:
     * - lowercases
     * - strips diacritics
     * - removes punctuation (keeps letters/digits/spaces)
     * - collapses whitespace
     */
    fun normalizeForMatch(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ""

        val noDiacritics = Normalizer
            .normalize(trimmed, Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")

        return noDiacritics
            .lowercase()
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Jaro–Winkler similarity in [0.0, 1.0].
     * Good for short strings (names), tolerant to transpositions.
     */
    fun jaroWinkler(aRaw: String, bRaw: String): Double {
        val a = normalizeForMatch(aRaw)
        val b = normalizeForMatch(bRaw)

        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0

        val jaro = jaroDistance(a, b)
        val prefix = commonPrefixLength(a, b, maxLen = 4)
        val scaling = 0.1
        return jaro + (prefix * scaling * (1.0 - jaro))
    }

    /**
     * Weighted score for matching (artist, title).
     * Returns [0.0, 1.0].
     */
    fun artistTitleScore(
        userArtist: String,
        userTitle: String,
        candidateArtist: String,
        candidateTitle: String,
        artistWeight: Double = 0.60,
    ): Double {
        val hasArtist = userArtist.isNotBlank() && candidateArtist.isNotBlank()
        val hasTitle = userTitle.isNotBlank() && candidateTitle.isNotBlank()

        // If one side is missing artist/title, re-weight so we don't punish missing data.
        val (wArtist, wTitle) = when {
            hasArtist && hasTitle -> artistWeight to (1.0 - artistWeight)
            hasArtist && !hasTitle -> 1.0 to 0.0
            !hasArtist && hasTitle -> 0.0 to 1.0
            else -> 0.0 to 0.0
        }

        val sArtist = if (wArtist > 0.0) jaroWinkler(userArtist, candidateArtist) else 0.0
        val sTitle = if (wTitle > 0.0) jaroWinkler(userTitle, candidateTitle) else 0.0

        val denom = (wArtist + wTitle)
        return if (denom <= 0.0) 0.0 else ((wArtist * sArtist) + (wTitle * sTitle)) / denom
    }

    private fun jaroDistance(s1: String, s2: String): Double {
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0 && len2 == 0) return 1.0
        if (len1 == 0 || len2 == 0) return 0.0

        val matchDistance = max(0, (max(len1, len2) / 2) - 1)

        val s1Matches = BooleanArray(len1)
        val s2Matches = BooleanArray(len2)

        var matches = 0
        for (i in 0 until len1) {
            val start = max(0, i - matchDistance)
            val end = min(i + matchDistance + 1, len2)

            for (j in start until end) {
                if (s2Matches[j]) continue
                if (s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        var t = 0
        var k = 0
        for (i in 0 until len1) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) t++
            k++
        }

        val transpositions = t / 2.0

        return (
                (matches / len1.toDouble()) +
                        (matches / len2.toDouble()) +
                        ((matches - transpositions) / matches.toDouble())
                ) / 3.0
    }

    private fun commonPrefixLength(a: String, b: String, maxLen: Int): Int {
        val n = min(min(a.length, b.length), maxLen)
        var i = 0
        while (i < n && a[i] == b[i]) i++
        return i
    }
}