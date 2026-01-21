package com.zak.pressmark.data.repository

import com.zak.pressmark.data.model.inbox.CandidateScore
import com.zak.pressmark.data.model.inbox.InboxErrorCode
import com.zak.pressmark.data.model.inbox.ProviderCandidate
import org.json.JSONArray
import org.json.JSONObject
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

        if (!queryBarcode.isNullOrBlank() && queryBarcode == candidate.barcode) {
            score += 50
            reasons.add("barcode_match")
        }

        if (!queryCatalogNo.isNullOrBlank() && queryCatalogNo.equals(candidate.catalogNo, ignoreCase = true)) {
            score += 25
            reasons.add("catalog_no_match")
        }

        if (!queryTitle.isNullOrBlank() && candidate.title.contains(queryTitle, ignoreCase = true)) {
            score += 15
            reasons.add("title_match")
        }

        if (!queryArtist.isNullOrBlank() && candidate.artist.contains(queryArtist, ignoreCase = true)) {
            score += 10
            reasons.add("artist_match")
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
}
