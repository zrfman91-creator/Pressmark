package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.entity.MasterIdentityEntity
import com.zak.pressmark.data.local.entity.Provider
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterIdentityDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: MasterIdentityEntity): Long

    @Query("SELECT * FROM master_identities WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MasterIdentityEntity?

    @Query("SELECT * FROM master_identities WHERE provider = :provider AND provider_master_id = :providerMasterId LIMIT 1")
    suspend fun getByProviderId(provider: Provider, providerMasterId: String): MasterIdentityEntity?

    @Query("SELECT * FROM master_identities WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<MasterIdentityEntity?>
}
