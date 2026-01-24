package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.zak.pressmark.data.local.entity.v1.CatalogItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CatalogItemEntity): Long

    @Query("SELECT * FROM catalog_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CatalogItemEntity?

    @Query("SELECT * FROM catalog_items WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<CatalogItemEntity?>

    /**
     * Master-first list query entry point.
     * Build the query using SimpleSQLiteQuery so sorting/filtering stays inside SQLite.
     */
    @RawQuery(observedEntities = [CatalogItemEntity::class])
    fun observeList(query: SupportSQLiteQuery): Flow<List<CatalogItemEntity>>
}
