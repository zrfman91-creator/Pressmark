package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface EvidenceArtifactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<com.zak.pressmark.data.local.entity.EvidenceArtifactEntity>)

    @androidx.room.Query(
        "DELETE FROM evidence_artifacts WHERE catalog_item_id = :catalogItemId"
    )
    suspend fun deleteByCatalogItemId(catalogItemId: String)
}
