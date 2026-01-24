package com.zak.pressmark.data.repository.catalog

import com.zak.pressmark.data.local.dao.CatalogItemDao
import com.zak.pressmark.data.local.dao.MasterIdentityDao
import com.zak.pressmark.data.local.entity.CatalogItemEntity
import com.zak.pressmark.data.local.entity.CatalogItemState
import com.zak.pressmark.data.local.entity.MasterIdentityEntity
import com.zak.pressmark.data.local.entity.Provider
import com.zak.pressmark.data.local.query.CatalogItemQueryBuilder
import com.zak.pressmark.data.util.Normalization
import kotlinx.coroutines.flow.Flow

/**
 * Phase 1 repository for master-first records.
 *
 * This does NOT replace your existing release-first repository yet.
 * Phase 0 cutover is handled by a higher-level compatibility layer (next batch).
 */
class CatalogItemRepository(
    private val catalogItemDao: CatalogItemDao,
    private val masterIdentityDao: MasterIdentityDao
) {

    /**
     * Create (or reuse) a MasterIdentity and a CatalogItem that points to it.
     * Intended for ingest: barcode/cover/quick add -> master-first record.
     */
    suspend fun createCatalogItemMasterFirst(
        provider: Provider,
        providerMasterId: String,
        title: String,
        artistLine: String,
        year: Int? = null,
        genresJson: String? = null,
        stylesJson: String? = null,
        artworkUri: String? = null,
        rawJson: String? = null
    ): Long {
        val now = System.currentTimeMillis()

        val existingMaster = masterIdentityDao.getByProviderId(provider, providerMasterId)
        val masterId = if (existingMaster != null) {
            existingMaster.id
        } else {
            val toInsert = MasterIdentityEntity(
                provider = provider,
                providerMasterId = providerMasterId,
                title = title,
                artistLine = artistLine,
                year = year,
                genresJson = genresJson,
                stylesJson = stylesJson,
                artworkUri = artworkUri,
                rawJson = rawJson,
                titleSort = Normalization.sortKey(title),
                artistSort = Normalization.sortKey(artistLine),
                createdAt = now,
                updatedAt = now
            )
            val insertedId = masterIdentityDao.insertIgnore(toInsert)
            if (insertedId > 0) insertedId else masterIdentityDao.getByProviderId(provider, providerMasterId)?.id
                ?: error("Failed to create/retrieve MasterIdentity for $provider:$providerMasterId")
        }

        val item = CatalogItemEntity(
            masterIdentityId = masterId,
            displayTitle = title,
            displayArtistLine = artistLine,
            primaryArtworkUri = artworkUri,
            state = CatalogItemState.MASTER_ONLY,
            displayTitleSort = Normalization.sortKey(title),
            displayArtistSort = Normalization.sortKey(artistLine),
            createdAt = now,
            updatedAt = now
        )

        return catalogItemDao.upsert(item)
    }

    fun observeCatalogList(query: CatalogItemQueryBuilder): Flow<List<CatalogItemEntity>> {
        // Convenience overload if you want to pass a builder instance (rare).
        return catalogItemDao.observeList(CatalogItemQueryBuilder.build())
    }

    fun observeCatalogList(
        titlePrefix: String? = null,
        artistPrefix: String? = null,
        state: CatalogItemState? = null,
        sort: CatalogItemQueryBuilder.Sort = CatalogItemQueryBuilder.Sort.ADDED_DESC,
        limit: Int = 200,
        offset: Int = 0
    ): Flow<List<CatalogItemEntity>> {
        val q = CatalogItemQueryBuilder.build(
            titlePrefix = titlePrefix,
            artistPrefix = artistPrefix,
            state = state,
            sort = sort,
            limit = limit,
            offset = offset
        )
        return catalogItemDao.observeList(q)
    }

    fun observeCatalogItem(id: Long): Flow<CatalogItemEntity?> = catalogItemDao.observeById(id)
    fun observeMasterIdentity(id: Long): Flow<MasterIdentityEntity?> = masterIdentityDao.observeById(id)
}
