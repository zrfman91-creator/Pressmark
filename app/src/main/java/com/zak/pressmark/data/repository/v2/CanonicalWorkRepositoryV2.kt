package com.zak.pressmark.data.repository.v2

import com.zak.pressmark.core.util.completion.CompletionRules
import com.zak.pressmark.data.local.dao.v2.ArtistDaoV2
import com.zak.pressmark.data.local.dao.v2.CanonicalWorkDaoV2
import com.zak.pressmark.data.local.entity.v2.ArtistEntityV2
import com.zak.pressmark.data.local.entity.v2.CanonicalWorkEntityV2
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalWorkRepositoryV2 @Inject constructor(
    private val artistDao: ArtistDaoV2,
    private val canonicalWorkDao: CanonicalWorkDaoV2,
) {

    suspend fun getOrCreateArtistId(
        name: String,
        discogsArtistId: Long? = null,
    ): String {
        val normalized = normalize(name)
        val existing = artistDao.getByNormalized(normalized)
        if (existing != null) return existing.id

        val now = System.currentTimeMillis()
        val id = "artist:${sha1(normalized)}"
        val entity = ArtistEntityV2(
            id = id,
            name = name.trim(),
            nameNormalized = normalized,
            discogsArtistId = discogsArtistId,
            createdAt = now,
            updatedAt = now,
        )
        artistDao.upsert(entity)
        return id
    }

    suspend fun upsertCanonicalWork(
        artistId: String,
        discogsMasterId: Long?,
        title: String,
        year: Int?,
        formatType: String?,
        releaseType: String,
    ): String {
        val now = System.currentTimeMillis()
        val normalizedTitle = normalize(title)
        val existing = discogsMasterId?.let { canonicalWorkDao.getByDiscogsMasterId(it) }
        val id = existing?.id ?: when (discogsMasterId) {
            null -> "canonical:${sha1("$artistId|$normalizedTitle|$year")}"
            else -> "canonical:discogsMaster:$discogsMasterId"
        }

        val entity = CanonicalWorkEntityV2(
            id = id,
            artistId = artistId,
            title = title.trim(),
            titleNormalized = normalizedTitle,
            year = year,
            formatType = formatType,
            releaseType = releaseType,
            discogsMasterId = discogsMasterId,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        canonicalWorkDao.upsert(entity)
        return id
    }

    suspend fun upsertCanonicalWorkFromDiscogs(
        artistName: String,
        discogsMasterId: Long,
        title: String,
        year: Int?,
        formatType: String?,
        releaseType: String,
    ): String {
        val artistId = getOrCreateArtistId(name = artistName)
        val resolvedFormat = formatType ?: CompletionRules.FORMAT_LP
        return upsertCanonicalWork(
            artistId = artistId,
            discogsMasterId = discogsMasterId,
            title = title,
            year = year,
            formatType = resolvedFormat,
            releaseType = releaseType,
        )
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").trim()

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
