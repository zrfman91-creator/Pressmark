package com.zak.pressmark.data.repository

import kotlinx.coroutines.flow.Flow

import com.zak.pressmark.data.local.dao.MergeArtistsResult
import com.zak.pressmark.core.util.Normalizer
import com.zak.pressmark.data.local.dao.ArtistDao
import com.zak.pressmark.data.local.entity.ArtistEntity

class ArtistRepository(
    private val dao: ArtistDao
) {

    fun observeTopArtists(limit: Int = 20): Flow<List<ArtistEntity>> =
        dao.observeTopArtists(limit)

    fun searchByName(query: String, limit: Int = 20): Flow<List<ArtistEntity>> =
        dao.searchByName(query, limit)

    fun observeById(artistId: Long): Flow<ArtistEntity?> =
        dao.observeById(artistId)

    suspend fun getOrCreateArtistId(displayName: String): Long {
        val key = Normalizer.artistKey(displayName)
        val canon = Normalizer.artistDisplay(displayName)

        val existing = dao.findByNormalizedName(key)
        if (existing != null) {
            if (existing.displayName != canon || existing.sortName != canon) {
                dao.updateNames(existing.id, canon, canon)
            }
            return existing.id
        }
        val entity = ArtistEntity(
            displayName = canon,
            sortName = canon,
            nameNormalized = key,
            artistType = null
        )
        return try {
            dao.insert(entity)
        } catch (_: Throwable) {
            dao.findByNormalizedName(key)?.id
                ?: throw IllegalStateException("Failed to create artist record")
        }
    }

    suspend fun mergeArtists(duplicateId: Long, canonicalId: Long): MergeArtistsResult {
        require(duplicateId > 0L) { "duplicateId must be > 0" }
        require(canonicalId > 0L) { "canonicalId must be > 0" }
        require(duplicateId != canonicalId) { "Cannot merge an artist into itself" }

        // Safety: ensure both rows exist before we do anything destructive.
        val dup = dao.getById(duplicateId)
            ?: throw IllegalArgumentException("Duplicate artist not found: $duplicateId")

        val canon = dao.getById(canonicalId)
            ?: throw IllegalArgumentException("Canonical artist not found: $canonicalId")

        // Optional extra safety: if canon is "hidden"/special later, you can block merges here.
        return dao.mergeAndDeleteArtist(duplicateId = dup.id, canonicalId = canon.id)
    }
}
