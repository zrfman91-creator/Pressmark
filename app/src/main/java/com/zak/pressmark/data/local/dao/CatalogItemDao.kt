package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.DbSchema.CatalogItem
import com.zak.pressmark.data.model.CatalogItemSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: com.zak.pressmark.data.local.entity.CatalogItemEntity)

    @Query("DELETE FROM ${CatalogItem.TABLE} WHERE ${CatalogItem.ID} = :catalogItemId")
    suspend fun deleteById(catalogItemId: String)

    @Query("SELECT * FROM ${CatalogItem.TABLE} WHERE ${CatalogItem.ID} = :catalogItemId LIMIT 1")
    fun observeById(catalogItemId: String): Flow<com.zak.pressmark.data.local.entity.CatalogItemEntity?>

    @Query(
        """
        SELECT * FROM ${CatalogItem.TABLE}
        WHERE ${CatalogItem.MASTER_IDENTITY_ID} = :masterIdentityId
        LIMIT 1
        """
    )
    suspend fun getByMasterIdentityId(masterIdentityId: String): com.zak.pressmark.data.local.entity.CatalogItemEntity?

    @Query(
        """
        SELECT
            ${CatalogItem.ID} AS catalogItemId,
            ${CatalogItem.DISPLAY_TITLE} AS displayTitle,
            ${CatalogItem.DISPLAY_ARTIST_LINE} AS displayArtistLine,
            ${CatalogItem.PRIMARY_ARTWORK_URI} AS primaryArtworkUri,
            ${CatalogItem.RELEASE_YEAR} AS releaseYear,
            ${CatalogItem.ADDED_AT} AS addedAt,
            ${CatalogItem.STATE} AS state
        FROM ${CatalogItem.TABLE}
        WHERE (:query = '' OR lower(${CatalogItem.DISPLAY_TITLE}) LIKE '%' || :query || '%'
            OR lower(${CatalogItem.DISPLAY_ARTIST_LINE}) LIKE '%' || :query || '%')
        ORDER BY ${CatalogItem.DISPLAY_TITLE} COLLATE NOCASE ASC
        """
    )
    fun observeSummariesByTitle(query: String): Flow<List<CatalogItemSummary>>

    @Query(
        """
        SELECT
            ${CatalogItem.ID} AS catalogItemId,
            ${CatalogItem.DISPLAY_TITLE} AS displayTitle,
            ${CatalogItem.DISPLAY_ARTIST_LINE} AS displayArtistLine,
            ${CatalogItem.PRIMARY_ARTWORK_URI} AS primaryArtworkUri,
            ${CatalogItem.RELEASE_YEAR} AS releaseYear,
            ${CatalogItem.ADDED_AT} AS addedAt,
            ${CatalogItem.STATE} AS state
        FROM ${CatalogItem.TABLE}
        WHERE (:query = '' OR lower(${CatalogItem.DISPLAY_TITLE}) LIKE '%' || :query || '%'
            OR lower(${CatalogItem.DISPLAY_ARTIST_LINE}) LIKE '%' || :query || '%')
        ORDER BY ${CatalogItem.DISPLAY_ARTIST_LINE} COLLATE NOCASE ASC,
                 ${CatalogItem.DISPLAY_TITLE} COLLATE NOCASE ASC
        """
    )
    fun observeSummariesByArtist(query: String): Flow<List<CatalogItemSummary>>

    @Query(
        """
        SELECT
            ${CatalogItem.ID} AS catalogItemId,
            ${CatalogItem.DISPLAY_TITLE} AS displayTitle,
            ${CatalogItem.DISPLAY_ARTIST_LINE} AS displayArtistLine,
            ${CatalogItem.PRIMARY_ARTWORK_URI} AS primaryArtworkUri,
            ${CatalogItem.RELEASE_YEAR} AS releaseYear,
            ${CatalogItem.ADDED_AT} AS addedAt,
            ${CatalogItem.STATE} AS state
        FROM ${CatalogItem.TABLE}
        WHERE (:query = '' OR lower(${CatalogItem.DISPLAY_TITLE}) LIKE '%' || :query || '%'
            OR lower(${CatalogItem.DISPLAY_ARTIST_LINE}) LIKE '%' || :query || '%')
        ORDER BY ${CatalogItem.RELEASE_YEAR} DESC,
                 ${CatalogItem.DISPLAY_TITLE} COLLATE NOCASE ASC
        """
    )
    fun observeSummariesByYear(query: String): Flow<List<CatalogItemSummary>>

    @Query(
        """
        SELECT
            ${CatalogItem.ID} AS catalogItemId,
            ${CatalogItem.DISPLAY_TITLE} AS displayTitle,
            ${CatalogItem.DISPLAY_ARTIST_LINE} AS displayArtistLine,
            ${CatalogItem.PRIMARY_ARTWORK_URI} AS primaryArtworkUri,
            ${CatalogItem.RELEASE_YEAR} AS releaseYear,
            ${CatalogItem.ADDED_AT} AS addedAt,
            ${CatalogItem.STATE} AS state
        FROM ${CatalogItem.TABLE}
        WHERE (:query = '' OR lower(${CatalogItem.DISPLAY_TITLE}) LIKE '%' || :query || '%'
            OR lower(${CatalogItem.DISPLAY_ARTIST_LINE}) LIKE '%' || :query || '%')
        ORDER BY ${CatalogItem.ADDED_AT} DESC
        """
    )
    fun observeSummariesByAdded(query: String): Flow<List<CatalogItemSummary>>
}
