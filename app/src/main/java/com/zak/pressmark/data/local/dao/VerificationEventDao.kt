package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface VerificationEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: com.zak.pressmark.data.local.entity.VerificationEventEntity)

    @androidx.room.Query(
        "DELETE FROM verification_events WHERE catalog_item_id = :catalogItemId"
    )
    suspend fun deleteByCatalogItemId(catalogItemId: String)
}
