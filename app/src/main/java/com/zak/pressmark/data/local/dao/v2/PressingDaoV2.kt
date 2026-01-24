// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/v2/PressingDaoV2.kt
package com.zak.pressmark.data.local.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.v2.DbSchemaV2
import com.zak.pressmark.data.local.entity.v2.PressingEntityV2
import kotlinx.coroutines.flow.Flow

@Dao
interface PressingDaoV2 {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pressing: PressingEntityV2)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(pressings: List<PressingEntityV2>)

    @Query("DELETE FROM ${DbSchemaV2.Pressing.TABLE} WHERE ${DbSchemaV2.Pressing.RELEASE_ID} = :releaseId")
    suspend fun deleteByReleaseId(releaseId: String)

    @Query("SELECT * FROM ${DbSchemaV2.Pressing.TABLE} WHERE ${DbSchemaV2.Pressing.ID} = :pressingId LIMIT 1")
    suspend fun getById(pressingId: String): PressingEntityV2?

    @Query("SELECT * FROM ${DbSchemaV2.Pressing.TABLE} WHERE ${DbSchemaV2.Pressing.RELEASE_ID} = :releaseId")
    fun observeByReleaseId(releaseId: String): Flow<List<PressingEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Pressing.TABLE}
        WHERE ${DbSchemaV2.Pressing.BARCODE_NORMALIZED} = :barcodeNormalized
        ORDER BY ${DbSchemaV2.Pressing.UPDATED_AT} DESC
        """
    )
    suspend fun findByBarcodeNormalized(barcodeNormalized: String): List<PressingEntityV2>
}
