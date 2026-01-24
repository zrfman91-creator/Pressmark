// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/v1/CatalogItemPressingDao.kt
package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.model.CatalogPressingSummary
import kotlinx.coroutines.flow.Flow

/**
 * Fix: legacy queries referenced `release_id`, but the V1 catalog linkage table is now pressing-based.
 *
 * - If your CatalogItemPressingEntity has a column named `pressing_id`, all queries should reference it.
 * - We keep the original method names/signatures to avoid cascading call-site changes during the refactor.
 *
 * NOTE:
 * observePressingSummaries previously joined the legacy `releases` table via release_id. With a pressing-based link,
 * that join is no longer valid unless you have a separate table keyed by pressing_id. To keep compilation unblocked,
 * this version returns nulls for the release metadata columns (title/label/catalogNo/country/releaseYear).
 * We will replace this with a proper V2 join once V2 pressings are the canonical source.
 */
@Dao
interface CatalogItemPressingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pressing: com.zak.pressmark.data.local.entity.v1.CatalogItemPressingEntity)

    @Query("DELETE FROM catalog_item_pressings WHERE catalog_item_id = :catalogItemId")
    suspend fun deleteByCatalogItemId(catalogItemId: String)

    @Query(
        """
        SELECT pressing_id
        FROM catalog_item_pressings
        WHERE catalog_item_id = :catalogItemId
        """
    )
    suspend fun listReleaseIds(catalogItemId: String): List<String?>

    @Query(
        """
        SELECT *
        FROM catalog_item_pressings
        WHERE pressing_id = :releaseId
        LIMIT 1
        """
    )
    suspend fun findByReleaseId(releaseId: String): com.zak.pressmark.data.local.entity.v1.CatalogItemPressingEntity?

    @Query(
        """
        SELECT
            p.id              AS pressingId,
            p.catalog_item_id AS catalogItemId,
            p.pressing_id     AS releaseId,
            p.evidence_score  AS evidenceScore,
            NULL              AS title,
            NULL              AS label,
            NULL              AS catalogNo,
            NULL              AS country,
            NULL              AS releaseYear
        FROM catalog_item_pressings p
        WHERE p.catalog_item_id = :catalogItemId
        ORDER BY p.created_at DESC
        """
    )
    fun observePressingSummaries(catalogItemId: String): Flow<List<CatalogPressingSummary>>

    @Query(
        """
        SELECT catalog_item_id
        FROM catalog_item_pressings
        WHERE pressing_id = :releaseId
        LIMIT 1
        """
    )
    suspend fun findCatalogItemIdByReleaseId(releaseId: String): String?
}
