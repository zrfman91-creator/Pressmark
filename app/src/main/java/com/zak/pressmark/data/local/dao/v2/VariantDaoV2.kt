// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/v2/VariantDaoV2.kt
package com.zak.pressmark.data.local.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.v2.DbSchemaV2
import com.zak.pressmark.data.local.entity.v2.VariantEntityV2
import kotlinx.coroutines.flow.Flow

data class SelectedPressingDetailsRow(
    val discogsReleaseId: Long?,
    val label: String?,
    val catalogNo: String?,
    val country: String?,
    val year: Int?,
    val format: String?,
)

@Dao
interface VariantDaoV2 {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(variant: VariantEntityV2)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(variants: List<VariantEntityV2>)

    @Query("DELETE FROM ${DbSchemaV2.Variant.TABLE} WHERE ${DbSchemaV2.Variant.WORK_ID} = :workId")
    suspend fun deleteByWorkId(workId: String)

    @Query("SELECT * FROM ${DbSchemaV2.Variant.TABLE} WHERE ${DbSchemaV2.Variant.WORK_ID} = :workId ORDER BY ${DbSchemaV2.Variant.ADDED_AT} DESC")
    fun observeByWorkId(workId: String): Flow<List<VariantEntityV2>>

    @Query("SELECT * FROM ${DbSchemaV2.Variant.TABLE} WHERE ${DbSchemaV2.Variant.PRESSING_ID} = :pressingId ORDER BY ${DbSchemaV2.Variant.ADDED_AT} DESC")
    fun observeByPressingId(pressingId: String): Flow<List<VariantEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Variant.TABLE}
        WHERE ${DbSchemaV2.Variant.WORK_ID} = :workId
          AND ${DbSchemaV2.Variant.PRESSING_ID} = :pressingId
          AND ${DbSchemaV2.Variant.VARIANT_KEY} = :variantKey
        LIMIT 1
        """
    )
    suspend fun getByKey(workId: String, pressingId: String, variantKey: String): VariantEntityV2?

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Variant.TABLE}
        WHERE ${DbSchemaV2.Variant.WORK_ID} = :workId
          AND ${DbSchemaV2.Variant.VARIANT_KEY} = :variantKey
        LIMIT 1
        """
    )
    suspend fun getByWorkAndKey(workId: String, variantKey: String): VariantEntityV2?

    /**
     * The "chosen refinement" = the default variant for this work.
     * Joins to Pressing + Release to provide a stable display surface on Album Details.
     *
     * This is a single-row lookup (LIMIT 1), used only on the details screen.
     */
    @Query(
        """
        SELECT
          p.${DbSchemaV2.Pressing.DISCOGS_RELEASE_ID} AS discogsReleaseId,
          COALESCE(p.${DbSchemaV2.Pressing.LABEL}, r.${DbSchemaV2.Release.LABEL}) AS label,
          COALESCE(p.${DbSchemaV2.Pressing.CATALOG_NO}, r.${DbSchemaV2.Release.CATALOG_NO}) AS catalogNo,
          COALESCE(p.${DbSchemaV2.Pressing.COUNTRY}, r.${DbSchemaV2.Release.COUNTRY}) AS country,
          COALESCE(p.${DbSchemaV2.Pressing.RELEASE_YEAR}, r.${DbSchemaV2.Release.RELEASE_YEAR}) AS year,
          COALESCE(p.${DbSchemaV2.Pressing.FORMAT}, r.${DbSchemaV2.Release.FORMAT}) AS format
        FROM ${DbSchemaV2.Variant.TABLE} v
        INNER JOIN ${DbSchemaV2.Pressing.TABLE} p
          ON p.${DbSchemaV2.Pressing.ID} = v.${DbSchemaV2.Variant.PRESSING_ID}
        INNER JOIN ${DbSchemaV2.Release.TABLE} r
          ON r.${DbSchemaV2.Release.ID} = p.${DbSchemaV2.Pressing.RELEASE_ID}
        WHERE v.${DbSchemaV2.Variant.WORK_ID} = :workId
          AND v.${DbSchemaV2.Variant.VARIANT_KEY} = 'default'
        LIMIT 1
        """
    )
    fun observeSelectedPressingDetails(workId: String): Flow<SelectedPressingDetailsRow?>
}
