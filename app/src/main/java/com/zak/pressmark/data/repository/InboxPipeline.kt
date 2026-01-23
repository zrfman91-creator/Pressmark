package com.zak.pressmark.data.repository

import com.zak.pressmark.data.model.inbox.CandidateScore
import com.zak.pressmark.data.model.inbox.InboxErrorCode
import com.zak.pressmark.data.model.inbox.ProviderCandidate
import com.zak.pressmark.data.model.inbox.ReasonCode
import java.text.Normalizer
import kotlin.math.min
import kotlin.random.Random

private const val CONFIDENCE_MAX = 100
private const val BARCODE_MATCH_SCORE = 40
private const val BARCODE_CHECKSUM_SCORE = 8
private const val BARCODE_NORMALIZED_SCORE = 4
private const val TITLE_STRONG_SCORE = 25
private const val TITLE_WEAK_SCORE = 15
private const val ARTIST_STRONG_SCORE = 25
private const val ARTIST_WEAK_SCORE = 15
private const val LABEL_SCORE = 8
private const val CATNO_SCORE = 12
private const val VINYL_SCORE = 6

object InboxPipeline {
    fun scoreCandidate(
        queryTitle: String?,
        queryArtist: String?,
        queryCatalogNo: String?,
        queryLabel: String?,
        queryBarcode: String?,
        candidate: ProviderCandidate,
    ): CandidateScore {
        var score = 0
        val reasons = mutableListOf<String>()

        val normalizedQueryTitle = normalizeString(queryTitle)
        val normalizedQueryArtist = normalizeString(queryArtist)
        val normalizedQueryCatalogNo = normalizeString(queryCatalogNo)
        val normalizedQueryLabel = normalizeString(queryLabel)
        val normalizedCandidateTitle = normalizeString(candidate.title)
        val normalizedCandidateArtist = normalizeString(candidate.artist)
        val normalizedCandidateCatalogNo = normalizeString(candidate.catalogNo)
        val normalizedCandidateLabel = normalizeString(candidate.label)
        val queryTitleTokens = normalizeTokensFromNormalized(normalizedQueryTitle)
        val queryArtistTokens = normalizeTokensFromNormalized(normalizedQueryArtist)
        val candidateTitleTokens = normalizeTokensFromNormalized(normalizedCandidateTitle)
        val candidateArtistTokens = normalizeTokensFromNormalized(normalizedCandidateArtist)
        val candidateRawJsonTokens = normalizeTokens(candidate.rawJson)

        val normalizedQueryBarcode = normalizeBarcode(queryBarcode)
        val normalizedCandidateBarcode = normalizeBarcode(candidate.barcode)
        if (!normalizedQueryBarcode.isNullOrBlank() && normalizedQueryBarcode == normalizedCandidateBarcode) {
            score += BARCODE_MATCH_SCORE
            if (queryBarcode != normalizedQueryBarcode) {
                score += BARCODE_NORMALIZED_SCORE
                reasons.add(ReasonCode.BARCODE_NORMALIZED)
            }
            if (isValidBarcode(normalizedQueryBarcode)) {
                score += BARCODE_CHECKSUM_SCORE
                reasons.add(ReasonCode.BARCODE_VALID_CHECKSUM)
            }
        }

        if (matchesNormalizedText(normalizedQueryCatalogNo, normalizedCandidateCatalogNo)) {
            score += CATNO_SCORE
            reasons.add(ReasonCode.CATNO_MATCH)
        }

        if (matchesNormalizedText(normalizedQueryArtist, normalizedCandidateArtist)) {
            score += ARTIST_STRONG_SCORE
            reasons.add(ReasonCode.ARTIST_MATCH)
        } else {
            val artistRatio = tokenOverlapRatio(queryArtistTokens, candidateArtistTokens)
            if (artistRatio >= 0.4) {
                score += ARTIST_WEAK_SCORE
                reasons.add(ReasonCode.WEAK_MATCH_ARTIST)
            }
        }

        if (matchesNormalizedText(normalizedQueryTitle, normalizedCandidateTitle)) {
            score += TITLE_STRONG_SCORE
            reasons.add(ReasonCode.TITLE_MATCH)
        } else {
            val titleRatio = tokenOverlapRatio(queryTitleTokens, candidateTitleTokens)
            if (titleRatio >= 0.4) {
                score += TITLE_WEAK_SCORE
                reasons.add(ReasonCode.WEAK_MATCH_TITLE)
            }
        }

        if (matchesNormalizedText(normalizedQueryLabel, normalizedCandidateLabel)) {
            score += LABEL_SCORE
            reasons.add(ReasonCode.LABEL_MATCH)
        }

        val candidateTokens = candidateTitleTokens + candidateRawJsonTokens
        val vinylMatch = hasVinylIndicator(queryTitleTokens, candidateTokens)
        if (vinylMatch) {
            score += VINYL_SCORE
            reasons.add(ReasonCode.FORMAT_MATCH_VINYL)
        }

        val boundedScore = min(CONFIDENCE_MAX, score)
        val json = ReasonCode.encode(reasons)

        return CandidateScore(
            confidence = boundedScore,
            reasonsJson = json,
        )
    }

    fun shouldAutoCommit(
        topScore: Int,
        secondScore: Int?,
        wasUndone: Boolean,
        hasBarcode: Boolean,
    ): Boolean {
        if (wasUndone) return false
        val minScore = if (hasBarcode) 80 else 90
        if (topScore < minScore) return false
        val gap = secondScore?.let { topScore - it } ?: topScore
        val minGap = if (hasBarcode) 12 else 8
        return gap >= minGap
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

    private fun tokenOverlapRatio(leftTokens: Set<String>, rightTokens: Set<String>): Double {
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0

        val overlap = leftTokens.intersect(rightTokens).size
        if (overlap == 0) return 0.0

        return overlap.toDouble() / maxOf(leftTokens.size, rightTokens.size).toDouble()
    }

    private fun matchesText(left: String?, right: String?): Boolean {
        return matchesNormalizedText(normalizeString(left), normalizeString(right))
    }

    private fun matchesNormalizedText(left: String, right: String): Boolean {
        return left.isNotBlank() && left == right
    }

    private fun normalizeBarcode(value: String?): String? {
        val digits = value
            ?.trim()
            ?.replace(Regex("[^0-9]"), "")
            ?.takeIf { it.isNotBlank() }

        return when (digits?.length) {
            12 -> "0$digits" // UPC-A -> EAN-13 normalization
            13 -> digits
            14 -> digits.drop(1) // EAN-14 -> EAN-13
            else -> digits
        }
    }

    private fun isValidBarcode(value: String): Boolean {
        if (value.length != 12 && value.length != 13) return false
        val digits = value.map { it.digitToInt() }
        val checkDigit = digits.last()
        val payload = digits.dropLast(1).reversed()
        val sum = payload.mapIndexed { index, digit ->
            val weight = if (index % 2 == 0) 3 else 1
            digit * weight
        }.sum()
        val computed = (10 - (sum % 10)) % 10
        return computed == checkDigit
    }

    private fun normalizeString(value: String?): String {
        return value
            ?.trim()
            ?.lowercase()
            ?.let { Normalizer.normalize(it, Normalizer.Form.NFD) }
            ?.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            ?.replace(Regex("[^a-z0-9\\s]"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
    }

    private fun normalizeTokens(value: String?): Set<String> {
        val normalized = normalizeString(value)
        return normalizeTokensFromNormalized(normalized)
    }

    private fun normalizeTokensFromNormalized(normalized: String): Set<String> {
        if (normalized.isBlank()) return emptySet()

        return normalized.split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun hasVinylIndicator(queryTokens: Set<String>, candidateTokens: Set<String>): Boolean {
        val indicators = setOf("vinyl", "lp", "ep", "12", "33", "rpm")
        if (queryTokens.isEmpty()) return false
        val hasMatch = queryTokens.intersect(indicators).isNotEmpty() &&
            candidateTokens.intersect(indicators).isNotEmpty()
        return hasMatch
    }
}
