package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.DbSchema.MasterIdentity
import com.zak.pressmark.data.local.entity.MasterIdentityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterIdentityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MasterIdentityEntity)

    @Query("SELECT * FROM ${MasterIdentity.TABLE} WHERE ${MasterIdentity.ID} = :id LIMIT 1")
    fun observeById(id: String): Flow<MasterIdentityEntity?>
}
