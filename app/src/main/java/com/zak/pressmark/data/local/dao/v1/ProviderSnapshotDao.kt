package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.DbSchema
import com.zak.pressmark.data.local.entity.v1.ProviderSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProviderSnapshotEntity>)

    @Query("SELECT * FROM ${DbSchema.ProviderSnapshot.TABLE} WHERE ${DbSchema.ProviderSnapshot.INBOX_ITEM_ID} = :inboxItemId ORDER BY ${DbSchema.ProviderSnapshot.CONFIDENCE} DESC")
    fun observeForInboxItem(inboxItemId: String): Flow<List<ProviderSnapshotEntity>>

    @Query("DELETE FROM ${DbSchema.ProviderSnapshot.TABLE} WHERE ${DbSchema.ProviderSnapshot.INBOX_ITEM_ID} = :inboxItemId")
    suspend fun deleteForInboxItem(inboxItemId: String)
}
