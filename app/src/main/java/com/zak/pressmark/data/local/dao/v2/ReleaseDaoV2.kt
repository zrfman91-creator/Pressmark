// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/v2/ReleaseDaoV2.kt
package com.zak.pressmark.data.local.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.DbSchemaV2
import com.zak.pressmark.data.local.entity.v2.ReleaseEntityV2
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDaoV2 {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(release: ReleaseEntityV2)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(releases: List<ReleaseEntityV2>)

    @Query("SELECT * FROM ${DbSchemaV2.Release.TABLE} WHERE ${DbSchemaV2.Release.ID} = :releaseId LIMIT 1")
    suspend fun getById(releaseId: String): ReleaseEntityV2?

    @Query("SELECT * FROM ${DbSchemaV2.Release.TABLE} WHERE ${DbSchemaV2.Release.WORK_ID} = :workId ORDER BY ${DbSchemaV2.Release.RELEASE_YEAR} DESC")
    fun observeByWorkId(workId: String): Flow<List<ReleaseEntityV2>>
}
