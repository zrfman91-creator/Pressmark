package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.DbSchema.CatalogItemPressing
import com.zak.pressmark.data.local.entity.v1.CatalogItemPressingEntity
import com.zak.pressmark.data.model.CatalogPressingSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogItemPressingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pressing: CatalogItemPressingEntity)

    @Query("DELETE FROM ${CatalogItemPressing.TABLE} WHERE ${CatalogItemPressing.CATALOG_ITEM_ID} = :catalogItemId")
    suspend fun deleteByCatalogItemId(catalogItemId: String)

    @Query(
        """
        SELECT ${CatalogItemPressing.RELEASE_ID}
        FROM ${CatalogItemPressing.TABLE}
        WHERE ${CatalogItemPressing.CATALOG_ITEM_ID} = :catalogItemId
        """
    )
    suspend fun listReleaseIds(catalogItemId: String): List<String?>

    @Query(
        """
        SELECT * FROM ${CatalogItemPressing.TABLE}
        WHERE ${CatalogItemPressing.RELEASE_ID} = :releaseId
        LIMIT 1
        """
    )
    suspend fun findByReleaseId(releaseId: String): CatalogItemPressingEntity?

    @Query(
        """
        SELECT
            p.${CatalogItemPressing.ID} AS pressingId,
            p.${CatalogItemPressing.CATALOG_ITEM_ID} AS catalogItemId,
            p.${CatalogItemPressing.RELEASE_ID} AS releaseId,
            p.${CatalogItemPressing.EVIDENCE_SCORE} AS evidenceScore,
            r.title AS title,
            r.label AS label,
            r.catalog_no AS catalogNo,
            r.country AS country,
            r.release_year AS releaseYear
        FROM ${CatalogItemPressing.TABLE} p
        LEFT JOIN releases r
          ON r.id = p.${CatalogItemPressing.RELEASE_ID}
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
