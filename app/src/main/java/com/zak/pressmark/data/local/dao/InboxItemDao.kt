package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zak.pressmark.data.local.db.DbSchema
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.model.inbox.OcrStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InboxItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<InboxItemEntity>)

    @Update
    suspend fun update(item: InboxItemEntity)

    @Query("SELECT * FROM ${DbSchema.InboxItem.TABLE} WHERE ${DbSchema.InboxItem.ID} = :id")
    suspend fun getById(id: String): InboxItemEntity?

    @Query("SELECT * FROM ${DbSchema.InboxItem.TABLE} WHERE ${DbSchema.InboxItem.ID} = :id")
    fun observeById(id: String): Flow<InboxItemEntity?>

    @Query("DELETE FROM ${DbSchema.InboxItem.TABLE} WHERE ${DbSchema.InboxItem.ID} = :id")
    suspend fun deleteById(id: String)

    @Query(
        "SELECT COUNT(*) FROM ${DbSchema.InboxItem.TABLE} " +
            "WHERE ${DbSchema.InboxItem.DELETED_AT} IS NULL"
    )
    fun observeInboxCount(): Flow<Int>

    @Query(
        "SELECT * FROM ${DbSchema.InboxItem.TABLE} " +
            "WHERE ${DbSchema.InboxItem.DELETED_AT} IS NULL " +
            "ORDER BY ${DbSchema.InboxItem.CREATED_AT} DESC"
    )
    fun observeAll(): Flow<List<InboxItemEntity>>

    @Query(
        "SELECT ${DbSchema.InboxItem.ID} FROM ${DbSchema.InboxItem.TABLE} " +
            "WHERE ${DbSchema.InboxItem.DELETED_AT} IS NULL " +
            "ORDER BY ${DbSchema.InboxItem.CREATED_AT} DESC"
    )
    suspend fun fetchOrderedIds(): List<String>

    @Query(
        """
        SELECT * FROM ${DbSchema.InboxItem.TABLE}
        WHERE ${DbSchema.InboxItem.OCR_STATUS} = :status
          AND ${DbSchema.InboxItem.NEXT_OCR_AT} <= :now
          AND ${DbSchema.InboxItem.PHOTO_URIS_JSON} IS NOT NULL
          AND ${DbSchema.InboxItem.DELETED_AT} IS NULL
        ORDER BY ${DbSchema.InboxItem.CREATED_AT} ASC
        LIMIT :limit
        """
    )
    suspend fun fetchOcrEligible(
        status: OcrStatus,
        now: Long,
        limit: Int,
    ): List<InboxItemEntity>

    @Query(
        """
        SELECT * FROM ${DbSchema.InboxItem.TABLE}
        WHERE ${DbSchema.InboxItem.LOOKUP_STATUS} = :status
          AND ${DbSchema.InboxItem.NEXT_LOOKUP_AT} <= :now
          AND ${DbSchema.InboxItem.DELETED_AT} IS NULL
        ORDER BY ${DbSchema.InboxItem.CREATED_AT} ASC
        LIMIT :limit
        """
    )
    suspend fun fetchLookupEligible(
        status: LookupStatus,
        now: Long,
        limit: Int,
    ): List<InboxItemEntity>
}
