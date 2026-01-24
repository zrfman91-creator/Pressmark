package com.zak.pressmark.data.remote.discogs

import javax.inject.Inject
import javax.inject.Singleton

data class DiscogsMasterCandidate(
    val masterId: Long,
    val displayTitle: String,
    val subtitle: String?,
    val year: Int?,
    val thumbUrl: String?,
    val coverUrl: String?,
    val genres: List<String>,
    val styles: List<String>,
)

@Singleton
class DiscogsClient @Inject constructor(
    private val api: DiscogsApi,
) {
    suspend fun searchMasters(
        artist: String,
        title: String,
        year: Int?,
        limit: Int,
    ): List<DiscogsMasterCandidate> {
        val resp = api.search(
            artist = artist,
            releaseTitle = title,
            year = year,
            perPage = limit,
            page = 1,
        )

        return resp.results.map { r ->
            DiscogsMasterCandidate(
                masterId = r.id,
                displayTitle = r.title,
                subtitle = buildSubtitle(r.genre, r.style),
                year = r.year,
                thumbUrl = r.thumb,
                coverUrl = r.coverImage,
                genres = r.genre.orEmpty(),
                styles = r.style.orEmpty(),
            )
        }
    }

    suspend fun getMaster(masterId: Long): DiscogsMasterResponse =
        api.getMaster(masterId)

    private fun buildSubtitle(genres: List<String>?, styles: List<String>?): String? {
        val g = genres.orEmpty().joinToString(" • ").takeIf { it.isNotBlank() }
        val s = styles.orEmpty().joinToString(" • ").takeIf { it.isNotBlank() }
        return when {
            g != null && s != null -> "$g • $s"
            g != null -> g
            s != null -> s
            else -> null
        }
    }
}
