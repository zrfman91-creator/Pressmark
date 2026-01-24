package com.zak.pressmark.data.repository

import androidx.room.withTransaction
import com.zak.pressmark.core.credits.ArtistCreditFormatter
import com.zak.pressmark.data.local.dao.ArtworkDao
import com.zak.pressmark.data.local.dao.CatalogItemDao
import com.zak.pressmark.data.local.dao.CatalogItemPressingDao
import com.zak.pressmark.data.local.dao.CatalogVariantDao
import com.zak.pressmark.data.local.dao.EvidenceArtifactDao
import com.zak.pressmark.data.local.dao.MasterIdentityDao
import com.zak.pressmark.data.local.dao.ReleaseArtistCreditDao
import com.zak.pressmark.data.local.dao.ReleaseDao
import com.zak.pressmark.data.local.dao.VerificationEventDao
import com.zak.pressmark.data.local.db.AppDatabase
import com.zak.pressmark.data.local.entity.CatalogItemEntity
import com.zak.pressmark.data.local.entity.CatalogItemPressingEntity
import com.zak.pressmark.data.local.entity.MasterIdentityEntity
import com.zak.pressmark.data.local.model.ArtistCreditFormatMapper
import com.zak.pressmark.data.model.CatalogItemDetails
import com.zak.pressmark.data.model.CatalogItemSummary
import com.zak.pressmark.data.model.CatalogPressingDetails
import com.zak.pressmark.data.model.MasterIdentitySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.UUID

class CatalogRepository(
    private val db: AppDatabase,
    private val catalogItemDao: CatalogItemDao = db.catalogItemDao(),
    private val masterIdentityDao: MasterIdentityDao = db.masterIdentityDao(),
    private val catalogItemPressingDao: CatalogItemPressingDao = db.catalogItemPressingDao(),
    private val catalogVariantDao: CatalogVariantDao = db.catalogVariantDao(),
    private val evidenceArtifactDao: EvidenceArtifactDao = db.evidenceArtifactDao(),
    private val verificationEventDao: VerificationEventDao = db.verificationEventDao(),
    private val releaseDao: ReleaseDao = db.releaseDao(),
    private val creditDao: ReleaseArtistCreditDao = db.releaseArtistCreditDao(),
    private val artworkDao: ArtworkDao = db.artworkDao(),
) {
    fun observeCatalogItemSummaries(
        query: Flow<String>,
        sort: Flow<com.zak.pressmark.feature.catalog.vm.CatalogSort>,
    ): Flow<List<CatalogItemSummary>> {
        return combine(query, sort) { rawQuery, sortValue ->
            rawQuery.trim().lowercase() to sortValue
        }.flatMapLatest { (normalizedQuery, sortValue) ->
            when (sortValue) {
                com.zak.pressmark.feature.catalog.vm.CatalogSort.TitleAZ ->
                    catalogItemDao.observeSummariesByTitle(normalizedQuery)
                com.zak.pressmark.feature.catalog.vm.CatalogSort.ArtistAZ ->
                    catalogItemDao.observeSummariesByArtist(normalizedQuery)
                com.zak.pressmark.feature.catalog.vm.CatalogSort.YearNewest ->
                    catalogItemDao.observeSummariesByYear(normalizedQuery)
                com.zak.pressmark.feature.catalog.vm.CatalogSort.AddedNewest ->
                    catalogItemDao.observeSummariesByAdded(normalizedQuery)
            }
        }
    }

    fun observeCatalogItemDetails(catalogItemId: String): Flow<CatalogItemDetails?> {
        val itemFlow = catalogItemDao.observeById(catalogItemId)
        val masterFlow = itemFlow.flatMapLatest { item ->
            item?.let { masterIdentityDao.observeById(it.masterIdentityId) }
                ?: kotlinx.coroutines.flow.flowOf(null)
        }
        val pressingFlow = catalogItemPressingDao.observePressingSummaries(catalogItemId)
        val variantFlow = catalogVariantDao.observeVariantsForCatalogItem(catalogItemId)

        return combine(itemFlow, masterFlow, pressingFlow, variantFlow) { item, master, pressings, variants ->
            item?.let {
                val masterSummary = master?.let {
                    MasterIdentitySummary(
                        provider = it.provider,
                        masterId = it.masterId,
                        title = it.title,
                        artistLine = it.artistLine,
                        year = it.year,
                        genres = it.genres,
                        styles = it.styles,
                        artworkUri = it.artworkUri,
                    )
                }
                val pressingsWithVariants = pressings.map { pressing ->
                    val linkedVariants = variants.filter { it.pressingId == pressing.pressingId }
                    CatalogPressingDetails(
                        summary = pressing,
                        variants = linkedVariants,
                    )
                }
                CatalogItemDetails(
                    catalogItemId = it.id,
                    displayTitle = it.displayTitle,
                    displayArtistLine = it.displayArtistLine,
                    primaryArtworkUri = it.primaryArtworkUri,
                    releaseYear = it.releaseYear,
                    state = it.state,
                    master = masterSummary,
                    pressings = pressingsWithVariants,
                )
            }
        }
    }

    suspend fun upsertFromRelease(
        releaseId: String,
        artistLineOverride: String? = null,
    ): String? {
        val release = releaseDao.getById(releaseId) ?: return null
        val artistLine = artistLineOverride
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: buildArtistLine(releaseId)
        val artworkUri = artworkDao.primaryOrLatestForRelease(releaseId)?.uri
        val provider = release.artworkProvider?.trim()?.takeIf { it.isNotBlank() }
            ?: if (release.masterId != null || release.discogsReleaseId != null) "discogs" else "local"
        val masterKey = release.masterId?.toString() ?: release.discogsReleaseId?.toString()
        val masterIdentityId = if (masterKey != null) "${provider.lowercase()}:$masterKey" else "local:$releaseId"
        val now = System.currentTimeMillis()

        return db.withTransaction {
            val masterIdentity = MasterIdentityEntity(
                id = masterIdentityId,
                provider = provider,
                masterId = release.masterId?.toString(),
                title = release.title,
                artistLine = artistLine,
                year = release.releaseYear,
                genres = release.genre,
                styles = null,
                artworkUri = artworkUri,
                rawJson = null,
                createdAt = now,
            )
            masterIdentityDao.upsert(masterIdentity)

            val existingCatalogItem = catalogItemDao.getByMasterIdentityId(masterIdentityId)
            val catalogItemId = existingCatalogItem?.id ?: UUID.randomUUID().toString()
            val addedAt = existingCatalogItem?.addedAt ?: release.addedAt
            val catalogItem = CatalogItemEntity(
                id = catalogItemId,
                masterIdentityId = masterIdentityId,
                displayTitle = release.title,
                displayArtistLine = artistLine,
                primaryArtworkUri = artworkUri,
                releaseYear = release.releaseYear,
                state = "RELEASE_CONFIRMED",
                addedAt = addedAt,
                updatedAt = now,
            )
            catalogItemDao.upsert(catalogItem)

            val existingPressing = catalogItemPressingDao.findByReleaseId(releaseId)
            if (existingPressing == null) {
                val pressing = CatalogItemPressingEntity(
                    id = UUID.randomUUID().toString(),
                    catalogItemId = catalogItemId,
                    releaseId = releaseId,
                    evidenceScore = null,
                    createdAt = now,
                )
                catalogItemPressingDao.upsert(pressing)
            }

            catalogItemId
        }
    }

    suspend fun deleteCatalogItem(catalogItemId: String) {
        db.withTransaction {
            val releaseIds = catalogItemPressingDao.listReleaseIds(catalogItemId)
                .filterNotNull()
            catalogItemPressingDao.deleteByCatalogItemId(catalogItemId)
            catalogVariantDao.deleteByCatalogItemId(catalogItemId)
            evidenceArtifactDao.deleteByCatalogItemId(catalogItemId)
            verificationEventDao.deleteByCatalogItemId(catalogItemId)
            catalogItemDao.deleteById(catalogItemId)
            releaseIds.forEach { releaseDao.deleteById(it) }
        }
    }

    private suspend fun buildArtistLine(releaseId: String): String {
        val creditRows = creditDao.creditRowsForRelease(releaseId)
        if (creditRows.isEmpty()) return "Unknown artist"
        val formatterCredits = creditRows.map { row ->
            val creditEntity = com.zak.pressmark.data.local.entity.ReleaseArtistCreditEntity(
                releaseId = releaseId,
                artistId = row.artistId,
                role = row.role,
                position = row.position,
                displayHint = row.displayHint,
            )
            ArtistCreditFormatMapper.toFormatterCredit(creditEntity, row.artistDisplayName)
        }
        return ArtistCreditFormatter.formatSingleLine(formatterCredits)
    }
}
