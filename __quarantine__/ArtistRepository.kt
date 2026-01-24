// FILE: app/src/main/java/com/zak/pressmark/data/repository/ArtistRepository.kt
package com.zak.pressmark.data.repository.v1

import kotlinx.coroutines.flow.Flow
import com.zak.pressmark.core.util.Normalizer
import com.zak.pressmark.data.local.dao.v2.ArtistDao
import com.zak.pressmark.data.local.dao.v2.MergeArtistsResult
import com.zak.pressmark.data.local.entity.v2.ArtistEntity

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
        val canonDisplay = Normalizer.artistDisplay(displayName)
        val canonSort = Normalizer.artistSortNameFromDisplay(canonDisplay)

        val existing = dao.findByNormalizedName(key)
        if (existing != null) {
            if (existing.displayName != canonDisplay || existing.sortName != canonSort) {
                dao.updateNames(existing.id, canonDisplay, canonSort)
            }
            return existing.id
        }

        val entity = ArtistEntity(
            displayName = canonDisplay,
            sortName = canonSort,
            nameNormalized = key,
            // Keep null for now to match your existing model; we'll tighten later.
            artistType = null
        )

        return try {
            dao.insert(entity)
        } catch (_: Throwable) {
            dao.findByNormalizedName(key)?.id
                ?: throw IllegalStateException("Failed to create artist record")
        }
    }

    /**
     * Merge duplicate artist into canonical artist.
     *
     * NOTE: This now uses the new release-credit model:
     * - reassigns ReleaseArtistCredit rows
     * - dedupes collisions
     * - deletes the duplicate artist
     */
    suspend fun mergeArtists(duplicateId: Long, canonicalId: Long): MergeArtistsResult {
        require(duplicateId > 0L) { "duplicateId must be > 0" }
        require(canonicalId > 0L) { "canonicalId must be > 0" }
        require(duplicateId != canonicalId) { "Cannot merge an artist into itself" }

        // Safety: ensure both rows exist before we do anything destructive.
        dao.getById(duplicateId)
            ?: throw IllegalArgumentException("Duplicate artist not found: $duplicateId")

        dao.getById(canonicalId)
            ?: throw IllegalArgumentException("Canonical artist not found: $canonicalId")

        return dao.mergeArtist(duplicateId = duplicateId, canonicalId = canonicalId)
    }

    /**
     * Delete an artist only if it has zero credits.
     *
     * This prevents breaking referential integrity and keeps the artist table pristine.
     */
    suspend fun deleteArtistIfUnused(artistId: Long): DeleteArtistResult {
        require(artistId > 0L) { "artistId must be > 0" }

        val existing = dao.getById(artistId) ?: return DeleteArtistResult.NotFound

        val creditCount = dao.countCredits(existing.id)
        if (creditCount > 0) return DeleteArtistResult.Blocked(creditCount)

        val deleted = dao.deleteById(existing.id)
        return if (deleted > 0) DeleteArtistResult.Deleted else DeleteArtistResult.NotFound
    }
}

sealed class DeleteArtistResult {
    data object Deleted : DeleteArtistResult()
    data object NotFound : DeleteArtistResult()
    data class Blocked(val creditCount: Int) : DeleteArtistResult()
}
