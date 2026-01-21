package com.zak.pressmark.data.repository

import com.zak.pressmark.data.model.inbox.CandidateScore
import com.zak.pressmark.data.model.inbox.InboxErrorCode
import com.zak.pressmark.data.model.inbox.ProviderCandidate
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import kotlin.math.min
import kotlin.random.Random

private const val CONFIDENCE_MAX = 100

object InboxPipeline {
    fun scoreCandidate(
        queryTitle: String?,
        queryArtist: String?,
        queryCatalogNo: String?,
        queryBarcode: String?,
        candidate: ProviderCandidate,
    ): CandidateScore {
        var score = 0
        val reasons = mutableListOf<String>()

        if (!queryBarcode.isNullOrBlank() && matchesBarcode(queryBarcode, candidate.barcode)) {
            score += 60
            reasons.add("barcode_match")
        }

        if (!queryCatalogNo.isNullOrBlank() && matchesText(queryCatalogNo, candidate.catalogNo)) {
            score += 25
            reasons.add("catalog_no_match")
        }

        val titleScore = tokenOverlapScore(queryTitle, candidate.title)
        if (titleScore > 0) {
            score += titleScore
            reasons.add("title_tokens")
        }

        val artistScore = tokenOverlapScore(queryArtist, candidate.artist)
        if (artistScore > 0) {
            score += artistScore
            reasons.add("artist_tokens")
        }

        val boundedScore = min(CONFIDENCE_MAX, score)
        val json = JSONObject()
            .put("reasons", JSONArray(reasons))
            .put("score", boundedScore)
            .toString()

        return CandidateScore(
            confidence = boundedScore,
            reasonsJson = json,
        )
    }

    fun shouldAutoCommit(
        topScore: Int,
        secondScore: Int?,
        wasUndone: Boolean,
    ): Boolean {
        if (wasUndone) return false
        if (topScore < 95) return false
        val gap = secondScore?.let { topScore - it } ?: topScore
        return gap >= 10
    }

    fun computeBackoffMillis(
        errorCode: InboxErrorCode,
        retryCount: Int,
        random: Random = Random.Default,
    ): Long {
        val baseMinutes = when (errorCode) {
            InboxErrorCode.OFFLINE -> 10
            InboxErrorCode.RATE_LIMIT -> 60
            InboxErrorCode.API_ERROR -> 15
            InboxErrorCode.NO_MATCH -> 1440
            InboxErrorCode.NONE -> 5
        }

        val multiplier = (1 shl retryCount.coerceAtMost(4))
        val jitter = random.nextInt(0, 5)
        return (baseMinutes * multiplier + jitter).toLong() * 60_000L
    }

    private fun tokenOverlapScore(left: String?, right: String?): Int {
        val leftTokens = normalizeTokens(left)
        val rightTokens = normalizeTokens(right)
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0

        val overlap = leftTokens.intersect(rightTokens).size
        if (overlap == 0) return 0

        val ratio = overlap.toDouble() / maxOf(leftTokens.size, rightTokens.size).toDouble()
        return when {
            ratio >= 0.9 -> 15
            ratio >= 0.7 -> 10
            ratio >= 0.5 -> 6
            ratio >= 0.3 -> 3
            else -> 1
        }
    }

    private fun matchesText(left: String?, right: String?): Boolean {
        val a = normalizeTokens(left).joinToString(" ")
        val b = normalizeTokens(right).joinToString(" ")
        return a.isNotBlank() && a == b
    }

    private fun matchesBarcode(left: String?, right: String?): Boolean {
        val a = normalizeBarcode(left)
        val b = normalizeBarcode(right)
        return !a.isNullOrBlank() && a == b
    }

    private fun normalizeBarcode(value: String?): String? {
        return value
            ?.trim()
            ?.replace(Regex("[^0-9]"), "")
            ?.takeIf { it.isNotBlank() }
    }

    private fun normalizeTokens(value: String?): Set<String> {
        val normalized = value
            ?.trim()
            ?.lowercase()
            ?.let { Normalizer.normalize(it, Normalizer.Form.NFD) }
            ?.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            ?.replace(Regex("[^a-z0-9\\s]"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        if (normalized.isBlank()) return emptySet()

        return normalized.split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }
}
