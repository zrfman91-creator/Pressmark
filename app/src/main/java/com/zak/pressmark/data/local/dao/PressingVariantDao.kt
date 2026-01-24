package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.entity.CatalogItemPressingEntity
import com.zak.pressmark.data.local.entity.CatalogVariantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PressingVariantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPressing(entity: CatalogItemPressingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVariant(entity: CatalogVariantEntity): Long

    @Query("SELECT * FROM catalog_item_pressings WHERE catalog_item_id = :catalogItemId ORDER BY updated_at DESC")
    fun observePressingsForItem(catalogItemId: Long): Flow<List<CatalogItemPressingEntity>>

    @Query("SELECT * FROM catalog_variants WHERE pressing_id = :pressingId ORDER BY updated_at DESC")
    fun observeVariantsForPressing(pressingId: Long): Flow<List<CatalogVariantEntity>>
}
