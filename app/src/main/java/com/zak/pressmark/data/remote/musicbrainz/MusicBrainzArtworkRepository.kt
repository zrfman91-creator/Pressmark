// =======================================================
// file: app/src/main/java/com/zak/pressmark/data/remote/musicbrainz/MusicBrainzArtworkRepository.kt
// =======================================================
package com.zak.pressmark.data.remote.musicbrainz

import android.util.Log
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.data.local.entity.AlbumEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * MusicBrainz artwork lookup:
 * - Searches MusicBrainz for a likely matching release (MBID)
 * - Fetches cover art via the Cover Art Archive (CAA)
 *
 * Notes:
 * - MusicBrainz requires a descriptive User-Agent that includes a way to contact you.
 *   Example: "Pressmark/0.2.0 (wanderingbogeygrips@gmail.com)"
 */
data class MusicBrainzArtwork(
    val releaseMbid: String,
    val coverUrl: String,
    val score: Int,
)

interface MusicBrainzArtworkRepository {
    suspend fun getArtwork(album: AlbumEntity): MusicBrainzArtwork?
    suspend fun getCoverUrl(album: AlbumEntity): String? = getArtwork(album)?.coverUrl
}

class DefaultMusicBrainzArtworkRepository(
    private val api: MusicBrainzArtworkApi,
) : MusicBrainzArtworkRepository {

    override suspend fun getArtwork(album: AlbumEntity): MusicBrainzArtwork? {
        val title = album.title.trim()
        if (title.isBlank()) return null

        val result = api.findCover(
            title = title,
            artist = album.artist.trim().takeIf { it.isNotBlank() },
            year = album.releaseYear,
            catno = album.catalogNo?.trim().takeIf { !it.isNullOrBlank() },
            label = album.label?.trim().takeIf { !it.isNullOrBlank() },
        ) ?: return null

        return MusicBrainzArtwork(
            releaseMbid = result.releaseMbid,
            coverUrl = result.coverUrl,
            score = result.score,
        )
    }
}

/**
 * Thin API wrapper around:
 * - MusicBrainz Search: https://musicbrainz.org/doc/MusicBrainz_API/Search
 * - Cover Art Archive: https://musicbrainz.org/doc/Cover_Art_Archive/API
 */
class MusicBrainzArtworkApi(
    private val userAgent: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val mbBaseUrl: String = "https://musicbrainz.org",
    private val caaBaseUrl: String = "https://coverartarchive.org",
    private val minAcceptScore: Int = 70,
    private val debugLogging: Boolean = false,
    private val debugTopN: Int = 3,
    private val logTag: String = "MusicBrainzArtworkApi",
) {

    data class MusicBrainzCover(
        val releaseMbid: String,
        val coverUrl: String,
        val score: Int,
        val mbTitle: String,
        val mbArtist: String?,
        val mbYear: Int?,
    )

    suspend fun findCover(
        title: String,
        artist: String?,
        year: Int?,
        catno: String? = null,
        label: String? = null,
    ): MusicBrainzCover? = withContext(Dispatchers.IO) {
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
                429, 503 -> throw MusicBrainzRateLimitedException(r.header("Retry-After")?.toIntOrNull())
                else -> throw MusicBrainzHttpException(r.code, "MusicBrainz HTTP ${r.code}: ${r.message}")
            }
        }
    }

    private fun buildSearchUrl(
        title: String,
        artist: String?,
        year: Int?,
        catno: String?,
        label: String?,
    ) = "$mbBaseUrl/ws/2/release/".toHttpUrl().newBuilder()
        .addQueryParameter("fmt", "json")
        .addQueryParameter("limit", "5")
        .addQueryParameter("query", buildLuceneQuery(title, artist, year, catno, label))
        .build()

    private fun buildLuceneQuery(
        title: String,
        artist: String?,
        year: Int?,
        catno: String?,
        label: String?,
    ): String {
        val parts = mutableListOf<String>()
        parts += "release:\"${escapeLucene(title)}\""

        if (!artist.isNullOrBlank()) {
            parts += "artist:\"${escapeLucene(artist)}\""
        }
        if (year != null && year > 0) {
            parts += "date:$year"
        }
        if (!catno.isNullOrBlank()) {
            parts += "catno:\"${escapeLucene(catno)}\""
        }
        if (!label.isNullOrBlank()) {
            parts += "label:\"${escapeLucene(label)}\""
        }

        return parts.joinToString(" AND ")
    }

    private fun escapeLucene(raw: String): String =
        raw
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

    private data class Candidate(
        val mbid: String,
        val title: String,
        val artist: String?,
        val year: Int?,
        val score: Int,
    )

    private fun parseBestCover(
        response: Response,
        queryTitle: String,
        queryArtist: String?,
        queryYear: Int?,
        queryCatno: String?,
        queryLabel: String?,
    ): MusicBrainzCover? {
        val body = response.body?.string()?.takeIf { it.isNotBlank() } ?: return null
        val json = JSONObject(body)
        val releases = json.optJSONArray("releases") ?: return null
        if (releases.length() == 0) return null

        val qTitleN = norm(queryTitle)
        val qArtistN = queryArtist?.let(::norm)
        val qCatnoN = queryCatno?.let(::norm)
        val qLabelN = queryLabel?.let(::norm)

        val candidates = mutableListOf<Candidate>()

        for (i in 0 until releases.length()) {
            val obj = releases.optJSONObject(i) ?: continue
            val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue

            val mbTitle = obj.optString("title").orEmpty()
            val mbArtist = parsePrimaryArtist(obj.optJSONArray("artist-credit"))
            val mbYear = parseYear(obj.optString("date"))

            val apiScore = obj.optInt("score", 0).coerceIn(0, 100)

            var score = apiScore
            if (norm(mbTitle) == qTitleN) score += 20
            if (!qArtistN.isNullOrBlank() && mbArtist != null && norm(mbArtist) == qArtistN) score += 15
            if (queryYear != null && mbYear != null && queryYear == mbYear) score += 10

            if (!qCatnoN.isNullOrBlank()) {
                val mbCatnos = parseAllCatnos(obj.optJSONArray("label-info"))
                if (mbCatnos.any { norm(it) == qCatnoN }) score += 10
            }
            if (!qLabelN.isNullOrBlank()) {
                val mbLabels = parseAllLabels(obj.optJSONArray("label-info"))
                if (mbLabels.any { norm(it) == qLabelN }) score += 5
            }

            candidates += Candidate(
                mbid = id,
                title = mbTitle,
                artist = mbArtist,
                year = mbYear,
                score = score.coerceAtMost(130),
            )
        }

        if (candidates.isEmpty()) return null

        val ranked = candidates.sortedByDescending { it.score }
        if (debugLogging) {
            Log.d(logTag, buildString {
                appendLine("MB query: title=\"$queryTitle\" artist=\"${queryArtist.orEmpty()}\" year=${queryYear ?: ""}")
                val top = ranked.take(debugTopN)
                top.forEachIndexed { idx, c ->
                    appendLine("  #${idx + 1} score=${c.score} title=\"${c.title}\" artist=\"${c.artist.orEmpty()}\" year=${c.year ?: ""} mbid=${c.mbid}")
                }
                appendLine("Min accept score: $minAcceptScore")
            })
        }

        val winner = ranked.first()
        if (winner.score < minAcceptScore) return null

        val coverUrl = fetchCoverArtFrontImageUrl(winner.mbid) ?: return null

        return MusicBrainzCover(
            releaseMbid = winner.mbid,
            coverUrl = coverUrl,
            score = winner.score,
            mbTitle = winner.title,
            mbArtist = winner.artist,
            mbYear = winner.year,
        )
    }

    private fun parsePrimaryArtist(artistCredit: JSONArray?): String? {
        if (artistCredit == null || artistCredit.length() == 0) return null
        val first = artistCredit.optJSONObject(0) ?: return null

        return first.optString("name").takeIf { it.isNotBlank() }
            ?: first.optJSONObject("artist")?.optString("name")?.takeIf { it.isNotBlank() }
    }

    private fun parseYear(date: String?): Int? {
        if (date.isNullOrBlank()) return null
        val yyyy = date.trim().take(4)
        return yyyy.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun parseAllCatnos(labelInfo: JSONArray?): List<String> {
        if (labelInfo == null || labelInfo.length() == 0) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until labelInfo.length()) {
            val obj = labelInfo.optJSONObject(i) ?: continue
            val catno = obj.optString("catalog-number").takeIf { it.isNotBlank() }
            if (catno != null) out += catno
        }
        return out
    }

    private fun parseAllLabels(labelInfo: JSONArray?): List<String> {
        if (labelInfo == null || labelInfo.length() == 0) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until labelInfo.length()) {
            val obj = labelInfo.optJSONObject(i) ?: continue
            val label = obj.optJSONObject("label")?.optString("name")?.takeIf { it.isNotBlank() }
            if (label != null) out += label
        }
        return out
    }

    private fun fetchCoverArtFrontImageUrl(releaseMbid: String): String? {
        val url = "$caaBaseUrl/release/$releaseMbid".toHttpUrl()

        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        response.use { r ->
            if (r.code == 404) return null
            if (r.code == 429 || r.code == 503) throw MusicBrainzRateLimitedException(r.header("Retry-After")?.toIntOrNull())
            if (!r.isSuccessful) throw MusicBrainzHttpException(r.code, "CAA HTTP ${r.code}: ${r.message}")

            val body = r.body?.string()?.takeIf { it.isNotBlank() } ?: return null
            val json = JSONObject(body)
            val images = json.optJSONArray("images") ?: return null
            if (images.length() == 0) return null

            var best: JSONObject? = null
            for (i in 0 until images.length()) {
                val img = images.optJSONObject(i) ?: continue
                if (img.optBoolean("front", false)) {
                    best = img
                    break
                }
            }
            if (best == null) best = images.optJSONObject(0)

            return best?.optString("image")?.takeIf { it.isNotBlank() }
        }
    }

    private fun norm(s: String): String =
        s.lowercase(Locale.US)
            .replace("&", "and")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}

class MusicBrainzHttpException(val code: Int, message: String) : IOException(message)

class MusicBrainzRateLimitedException(val retryAfterSeconds: Int?) :
    IOException("MusicBrainz rate limited (Retry-After=$retryAfterSeconds)")

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { try { cancel() } catch (_: Throwable) {} }

    enqueue(object : okhttp3.Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (cont.isCancelled) return
            cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }
    })
}
