// File: app/src/main/java/com/zak/pressmark/data/remote/discogs/DiscogsArtworkApi.kt
package com.zak.pressmark.data.remote.discogs

import android.util.Log
import com.zak.pressmark.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DiscogsArtworkApi(
    private val token: String,
    private val userAgent: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = "https://api.discogs.com",
    private val minAcceptScore: Int = 35,
    private val debugLogging: Boolean = false,
    private val debugTopN: Int = 3,
    private val logTag: String = "DiscogsArtworkApi",
    private val logger: ((String) -> Unit)? = null,
) {

    data class DiscogsCover(
        val releaseId: Long,
        val coverUrl: String,
        val score: Int,
    )

    suspend fun findCover(
        title: String,
        artist: String?,
        year: Int?,
        catno: String? = null,
        label: String? = null,
    ): DiscogsCover? = withContext(Dispatchers.IO) {
        val qTitle = title.trim()
        val qArtist = artist?.trim().takeIf { !it.isNullOrBlank() }
        val qCatno = catno?.trim().takeIf { !it.isNullOrBlank() }
        val qLabel = label?.trim().takeIf { !it.isNullOrBlank() }

        if (qTitle.isBlank()) return@withContext null

        val url = buildSearchUrl(
            title = qTitle,
            artist = qArtist,
            year = year,
            catno = qCatno,
            label = qLabel,
        )

        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Authorization", "Discogs token=$token")
            .build()

        val response = client.newCall(request).await()

        response.use { r ->
            when (r.code) {
                200 -> parseBestCover(
                    response = r,
                    queryTitle = qTitle,
                    queryArtist = qArtist,
                    queryYear = year,
                    queryCatno = qCatno,
                    queryLabel = qLabel,
                )
                401, 403 -> throw DiscogsAuthException("Discogs auth failed (${r.code}). Check token and User-Agent.")
                429 -> throw DiscogsRateLimitedException(r.header("Retry-After")?.toIntOrNull())
                else -> throw DiscogsHttpException(r.code, "Discogs HTTP ${r.code}: ${r.message}")
            }
        }
    }

    suspend fun findCoverUrl(
        title: String,
        artist: String?,
        year: Int?,
        catno: String? = null,
        label: String? = null,
    ): String? = findCover(title, artist, year, catno, label)?.coverUrl

    private fun buildSearchUrl(
        title: String,
        artist: String?,
        year: Int?,
        catno: String?,
        label: String?,
    ): HttpUrl {
        val base = "$baseUrl/database/search".toHttpUrl()
        val b = base.newBuilder()
            .addQueryParameter("type", "release")
            .addQueryParameter("per_page", "5")
            .addQueryParameter("page", "1")
            .addQueryParameter("release_title", title)

        if (!artist.isNullOrBlank()) b.addQueryParameter("artist", artist)
        if (year != null && year > 0) b.addQueryParameter("year", year.toString())
        if (!catno.isNullOrBlank()) b.addQueryParameter("catno", catno)
        if (!label.isNullOrBlank()) b.addQueryParameter("label", label)

        return b.build()
    }

    private data class Candidate(
        val cover: DiscogsCover,
        val discogsTitle: String,
        val year: Int?,
        val catno: String?,
    )

    private fun parseBestCover(
        response: Response,
        queryTitle: String,
        queryArtist: String?,
        queryYear: Int?,
        queryCatno: String?,
        queryLabel: String?,
    ): DiscogsCover? {
        val body = response.body?.string()?.takeIf { it.isNotBlank() } ?: return null
        val json = JSONObject(body)
        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null

        val qTitleN = norm(queryTitle)
        val qArtistN = queryArtist?.let(::norm)
        val qCatnoN = queryCatno?.let(::norm)
        val qLabelN = queryLabel?.let(::norm)

        val candidates = mutableListOf<Candidate>()

        for (i in 0 until results.length()) {
            val obj = results.optJSONObject(i) ?: continue

            val id = obj.optLong("id", -1L)
            if (id <= 0L) continue

            val coverUrl =
                obj.optString("cover_image").takeIf { it.isNotBlank() }
                    ?: obj.optString("thumb").takeIf { it.isNotBlank() }
                    ?: continue

            val discogsTitle = obj.optString("title").orEmpty()
            val score = scoreResult(
                obj = obj,
                qTitleN = qTitleN,
                qArtistN = qArtistN,
                qYear = queryYear,
                qCatnoN = qCatnoN,
                qLabelN = qLabelN,
            )

            candidates += Candidate(
                cover = DiscogsCover(releaseId = id, coverUrl = coverUrl, score = score),
                discogsTitle = discogsTitle,
                year = obj.optInt("year", -1).takeIf { it > 0 },
                catno = obj.optString("catno").takeIf { it.isNotBlank() },
            )
        }

        if (candidates.isEmpty()) return null

        val ranked = candidates.sortedByDescending { it.cover.score }
        val winner = ranked.first().cover

        debugLogSelection(
            queryTitle = queryTitle,
            queryArtist = queryArtist,
            queryYear = queryYear,
            minAcceptScore = minAcceptScore,
            ranked = ranked,
        )

        if (winner.score < minAcceptScore) return null

        // Prefer release endpoint image if available (often higher fidelity / canonical)
        val releaseImage = fetchReleasePrimaryImageUrl(winner.releaseId)
        return if (!releaseImage.isNullOrBlank()) {
            winner.copy(coverUrl = releaseImage)
        } else {
            winner
        }
    }

    private fun fetchReleasePrimaryImageUrl(releaseId: Long): String? {
        val url = "$baseUrl/releases/$releaseId".toHttpUrl()

        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Authorization", "Discogs token=$token")
            .build()

        val response = client.newCall(request).execute()
        response.use { r ->
            if (!r.isSuccessful) return null
            val body = r.body?.string()?.takeIf { it.isNotBlank() } ?: return null
            val json = JSONObject(body)
            val images = json.optJSONArray("images") ?: return null
            if (images.length() == 0) return null

            val first = images.optJSONObject(0) ?: return null
            return first.optString("uri").takeIf { it.isNotBlank() }
                ?: first.optString("uri150").takeIf { it.isNotBlank() }
        }
    }

    private fun debugLogSelection(
        queryTitle: String,
        queryArtist: String?,
        queryYear: Int?,
        minAcceptScore: Int,
        ranked: List<Candidate>,
    ) {
        if (!debugLogging) return
        if (ranked.isEmpty()) return

        val top = ranked.take(debugTopN.coerceAtLeast(1))
        val winner = top.first()

        val header = buildString {
            append("Discogs pick: qTitle=\"").append(queryTitle).append("\"")
            if (!queryArtist.isNullOrBlank()) append(", qArtist=\"").append(queryArtist).append("\"")
            if (queryYear != null && queryYear > 0) append(", qYear=").append(queryYear)
            append(", minScore=").append(minAcceptScore)
        }
        logLine(header)

        for ((idx, c) in top.withIndex()) {
            val line = buildString {
                append("#").append(idx + 1)
                append(" score=").append(c.cover.score)
                append(" id=").append(c.cover.releaseId)
                if (c.year != null) append(" year=").append(c.year)
                if (!c.catno.isNullOrBlank()) append(" catno=\"").append(c.catno).append("\"")
                append(" title=\"").append(c.discogsTitle).append("\"")
            }
            logLine(line)
        }

        val verdict = if (winner.cover.score >= minAcceptScore) "ACCEPT" else "REJECT"
        logLine("Discogs verdict: $verdict (winnerScore=${winner.cover.score})")
    }

    private fun logLine(message: String) {
        logger?.invoke(message) ?: Log.d(logTag, message)
    }

    private fun scoreResult(
        obj: JSONObject,
        qTitleN: String,
        qArtistN: String?,
        qYear: Int?,
        qCatnoN: String?,
        qLabelN: String?,
    ): Int {
        var score = 0

        val discogsTitleRaw = obj.optString("title").orEmpty()
        val (discogsArtistRaw, discogsReleaseRaw) = splitDiscogsTitle(discogsTitleRaw)
        val dArtistN = discogsArtistRaw.takeIf { it.isNotBlank() }?.let(::norm)
        val dReleaseN = discogsReleaseRaw.takeIf { it.isNotBlank() }?.let(::norm)
        val dTitleWholeN = norm(discogsTitleRaw)

        score += matchScore(qTitleN, dReleaseN) * 3
        score += matchScore(qTitleN, dTitleWholeN)

        if (qArtistN != null) {
            score += matchScore(qArtistN, dArtistN) * 2
            score += matchScore(qArtistN, dTitleWholeN)
        }

        if (qYear != null && qYear > 0) {
            val dYear = obj.optInt("year", -1)
            if (dYear == qYear) score += 12
        }

        if (qCatnoN != null) {
            val dCatnoN = obj.optString("catno").takeIf { it.isNotBlank() }?.let(::norm)
            score += matchScore(qCatnoN, dCatnoN) * 2
        }

        if (qLabelN != null) {
            val labels = obj.optJSONArray("label")
            if (labels != null && labels.anyStringMatches(qLabelN)) score += 8
        }

        return score
    }

    private fun matchScore(q: String, d: String?): Int {
        if (d.isNullOrBlank()) return 0
        if (q == d) return 25
        if (d.contains(q) || q.contains(d)) return 12
        return 0
    }

    private fun splitDiscogsTitle(raw: String): Pair<String, String> {
        val parts = raw.split(" - ", limit = 2)
        return if (parts.size == 2) parts[0] to parts[1] else "" to raw
    }

    private fun norm(s: String): String =
        s.lowercase()
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
            .replace(Regex("""\s+"""), " ")

    private fun JSONArray.anyStringMatches(queryNorm: String): Boolean {
        for (i in 0 until length()) {
            val v = optString(i).takeIf { it.isNotBlank() } ?: continue
            val n = norm(v)
            if (n == queryNorm || n.contains(queryNorm) || queryNorm.contains(n)) return true
        }
        return false
    }
}

class DiscogsAuthException(message: String) : RuntimeException(message)

class DiscogsRateLimitedException(val retryAfterSeconds: Int?) :
    RuntimeException("Discogs rate limited. Retry-After=$retryAfterSeconds")

class DiscogsHttpException(val code: Int, message: String) : RuntimeException(message)

private suspend fun Call.await(): Response =
    suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { runCatching { cancel() } }
        enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                cont.resume(response)
            }
        })
    }
