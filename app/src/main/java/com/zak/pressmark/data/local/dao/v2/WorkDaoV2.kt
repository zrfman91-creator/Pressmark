// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/v2/WorkDaoV2.kt
package com.zak.pressmark.data.local.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.v2.DbSchemaV2
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

    @Query("SELECT * FROM ${DbSchemaV2.Work.TABLE} WHERE ${DbSchemaV2.Work.ID} = :workId LIMIT 1")
    suspend fun getById(workId: String): WorkEntityV2?

    @Query("SELECT * FROM ${DbSchemaV2.Work.TABLE} WHERE ${DbSchemaV2.Work.DISCOGS_MASTER_ID} = :masterId LIMIT 1")
    suspend fun getByDiscogsMasterId(masterId: Long): WorkEntityV2?

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        WHERE ${DbSchemaV2.Work.ARTIST_NORMALIZED} = :artistNorm
          AND ${DbSchemaV2.Work.TITLE_NORMALIZED} = :titleNorm
          AND ((:year IS NULL AND ${DbSchemaV2.Work.YEAR} IS NULL)
            OR ${DbSchemaV2.Work.YEAR} = :year)
        ORDER BY ${DbSchemaV2.Work.UPDATED_AT} DESC
        LIMIT 1
        """
    )
    suspend fun getByNormalized(artistNorm: String, titleNorm: String, year: Int?): WorkEntityV2?

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        WHERE ${DbSchemaV2.Work.ARTIST_NORMALIZED} = :artistNorm
          AND ${DbSchemaV2.Work.TITLE_NORMALIZED} = :titleNorm
        ORDER BY ${DbSchemaV2.Work.UPDATED_AT} DESC
        LIMIT 1
        """
    )
    suspend fun getByNormalizedIgnoringYear(artistNorm: String, titleNorm: String): WorkEntityV2?

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.UPDATED_AT} DESC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAll(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.TITLE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.ARTIST_LINE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByTitle(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.ARTIST_LINE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.TITLE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByArtist(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.YEAR} IS NULL,
                 ${DbSchemaV2.Work.YEAR} DESC,
                 ${DbSchemaV2.Work.ARTIST_LINE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.TITLE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByYearDesc(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.UPDATED_AT} DESC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByUpdatedDesc(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.TITLE} COLLATE NOCASE DESC,
                 ${DbSchemaV2.Work.ARTIST_LINE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByTitleDesc(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.ARTIST_LINE} COLLATE NOCASE DESC,
                 ${DbSchemaV2.Work.TITLE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByArtistDesc(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.YEAR} IS NULL,
                 ${DbSchemaV2.Work.YEAR} ASC,
                 ${DbSchemaV2.Work.ARTIST_LINE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.TITLE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByYearAsc(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.CREATED_AT} DESC,
                 ${DbSchemaV2.Work.TITLE} COLLATE NOCASE ASC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByCreatedDesc(): Flow<List<WorkEntityV2>>
}
