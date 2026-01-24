package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.entity.v1.EvidenceArtifactEntity

@Dao
interface EvidenceArtifactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<EvidenceArtifactEntity>)

    @Query(
        "DELETE FROM evidence_artifacts WHERE catalog_item_id = :catalogItemId"
    )
    suspend fun deleteByCatalogItemId(catalogItemId: String)
}
