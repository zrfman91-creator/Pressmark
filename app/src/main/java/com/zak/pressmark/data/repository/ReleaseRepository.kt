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
import com.zak.pressmark.data.model.ReleaseDiscogsCandidate
import com.zak.pressmark.data.model.ReleaseDiscogsExtras
import com.zak.pressmark.data.model.ReleaseMarketPrice
import com.zak.pressmark.data.model.ReleaseSummary
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.remote.discogs.DiscogsMarketplacePrice
import com.zak.pressmark.data.remote.discogs.DiscogsRelease
import com.zak.pressmark.data.remote.discogs.DiscogsSearchResult
import com.zak.pressmark.data.remote.discogs.toReleaseMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

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
    private val discogsApiService: DiscogsApiService? = null,
    private val catalogRepository: CatalogRepository? = null,
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
            artistLineOverride = null,
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
            artistLineOverride = rawArtist,
        )
    }

    private suspend fun upsertReleaseInternal(
        release: ReleaseEntity,
        creditsProvider: suspend () -> List<ReleaseArtistCreditEntity>,
        artworks: List<ArtworkEntity>,
        primaryArtworkId: Long?,
        artistLineOverride: String?,
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

        catalogRepository?.upsertFromRelease(
            releaseId = release.id,
            artistLineOverride = artistLineOverride,
        )
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
        val updated = db.withTransaction {
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
        if (updated > 0) {
            catalogRepository?.upsertFromRelease(releaseId = releaseId, artistLineOverride = rawArtist)
        }
        return updated
    }

    suspend fun updateReleaseMetadata(
        releaseId: String,
        releaseYear: Int?,
        label: String?,
        catalogNo: String?,
        format: String?,
        country: String?,
        releaseType: String?,
        notes: String?,
        discogsReleaseId: Long?,
    ): Boolean {
        val updated = db.withTransaction {
            val existing = releaseDao.getById(releaseId) ?: return@withTransaction false
            releaseDao.update(
                existing.copy(
                    releaseYear = releaseYear,
                    label = label?.trim()?.takeIf { it.isNotBlank() },
                    catalogNo = catalogNo?.trim()?.takeIf { it.isNotBlank() },
                    format = format?.trim()?.takeIf { it.isNotBlank() },
                    country = country?.trim()?.takeIf { it.isNotBlank() },
                    releaseType = releaseType?.trim()?.takeIf { it.isNotBlank() },
                    notes = notes?.trim()?.takeIf { it.isNotBlank() },
                    discogsReleaseId = discogsReleaseId ?: existing.discogsReleaseId,
                )
            )
            true
        }
        if (updated) {
            catalogRepository?.upsertFromRelease(releaseId = releaseId)
        }
        return updated
    }

    suspend fun setLocalCover(releaseId: String, coverUri: String?) {
        setArtworkSelection(
            releaseId = releaseId,
            coverUrl = coverUri,
            provider = null,
            providerItemId = null,
        )
    }

    suspend fun upsertFromProvider(
        provider: String,
        providerItemId: String,
    ): String? {
        return when (provider.lowercase()) {
            "discogs" -> upsertFromDiscogs(providerItemId)
            else -> null
        }
    }

    private suspend fun upsertFromDiscogs(providerItemId: String): String? {
        val api = discogsApiService ?: return null
        val discogsReleaseId = providerItemId.toLongOrNull() ?: return null
        val release = api.getRelease(discogsReleaseId)
        val existing = releaseDao.getByDiscogsReleaseId(discogsReleaseId)

        val artistRaw = release.artists?.joinToString(", ") { it.name.trim() }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown artist"
        val label = release.labels?.firstOrNull()?.name?.trim()?.takeIf { it.isNotBlank() }
        val catalogNo = release.labels?.firstOrNull()?.catalogNumber?.trim()?.takeIf { it.isNotBlank() }
        val barcode = release.identifiers
            ?.firstOrNull { it.type?.equals("barcode", ignoreCase = true) == true }
            ?.value
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val metadata = release.toReleaseMetadata()
        val primaryImage = release.images
            ?.firstOrNull { it.type.equals("primary", ignoreCase = true) }
            ?: release.images?.firstOrNull()

        val releaseEntity = ReleaseEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = release.title.trim(),
            releaseYear = release.year,
            genre = existing?.genre ?: metadata.genres?.firstOrNull(),
            label = label,
            catalogNo = catalogNo,
            barcode = barcode,
            country = metadata.country,
            releaseType = metadata.releaseType,
            format = metadata.format,
            discogsReleaseId = release.id,
            masterId = release.masterId,
            artworkProvider = existing?.artworkProvider,
            artworkProviderItemId = existing?.artworkProviderItemId,
            notes = metadata.notes ?: existing?.notes,
            rating = existing?.rating,
            addedAt = existing?.addedAt ?: System.currentTimeMillis(),
            lastPlayedAt = existing?.lastPlayedAt,
        )

        upsertReleaseFromRawArtist(
            release = releaseEntity,
            rawArtist = artistRaw,
        )

        primaryImage?.uri?.trim()?.takeIf { it.isNotBlank() }?.let { coverUrl ->
            setDiscogsCover(
                releaseId = releaseEntity.id,
                coverUrl = coverUrl,
                discogsReleaseId = release.id,
            )
        }

        return releaseEntity.id
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

        catalogRepository?.upsertFromRelease(releaseId = releaseId)
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
            discogsReleaseId = release.discogsReleaseId,
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

    suspend fun fetchDiscogsExtras(releaseId: String): ReleaseDiscogsExtras? {
        val api = discogsApiService ?: return null
        val release = releaseDao.getById(releaseId) ?: return null
        val discogsReleaseId = release.discogsReleaseId ?: return null

        return runCatching {
            val discogsRelease = api.getRelease(discogsReleaseId)
            val stats = runCatching { api.getMarketplaceStats(discogsReleaseId) }.getOrNull()

            ReleaseDiscogsExtras(
                genres = discogsRelease.genres?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                styles = discogsRelease.styles?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                lastSoldDate = stats?.lastSoldDate,
                lowestPrice = stats?.lowestPrice?.toMarketPrice(),
                medianPrice = stats?.medianPrice?.toMarketPrice(),
                highestPrice = stats?.highestPrice?.toMarketPrice(),
            )
        }.getOrNull()
    }

    private fun DiscogsMarketplacePrice.toMarketPrice(): ReleaseMarketPrice? {
        val amount = value ?: return null
        val currencyCode = currency?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return ReleaseMarketPrice(
            value = amount,
            currency = currencyCode,
        )
    }

    suspend fun searchDiscogsCandidates(
        title: String,
        artist: String,
        releaseYear: Int?,
        label: String?,
        catalogNo: String?,
        barcode: String?,
    ): List<ReleaseDiscogsCandidate> {
        val api = discogsApiService ?: return emptyList()

        val response = api.searchReleases(
            artist = artist.takeIf { it.isNotBlank() },
            releaseTitle = title.takeIf { it.isNotBlank() },
            label = label?.takeIf { it.isNotBlank() },
            catno = catalogNo?.takeIf { it.isNotBlank() },
            perPage = 15,
        )

        val scored = response.results
            .take(10)
            .mapNotNull { result ->
                val discogsRelease = runCatching { api.getRelease(result.id) }.getOrNull()
                    ?: return@mapNotNull null
                buildDiscogsCandidate(
                    searchResult = result,
                    release = discogsRelease,
                    title = title,
                    artist = artist,
                    releaseYear = releaseYear,
                    label = label,
                    catalogNo = catalogNo,
                    barcode = barcode,
                )
            }

        return scored
            .sortedWith(
                compareByDescending<ReleaseDiscogsCandidate> { it.isVinyl }
                    .thenByDescending { it.confidenceScore }
                    .thenBy { it.title }
            )
    }

    suspend fun applyDiscogsCandidateFillMissing(
        releaseId: String,
        candidate: ReleaseDiscogsCandidate,
    ): Boolean {
        return db.withTransaction {
            val existing = releaseDao.getById(releaseId) ?: return@withTransaction false

            fun pick(existingValue: String?, candidateValue: String?): String? =
                existingValue?.takeIf { it.isNotBlank() } ?: candidateValue

            fun pickInt(existingValue: Int?, candidateValue: Int?): Int? =
                existingValue ?: candidateValue

            releaseDao.update(
                existing.copy(
                    releaseYear = pickInt(existing.releaseYear, candidate.year),
                    label = pick(existing.label, candidate.label),
                    catalogNo = pick(existing.catalogNo, candidate.catalogNo),
                    format = pick(existing.format, candidate.format),
                    barcode = pick(existing.barcode, candidate.barcode),
                    country = pick(existing.country, candidate.country),
                    releaseType = pick(existing.releaseType, candidate.releaseType),
                    notes = pick(existing.notes, candidate.notes),
                    discogsReleaseId = existing.discogsReleaseId ?: candidate.discogsReleaseId,
                )
            )
            true
        }
    }

    private fun buildDiscogsCandidate(
        searchResult: DiscogsSearchResult,
        release: DiscogsRelease,
        title: String,
        artist: String,
        releaseYear: Int?,
        label: String?,
        catalogNo: String?,
        barcode: String?,
    ): ReleaseDiscogsCandidate {
        val releaseLabel = release.labels?.firstOrNull()?.name?.trim()
        val releaseCatalogNo = release.labels?.firstOrNull()?.catalogNumber?.trim()
        val format = normalizeDiscogsFormat(release.formats)
        val isVinyl = format?.let { isVinylFormat(it) } ?: false
        val releaseArtist = release.artists?.joinToString(", ") { it.name.trim() }.orEmpty()
        val releaseBarcode = release.identifiers
            ?.firstOrNull { it.type?.equals("barcode", ignoreCase = true) == true }
            ?.value
            ?.trim()
        val releaseImage = release.images
            ?.firstOrNull { it.type.equals("primary", ignoreCase = true) }
            ?: release.images?.firstOrNull()
        val coverUrl = releaseImage?.uri?.trim()?.takeIf { it.isNotBlank() }
        val thumbUrl = releaseImage?.uri150?.trim()?.takeIf { it.isNotBlank() }

        val confidence = confidenceScore(
            title = title,
            artist = artist,
            releaseYear = releaseYear,
            label = label,
            catalogNo = catalogNo,
            barcode = barcode,
            candidateTitle = searchResult.title,
            candidateArtist = releaseArtist,
            candidateYear = release.year,
            candidateLabel = releaseLabel,
            candidateCatalogNo = releaseCatalogNo,
            candidateBarcode = releaseBarcode,
        )

        val genres = release.genres?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val styles = release.styles?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

        return ReleaseDiscogsCandidate(
            discogsReleaseId = release.id,
            title = searchResult.title ?: release.title,
            artist = releaseArtist,
            year = release.year,
            label = releaseLabel,
            catalogNo = releaseCatalogNo,
            format = format,
            barcode = releaseBarcode,
            coverUrl = coverUrl,
            thumbUrl = thumbUrl,
            country = release.country?.trim()?.takeIf { it.isNotBlank() },
            releaseType = normalizeReleaseType(release.styles, release.genres),
            notes = release.notes?.trim()?.takeIf { it.isNotBlank() },
            genres = genres,
            styles = styles,
            confidenceScore = confidence,
            isVinyl = isVinyl,
        )
    }

    private fun confidenceScore(
        title: String,
        artist: String,
        releaseYear: Int?,
        label: String?,
        catalogNo: String?,
        barcode: String?,
        candidateTitle: String?,
        candidateArtist: String?,
        candidateYear: Int?,
        candidateLabel: String?,
        candidateCatalogNo: String?,
        candidateBarcode: String?,
    ): Int {
        var score = 0
        if (normBarcode(barcode) != null && normBarcode(barcode) == normBarcode(candidateBarcode)) score += 6
        if (norm(catalogNo) != null && norm(catalogNo) == norm(candidateCatalogNo)) score += 5
        if (norm(label) != null && norm(label) == norm(candidateLabel)) score += 4
        if (norm(title) != null && norm(title) == norm(candidateTitle)) score += 4
        if (norm(artist) != null && norm(artist) == norm(candidateArtist)) score += 4
        if (releaseYear != null && releaseYear == candidateYear) score += 3
        return score
    }

    private fun norm(value: String?): String? =
        value?.trim()?.lowercase()?.replace(Regex("[^a-z0-9]"), "")?.takeIf { it.isNotBlank() }

    private fun normBarcode(value: String?): String? =
        value?.trim()?.replace(Regex("[^0-9]"), "")?.takeIf { it.isNotBlank() }

    private fun normalizeDiscogsFormat(formats: List<com.zak.pressmark.data.remote.discogs.DiscogsFormat>?): String? {
        val name = formats
            ?.firstOrNull()
            ?.name
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val lower = name.lowercase()
        return when {
            lower.contains("7") && lower.contains("inch") -> "7\""
            lower.contains("12") && lower.contains("inch") -> "12\""
            lower.contains("lp") || lower.contains("long play") -> "LP"
            lower.contains("ep") -> "EP"
            lower.contains("cd") -> "CD"
            lower.contains("cassette") -> "CASSETTE"
            lower.contains("vinyl") -> "VINYL"
            lower.contains("dvd") -> "DVD"
            lower.contains("blu-ray") || lower.contains("bluray") -> "BLU-RAY"
            lower.contains("file") || lower.contains("digital") -> "DIGITAL"
            else -> name.uppercase()
        }
    }

    private fun isVinylFormat(format: String): Boolean {
        val normalized = format.uppercase()
        return normalized.contains("VINYL") ||
            normalized == "LP" ||
            normalized == "EP" ||
            normalized.contains("7\"") ||
            normalized.contains("12\"")
    }

    private fun normalizeReleaseType(styles: List<String>?, genres: List<String>?): String? {
        val tokens = (styles.orEmpty() + genres.orEmpty())
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return null

        val normalized = tokens.map { it.lowercase() }

        return when {
            normalized.any { it.contains("live") } -> "LIVE"
            normalized.any { it.contains("compilation") } -> "COMPILATION"
            normalized.any { it.contains("soundtrack") } -> "SOUNDTRACK"
            normalized.any { it.contains("greatest hits") || it.contains("best of") } -> "GREATEST_HITS"
            else -> null
        }
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
