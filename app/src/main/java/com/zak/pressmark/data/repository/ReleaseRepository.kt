// FILE: app/src/main/java/com/zak/pressmark/data/repository/ReleaseRepository.kt
package com.zak.pressmark.data.repository

import androidx.room.withTransaction
import com.zak.pressmark.data.local.dao.ArtworkDao
import com.zak.pressmark.data.local.dao.ReleaseArtistCreditDao
import com.zak.pressmark.data.local.dao.ReleaseDao
import com.zak.pressmark.data.local.db.AppDatabase
import com.zak.pressmark.data.local.entity.ArtworkEntity
import com.zak.pressmark.data.local.entity.ReleaseArtistCreditEntity
import com.zak.pressmark.data.local.entity.ReleaseEntity
import com.zak.pressmark.data.local.model.ReleaseListItem
import com.zak.pressmark.data.local.model.ReleaseListItemMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Bottom-up repository for the Release-first model.
 *
 * Responsibilities:
 * - atomic upsert of ReleaseEntity + credits + optional artwork
 * - credits can be provided as entities OR built from a raw artist string (parser + canonical Artist resolution)
 * - list read-model for main UI via a single flat query (no N+1) + in-memory grouping/formatting
 */
class ReleaseRepository(
    private val db: AppDatabase,
    private val releaseDao: ReleaseDao = db.releaseDao(),
    private val creditDao: ReleaseArtistCreditDao = db.releaseArtistCreditDao(),
    private val artworkDao: ArtworkDao = db.artworkDao(),

    // Defaults keep this plug-and-play with existing call sites.
    private val artistRepository: ArtistRepository = ArtistRepository(db.artistDao()),
    private val creditsBuilder: ReleaseArtistCreditsBuilder = ReleaseArtistCreditsBuilder(
        artistRepository
    ),
) {
    data class ReleaseDetailsSnapshot(
        val release: ReleaseEntity?,
        val credits: List<ReleaseArtistCreditEntity>,
        val artworks: List<ArtworkEntity>,
    )


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
        upsertReleaseInternal(
            release = release,
            creditsProvider = { credits },
            artworks = artworks,
            primaryArtworkId = primaryArtworkId,
        )
    }

    /**
     * Create or update a release plus credits derived from a raw artist string, atomically.
     *
     * This runs parsing + ArtistEntity resolution/creation INSIDE the same transaction as:
     * - inserting the release
     * - replacing credits
     * - inserting artwork
     */
    suspend fun upsertReleaseFromRawArtist(
        release: ReleaseEntity,
        rawArtist: String,
        artworks: List<ArtworkEntity> = emptyList(),
        primaryArtworkId: Long? = null,
    ) {
        upsertReleaseInternal(
            release = release,
            creditsProvider = { creditsBuilder.buildForRelease(releaseId = release.id, rawArtist = rawArtist) },
            artworks = artworks,
            primaryArtworkId = primaryArtworkId,
        )
    }

    private suspend fun upsertReleaseInternal(
        release: ReleaseEntity,
        creditsProvider: suspend () -> List<ReleaseArtistCreditEntity>,
        artworks: List<ArtworkEntity>,
        primaryArtworkId: Long?,
    ) {
        db.withTransaction {
            releaseDao.insert(release)

            val credits = creditsProvider()

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

    /**
     * Main list read-model (no N+1), one-shot:
     * - one DAO query
     * - in-memory grouping (by release.id)
     * - pure formatting (ArtistCreditFormatter)
     */
    suspend fun listReleaseListItems(): List<ReleaseListItem> {
        val rows = releaseDao.listReleaseRowsFlat()
        return ReleaseListItemMapper.fromFlatRows(rows)
    }

    /**
     * Main list read-model (no N+1), live:
     * - one DAO Flow query
     * - in-memory grouping (by release.id)
     * - pure formatting (ArtistCreditFormatter)
     */
    fun observeReleaseListItems(): Flow<List<ReleaseListItem>> {
        return releaseDao
            .observeReleaseRowsFlat()
            .map { rows -> ReleaseListItemMapper.fromFlatRows(rows) }
    }

    fun observeRelease(releaseId: String): Flow<ReleaseEntity?> = releaseDao.observeById(releaseId)

    fun observeCreditsForRelease(releaseId: String): Flow<List<ReleaseArtistCreditEntity>> =
        creditDao.observeCreditsForRelease(releaseId)

    fun observeArtworksForRelease(releaseId: String): Flow<List<ArtworkEntity>> =
        artworkDao.observeArtworksForRelease(releaseId)

    fun observeReleaseDetails(releaseId: String): Flow<ReleaseDetailsSnapshot> {
        return combine(
            observeRelease(releaseId),
            observeCreditsForRelease(releaseId),
            observeArtworksForRelease(releaseId),
        ) { release, credits, artworks ->
            ReleaseDetailsSnapshot(
                release = release,
                credits = credits,
                artworks = artworks,
            )
        }
    }

    suspend fun getRelease(releaseId: String): ReleaseEntity? = releaseDao.getById(releaseId)

    suspend fun deleteRelease(releaseId: String) {
        // CASCADE FKs will clean up credits + artworks automatically.
        releaseDao.deleteById(releaseId)
    }
}
