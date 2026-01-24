// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/v2/WorkDaoV2.kt
package com.zak.pressmark.data.local.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.DbSchemaV2
import com.zak.pressmark.data.local.entity.v2.WorkEntityV2
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDaoV2 {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(work: WorkEntityV2)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(works: List<WorkEntityV2>)

    @Query("DELETE FROM ${DbSchemaV2.Work.TABLE} WHERE ${DbSchemaV2.Work.ID} = :workId")
    suspend fun deleteById(workId: String)

    @Query("SELECT * FROM ${DbSchemaV2.Work.TABLE} WHERE ${DbSchemaV2.Work.ID} = :workId LIMIT 1")
    fun observeById(workId: String): Flow<WorkEntityV2?>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.TITLE} COLLATE NOCASE ASC
        """
    )
    fun observeAllByTitle(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.ARTIST_LINE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.TITLE} COLLATE NOCASE ASC
        """
    )
    fun observeAllByArtist(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.YEAR} DESC,
                 ${DbSchemaV2.Work.TITLE} COLLATE NOCASE ASC
        """
    )
    fun observeAllByYearDesc(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.UPDATED_AT} DESC
        """
    )
    fun observeAllByUpdatedDesc(): Flow<List<WorkEntityV2>>
}
