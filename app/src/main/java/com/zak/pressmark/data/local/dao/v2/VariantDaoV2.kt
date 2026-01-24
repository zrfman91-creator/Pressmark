// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/v2/VariantDaoV2.kt
package com.zak.pressmark.data.local.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.DbSchemaV2
import com.zak.pressmark.data.local.entity.v2.VariantEntityV2
import kotlinx.coroutines.flow.Flow

@Dao
interface VariantDaoV2 {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(variant: VariantEntityV2)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(variants: List<VariantEntityV2>)

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
}
