// file: app/src/main/java/com/zak/pressmark/data/local/dao/ArtistDao.kt
package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.DbSchema.Artist
import com.zak.pressmark.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {

    @Query("SELECT * FROM ${Artist.TABLE} ORDER BY ${Artist.SORT_NAME} COLLATE NOCASE")
    fun observeAll(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM ${Artist.TABLE} WHERE ${Artist.ID} = :id LIMIT 1")
    fun observeById(id: Long): Flow<ArtistEntity?>

    @Query("SELECT * FROM ${Artist.TABLE} WHERE ${Artist.ID} = :id LIMIT 1")
    suspend fun getById(id: Long): ArtistEntity?

    @Query("SELECT * FROM ${Artist.TABLE} WHERE ${Artist.NAME_NORMALIZED} = :normalized LIMIT 1")
    suspend fun getByNormalized(normalized: String): ArtistEntity?

    @Query("SELECT ${Artist.ID} FROM ${Artist.TABLE} WHERE ${Artist.NAME_NORMALIZED} = :normalized LIMIT 1")
    suspend fun getIdByNormalized(normalized: String): Long?

    /**
     * Returns:
     * - new rowId if inserted
     * - -1 if ignored due to unique constraint
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(artist: ArtistEntity): Long

    /**
     * Optional helper for autocomplete, etc.
     */
    @Query(
        """
        SELECT * FROM ${Artist.TABLE}
        WHERE ${Artist.NAME_NORMALIZED} LIKE :normalizedPrefix || '%'
        ORDER BY ${Artist.SORT_NAME} COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun searchByNormalizedPrefix(
        normalizedPrefix: String,
        limit: Int = 20,
    ): List<ArtistEntity>
}
