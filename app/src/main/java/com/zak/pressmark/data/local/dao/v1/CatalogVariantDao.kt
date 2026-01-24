package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.DbSchema.CatalogVariant
import com.zak.pressmark.data.local.entity.v1.CatalogVariantEntity
import com.zak.pressmark.data.model.CatalogVariantSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogVariantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(variants: List<CatalogVariantEntity>)

    @Query("DELETE FROM ${CatalogVariant.TABLE} WHERE ${CatalogVariant.CATALOG_ITEM_ID} = :catalogItemId")
    suspend fun deleteByCatalogItemId(catalogItemId: String)

    @Query(
        """
        SELECT
            ${CatalogVariant.ID} AS variantId,
            ${CatalogVariant.PRESSING_ID} AS pressingId,
            ${CatalogVariant.VARIANT_KEY} AS variantKey,
            ${CatalogVariant.NOTES} AS notes
        FROM ${CatalogVariant.TABLE}
        WHERE ${CatalogVariant.CATALOG_ITEM_ID} = :catalogItemId
        ORDER BY ${CatalogVariant.CREATED_AT} DESC
        """
    )
    fun observeVariantsForCatalogItem(catalogItemId: String): Flow<List<CatalogVariantSummary>>
}
