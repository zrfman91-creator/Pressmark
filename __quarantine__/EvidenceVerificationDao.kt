package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.entity.v1.EvidenceArtifactEntity
import com.zak.pressmark.data.local.entity.v1.VerificationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceVerificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(entity: EvidenceArtifactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerificationEvent(entity: VerificationEventEntity): Long

    @Query("SELECT * FROM evidence_artifacts WHERE catalog_item_id = :catalogItemId ORDER BY created_at DESC")
    fun observeEvidenceForItem(catalogItemId: Long): Flow<List<EvidenceArtifactEntity>>

    @Query("SELECT * FROM verification_events WHERE catalog_item_id = :catalogItemId ORDER BY created_at DESC")
    fun observeEventsForItem(catalogItemId: Long): Flow<List<VerificationEventEntity>>
}
