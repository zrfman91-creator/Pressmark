package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.entity.v1.VerificationEventEntity

@Dao
interface VerificationEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: VerificationEventEntity)

    @Query(
        "DELETE FROM verification_events WHERE catalog_item_id = :catalogItemId"
    )
    suspend fun deleteByCatalogItemId(catalogItemId: String)
}
