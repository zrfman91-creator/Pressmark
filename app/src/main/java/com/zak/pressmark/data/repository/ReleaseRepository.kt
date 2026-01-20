package com.zak.pressmark.data.repository

import androidx.room.withTransaction
import com.zak.pressmark.core.credits.ArtistCreditFormatter
import com.zak.pressmark.data.local.dao.ArtworkDao
import com.zak.pressmark.data.local.dao.ReleaseArtistCreditDao
import com.zak.pressmark.data.local.dao.ReleaseDao
import com.zak.pressmark.data.local.db.AppDatabase
import com.zak.pressmark.data.local.entity.ArtworkEntity
import com.zak.pressmark.data.local.entity.ArtworkKind
import com.zak.pressmark.data.local.entity.ArtworkSource
import com.zak.pressmark.data.local.entity.ReleaseArtistCreditEntity
import com.zak.pressmark.data.local.entity.ReleaseEntity
import com.zak.pressmark.data.local.model.ArtistCreditFormatMapper
import com.zak.pressmark.data.local.model.ReleaseCreditRow
import com.zak.pressmark.data.local.model.ReleaseListItem
import com.zak.pressmark.data.local.model.ReleaseListItemMapper
import com.zak.pressmark.data.model.ReleaseArtwork
import com.zak.pressmark.data.model.ReleaseCredit
import com.zak.pressmark.data.model.ReleaseDetails
import com.zak.pressmark.data.model.ReleaseSummary
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
    private val creditsBuilder: ReleaseArtistCreditsBuilder = ReleaseArtistCreditsBuilder(artistRepository),
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

    suspend fun listReleaseSummaries(): List<ReleaseSummary> =
        listReleaseListItems().map { item -> item.toSummary() }

    fun observeReleaseSummaries(): Flow<List<ReleaseSummary>> =
        observeReleaseListItems().map { items -> items.map { item -> item.toSummary() } }

    fun observeRelease(releaseId: String): Flow<ReleaseEntity?> = releaseDao.observeById(releaseId)

    fun observeCreditsForRelease(releaseId: String): Flow<List<ReleaseArtistCreditEntity>> =
        creditDao.observeCreditsForRelease(releaseId)

    fun observeCreditRowsForRelease(releaseId: String): Flow<List<ReleaseCreditRow>> =
        creditDao.observeCreditRowsForRelease(releaseId)

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

    fun observeReleaseDetailsModel(releaseId: String): Flow<ReleaseDetails?> {
        return combine(
            observeRelease(releaseId),
            observeCreditRowsForRelease(releaseId),
            observeArtworksForRelease(releaseId),
        ) { release, credits, artworks ->
            release?.let { mapReleaseDetails(it, credits, artworks) }
        }
    }

    suspend fun getRelease(releaseId: String): ReleaseEntity? = releaseDao.getById(releaseId)

    suspend fun updateReleaseDetails(
        releaseId: String,
        title: String,
        rawArtist: String,
        releaseYear: Int?,
        label: String?,
        catalogNo: String?,
        format: String?,
        barcode: String?,
        country: String?,
        releaseType: String?,
        notes: String?,
        rating: Int?,
        lastPlayedAt: Long?,
    ): Int {
        return db.withTransaction {
            val updated = releaseDao.updateReleaseDetails(
                releaseId = releaseId,
                title = title.trim(),
                releaseYear = releaseYear,
                label = label?.trim()?.takeIf { it.isNotBlank() },
                catalogNo = catalogNo?.trim()?.takeIf { it.isNotBlank() },
                format = format?.trim()?.takeIf { it.isNotBlank() },
                barcode = barcode?.trim()?.takeIf { it.isNotBlank() },
                country = country?.trim()?.takeIf { it.isNotBlank() },
                releaseType = releaseType?.trim()?.takeIf { it.isNotBlank() },
                notes = notes?.trim()?.takeIf { it.isNotBlank() },
                rating = rating,
                lastPlayedAt = lastPlayedAt,
            )

            val credits = creditsBuilder.buildForRelease(releaseId = releaseId, rawArtist = rawArtist)
            creditDao.replaceCreditsForRelease(releaseId, credits)

            updated
        }
    }

    suspend fun setLocalCover(releaseId: String, coverUri: String?) {
        setArtworkSelection(
            releaseId = releaseId,
            coverUrl = coverUri,
            provider = null,
            providerItemId = null,
        )
    }

    suspend fun setDiscogsCover(
        releaseId: String,
        coverUrl: String?,
        discogsReleaseId: Long?,
    ) {
        setArtworkSelection(
            releaseId = releaseId,
            coverUrl = coverUrl,
            provider = "discogs",
            providerItemId = discogsReleaseId?.toString(),
        )
    }

    suspend fun setArtworkSelection(
        releaseId: String,
        coverUrl: String?,
        provider: String?,
        providerItemId: String?,
    ) {
        val normalizedCover = coverUrl?.trim()?.takeIf { it.isNotBlank() }
        val normalizedProvider = provider?.trim()?.takeIf { it.isNotBlank() }
        val normalizedItemId = providerItemId?.trim()?.takeIf { it.isNotBlank() }

        db.withTransaction {
            updateReleaseArtworkProvider(
                releaseId = releaseId,
                provider = normalizedProvider,
                providerItemId = normalizedItemId,
            )

            if (normalizedCover.isNullOrBlank()) {
                artworkDao.deleteByReleaseId(releaseId)
                return@withTransaction
            }

            val artworkId = artworkDao.insert(
                ArtworkEntity(
                    releaseId = releaseId,
                    uri = normalizedCover,
                    kind = ArtworkKind.COVER_FRONT,
                    source = if (normalizedProvider == "discogs") ArtworkSource.DISCOGS else ArtworkSource.LOCAL,
                    isPrimary = true,
                    createdAt = System.currentTimeMillis(),
                )
            )
            artworkDao.setPrimaryArtwork(releaseId, artworkId)
        }
    }

    suspend fun deleteRelease(releaseId: String) {
        // CASCADE FKs will clean up credits + artworks automatically.
        releaseDao.deleteById(releaseId)
    }

    private suspend fun updateReleaseArtworkProvider(
        releaseId: String,
        provider: String?,
        providerItemId: String?,
    ) {
        val existing = releaseDao.getById(releaseId) ?: return
        val discogsReleaseId =
            if (provider == "discogs") providerItemId?.toLongOrNull() else null

        releaseDao.update(
            existing.copy(
                artworkProvider = provider,
                artworkProviderItemId = providerItemId,
                discogsReleaseId = discogsReleaseId,
            )
        )
    }

    private fun mapReleaseDetails(
        release: ReleaseEntity,
        credits: List<ReleaseCreditRow>,
        artworks: List<ArtworkEntity>,
    ): ReleaseDetails {
        val formatterCredits = credits.map { row ->
            val creditEntity = ReleaseArtistCreditEntity(
                releaseId = release.id,
                artistId = row.artistId,
                role = row.role,
                position = row.position,
                displayHint = row.displayHint,
            )
            ArtistCreditFormatMapper.toFormatterCredit(
                credit = creditEntity,
                artistDisplayName = row.artistDisplayName,
            )
        }

        val artistLine = ArtistCreditFormatter.formatSingleLine(formatterCredits)
        val primaryArtwork = artworks.firstOrNull()

        return ReleaseDetails(
            releaseId = release.id,
            title = release.title,
            artistLine = artistLine,
            releaseYear = release.releaseYear,
            label = release.label,
            catalogNo = release.catalogNo,
            format = release.format,
            barcode = release.barcode,
            country = release.country,
            releaseType = release.releaseType,
            notes = release.notes,
            rating = release.rating,
            addedAt = release.addedAt,
            lastPlayedAt = release.lastPlayedAt,
            artwork = primaryArtwork?.let { artwork ->
                ReleaseArtwork(
                    id = artwork.id,
                    uri = artwork.uri,
                    isPrimary = artwork.isPrimary,
                    kind = artwork.kind.name,
                    source = artwork.source.name,
                    width = artwork.width,
                    height = artwork.height,
                )
            },
            credits = credits.map { row ->
                ReleaseCredit(
                    artistId = row.artistId,
                    artistName = row.artistDisplayName,
                    role = row.role.name,
                    position = row.position,
                    displayHint = row.displayHint,
                )
            },
        )
    }

    private fun ReleaseListItem.toSummary(): ReleaseSummary =
        ReleaseSummary(
            releaseId = release.id,
            title = release.title,
            artistLine = artistLine,
            releaseYear = release.releaseYear,
            artworkUri = artworkUri,
            catalogNo = release.catalogNo,
            barcode = release.barcode,
            label = release.label,
            country = release.country,
            format = release.format,
            releaseType = release.releaseType,
            addedAt = release.addedAt,
        )
}
