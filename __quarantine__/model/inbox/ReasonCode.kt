package com.zak.pressmark.data.model.inbox

import org.json.JSONArray

object ReasonCode {
    const val LOW_SIGNAL = "LOW_SIGNAL"
    const val MULTIPLE_CANDIDATES = "MULTIPLE_CANDIDATES"
    const val MISSING_TITLE = "MISSING_TITLE"
    const val MISSING_ARTIST = "MISSING_ARTIST"
    const val WEAK_MATCH_TITLE = "WEAK_MATCH_TITLE"
    const val WEAK_MATCH_ARTIST = "WEAK_MATCH_ARTIST"
    const val NO_API_MATCH = "NO_API_MATCH"
    const val BARCODE_VALID_CHECKSUM = "BARCODE_VALID_CHECKSUM"
    const val BARCODE_NORMALIZED = "BARCODE_NORMALIZED"
    const val FORMAT_MATCH_VINYL = "FORMAT_MATCH_VINYL"
    const val CATNO_MATCH = "CATNO_MATCH"
    const val LABEL_MATCH = "LABEL_MATCH"
    const val TITLE_MATCH = "TITLE_MATCH"
    const val ARTIST_MATCH = "ARTIST_MATCH"
    const val RUNNER_UP_GAP_STRONG = "RUNNER_UP_GAP_STRONG"

    private val ordered = listOf(
        LOW_SIGNAL,
        MULTIPLE_CANDIDATES,
        MISSING_TITLE,
        MISSING_ARTIST,
        WEAK_MATCH_TITLE,
        WEAK_MATCH_ARTIST,
        NO_API_MATCH,
        BARCODE_VALID_CHECKSUM,
        BARCODE_NORMALIZED,
        FORMAT_MATCH_VINYL,
        CATNO_MATCH,
        LABEL_MATCH,
        TITLE_MATCH,
        ARTIST_MATCH,
        RUNNER_UP_GAP_STRONG,
    )

    fun encode(reasons: List<String>): String {
        val array = JSONArray()
        normalize(reasons).forEach { array.put(it) }
        return array.toString()
    }

    fun decode(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).takeIf { it.isNotBlank() }
            }
        }.getOrDefault(emptyList())
    }

    fun append(existing: List<String>, reason: String): List<String> {
        return normalize(existing + reason)
    }

    fun remove(existing: List<String>, reason: String): List<String> {
        return normalize(existing.filterNot { it == reason })
    }

    private fun normalize(reasons: List<String>): List<String> {
        val seen = LinkedHashSet(reasons.filter { it.isNotBlank() })
        val known = ordered.filter { it in seen }
        val unknown = seen.filterNot { it in ordered }.sorted()
        return known + unknown
    }
}
