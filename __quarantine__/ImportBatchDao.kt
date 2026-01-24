package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.v1.DbSchema
import com.zak.pressmark.data.local.entity.v1.ImportBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportBatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(batch: ImportBatchEntity)

    @Query("SELECT * FROM ${DbSchema.ImportBatch.TABLE} WHERE ${DbSchema.ImportBatch.ID} = :id")
    suspend fun getById(id: String): ImportBatchEntity?

    @Query("SELECT * FROM ${DbSchema.ImportBatch.TABLE} ORDER BY ${DbSchema.ImportBatch.CREATED_AT} DESC")
    fun observeAll(): Flow<List<ImportBatchEntity>>
}
