// file: app/src/main/java/com/zak/pressmark/data/local/dao/ReleaseDao.kt
package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zak.pressmark.data.local.db.DbSchema.Release
import com.zak.pressmark.data.local.entity.ReleaseEntity

@Dao
interface ReleaseDao {

    // -----------------------------
    // Writes
    // -----------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(release: ReleaseEntity)

    @Update
    suspend fun update(release: ReleaseEntity)

    @Query("DELETE FROM ${Release.TABLE} WHERE ${Release.ID} = :releaseId")
    suspend fun deleteById(releaseId: String)

    // -----------------------------
    // Reads
    // -----------------------------

    @Query(
        """
        SELECT * FROM ${Release.TABLE}
        ORDER BY ${Release.ADDED_AT} DESC
        """
    )
    suspend fun listAll(): List<ReleaseEntity>

    @Query(
        """
        SELECT * FROM ${Release.TABLE}
        WHERE ${Release.ID} = :releaseId
        LIMIT 1
        """
    )
    suspend fun getById(releaseId: String): ReleaseEntity?

    @Query(
        """
        SELECT * FROM ${Release.TABLE}
        WHERE ${Release.TITLE} LIKE '%' || :query || '%'
        ORDER BY ${Release.ADDED_AT} DESC
        """
    )
    suspend fun searchByTitle(query: String): List<ReleaseEntity>
}
