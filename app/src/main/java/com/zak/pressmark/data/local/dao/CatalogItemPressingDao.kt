package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.DbSchema.CatalogItemPressing
import com.zak.pressmark.data.local.db.DbSchema.Release
import com.zak.pressmark.data.model.CatalogPressingSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogItemPressingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pressing: com.zak.pressmark.data.local.entity.CatalogItemPressingEntity)

    @Query("DELETE FROM ${CatalogItemPressing.TABLE} WHERE ${CatalogItemPressing.CATALOG_ITEM_ID} = :catalogItemId")
    suspend fun deleteByCatalogItemId(catalogItemId: String)

    @Query(
        """
        SELECT ${CatalogItemPressing.RELEASE_ID}
        FROM ${CatalogItemPressing.TABLE}
        WHERE ${CatalogItemPressing.CATALOG_ITEM_ID} = :catalogItemId
        """
    )
    suspend fun listReleaseIds(catalogItemId: String): List<String>

    @Query(
        """
        SELECT * FROM ${CatalogItemPressing.TABLE}
        WHERE ${CatalogItemPressing.RELEASE_ID} = :releaseId
        LIMIT 1
        """
    )
    suspend fun findByReleaseId(releaseId: String): com.zak.pressmark.data.local.entity.CatalogItemPressingEntity?

    @Query(
        """
        SELECT
            p.${CatalogItemPressing.ID} AS pressingId,
            p.${CatalogItemPressing.CATALOG_ITEM_ID} AS catalogItemId,
            p.${CatalogItemPressing.RELEASE_ID} AS releaseId,
            p.${CatalogItemPressing.EVIDENCE_SCORE} AS evidenceScore,
            r.${Release.TITLE} AS title,
            r.${Release.LABEL} AS label,
            r.${Release.CATALOG_NO} AS catalogNo,
            r.${Release.COUNTRY} AS country,
            r.${Release.RELEASE_YEAR} AS releaseYear
        FROM ${CatalogItemPressing.TABLE} p
        LEFT JOIN ${Release.TABLE} r
          ON r.${Release.ID} = p.${CatalogItemPressing.RELEASE_ID}
        WHERE p.${CatalogItemPressing.CATALOG_ITEM_ID} = :catalogItemId
        ORDER BY p.${CatalogItemPressing.CREATED_AT} DESC
        """
    )
    fun observePressingSummaries(catalogItemId: String): Flow<List<CatalogPressingSummary>>

    @Query(
        """
        SELECT ${CatalogItemPressing.CATALOG_ITEM_ID}
        FROM ${CatalogItemPressing.TABLE}
        WHERE ${CatalogItemPressing.RELEASE_ID} = :releaseId
        LIMIT 1
        """
    )
    suspend fun findCatalogItemIdByReleaseId(releaseId: String): String?
}
