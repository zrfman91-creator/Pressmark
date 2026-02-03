package com.zak.pressmark.domain.artwork

import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.remote.discogs.primaryFullUrl
import com.zak.pressmark.data.remote.discogs.primaryThumbUrl
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Singleton
class ArtworkResolver @Inject constructor(
    private val discogsApi: DiscogsApiService,
) {

    data class ResolvedArtwork(
        val url: String,
        val provider: Provider,
        val confidence: Int,
        val releaseId: Long? = null,
    )

    enum class Provider { DISCOGS }

    suspend fun resolveDiscogsCover(
        title: String,
        artist: String?,
        year: Int?,
        catno: String? = null,
        label: String? = null,
        barcode: String? = null,
    ): ResolvedArtwork? {
        val qTitle = title.trim()
        if (qTitle.isBlank()) return null

        val resp = discogsApi.searchReleases(
            artist = artist?.trim()?.takeIf { it.isNotBlank() },
            releaseTitle = qTitle,
            year = year,
            label = label?.trim()?.takeIf { it.isNotBlank() },
            catno = catno?.trim()?.takeIf { it.isNotBlank() },
            barcode = barcode?.trim()?.takeIf { it.isNotBlank() },
            perPage = 10,
            page = 1,
        )

        if (resp.results.isEmpty()) return null

        val scored = resp.results.map { r ->
            val score = scoreResult(
                queryTitle = qTitle,
                queryArtist = artist,
                queryYear = year,
                resultTitle = r.title,
                resultYear = r.year,
            )
            Triple(r, score, r.coverImage ?: r.thumb)
        }.sortedByDescending { it.second }

        val best = scored.firstOrNull() ?: return null
        val bestResult = best.first
        val bestScore = best.second

        val url = best.third
            ?: runCatching {
                val release = discogsApi.getRelease(bestResult.id)
                release.images.primaryFullUrl() ?: release.images.primaryThumbUrl()
            }.getOrNull()

        val finalUrl = url?.trim()?.takeIf { it.isNotBlank() } ?: return null

        return ResolvedArtwork(
            url = finalUrl,
            provider = Provider.DISCOGS,
            confidence = bestScore,
            releaseId = bestResult.id,
        )
    }

    /**
     * Resolve artwork for a known Discogs release ID.
     * Use this when the user has already selected a release candidate (e.g., refine pressing).
     */
    suspend fun resolveDiscogsReleaseArt(
        releaseId: Long,
        fallbackUrl: String? = null,
    ): ResolvedArtwork? {
        val resolvedUrl = runCatching {
            val release = discogsApi.getRelease(releaseId)
            release.images.primaryFullUrl() ?: release.images.primaryThumbUrl()
        }.getOrNull()
            ?: fallbackUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: return null

        return ResolvedArtwork(
            url = resolvedUrl,
            provider = Provider.DISCOGS,
            confidence = 100,
            releaseId = releaseId,
        )
    }

    private fun scoreResult(
        queryTitle: String,
        queryArtist: String?,
        queryYear: Int?,
        resultTitle: String,
        resultYear: Int?,
    ): Int {
        var score = 0

        val qt = queryTitle.lowercase()
        val rt = resultTitle.lowercase()

        score += when {
            rt == qt -> 60
            rt.contains(qt) -> 45
            qt.contains(rt) -> 35
            else -> 0
        }

        val qa = queryArtist?.trim()?.lowercase()
        if (!qa.isNullOrBlank()) {
            score += when {
                rt.startsWith(qa) -> 20
                rt.contains(qa) -> 10
                else -> 0
            }
        }

        if (queryYear != null && resultYear != null) {
            val diff = abs(queryYear - resultYear)
            score += when {
                diff == 0 -> 15
                diff == 1 -> 10
                diff <= 3 -> 5
                else -> 0
            }
        }

        return min(max(score, 0), 100)
    }
}
