// file: app/src/main/java/com/zak/pressmark/data/local/repository/ReleaseRepository.kt
package com.zak.pressmark.data.local.repository

import androidx.room.withTransaction
import com.zak.pressmark.data.local.dao.ArtworkDao
import com.zak.pressmark.data.local.dao.ReleaseArtistCreditDao
import com.zak.pressmark.data.local.dao.ReleaseDao
import com.zak.pressmark.data.local.db.AppDatabase
import com.zak.pressmark.data.local.entity.ArtworkEntity
import com.zak.pressmark.data.local.entity.ReleaseArtistCreditEntity
import com.zak.pressmark.data.local.entity.ReleaseEntity

/**
 * Bottom-up repository for the Release-first model.
 *
 * This repository intentionally does NOT resolve/create Artists yet.
 * That will be the next step (we'll extend ArtistDao + add a credit parser).
 */
class ReleaseRepository(
    private val db: AppDatabase,
    private val releaseDao: ReleaseDao = db.releaseDao(),
    private val creditDao: ReleaseArtistCreditDao = db.releaseArtistCreditDao(),
    private val artworkDao: ArtworkDao = db.artworkDao(),
) {

    /**
     * Create or update a release plus its credits and optional artwork, atomically.
     *
     * - Writes ReleaseEntity
     * - Replaces all credits for the release (no leftovers)
     * - Inserts artwork and optionally marks it primary
     */
    suspend fun upsertRelease(
        release: ReleaseEntity,
        credits: List<ReleaseArtistCreditEntity>,
        artworks: List<ArtworkEntity> = emptyList(),
        primaryArtworkId: Long? = null,
    ) {
        db.withTransaction {
            releaseDao.insert(release)

            // Ensure credits use the correct releaseId and are position-ordered.
            val normalizedCredits = credits
                .sortedWith(compareBy<ReleaseArtistCreditEntity> { it.position }.thenBy { it.id })
                .map { it.copy(releaseId = release.id) }

            creditDao.replaceCreditsForRelease(release.id, normalizedCredits)

            if (artworks.isNotEmpty()) {
                val normalizedArtworks = artworks.map { it.copy(releaseId = release.id) }
                artworkDao.insertAll(normalizedArtworks)
            }

            // Optional: set a specific artwork row as primary
            if (primaryArtworkId != null) {
                artworkDao.setPrimaryArtwork(release.id, primaryArtworkId)
            }
        }
    }

    suspend fun listReleases(): List<ReleaseEntity> = releaseDao.listAll()

    suspend fun getRelease(releaseId: String): ReleaseEntity? = releaseDao.getById(releaseId)

    suspend fun deleteRelease(releaseId: String) {
        // CASCADE FKs will clean up credits + artworks automatically.
        releaseDao.deleteById(releaseId)
    }
}
