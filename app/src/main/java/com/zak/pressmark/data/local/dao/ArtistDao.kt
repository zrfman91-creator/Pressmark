package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

import com.zak.pressmark.data.local.db.DbSchema.Artist
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.data.local.entity.normalizeArtistName
import androidx.room.Transaction
import com.zak.pressmark.data.local.db.DbSchema.Album


@Dao
interface ArtistDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ArtistEntity): Long

    // Canonical lookup (used by ArtistRepository.getOrCreateArtistId)
    @Query("SELECT * FROM ${Artist.TABLE} WHERE ${Artist.NAME_NORMALIZED} = :normalized LIMIT 1")
    suspend fun findByNormalizedName(normalized: String): ArtistEntity?

    // Needed by ArtistRepository.observeById() and ArtistViewModel
    @Query("SELECT * FROM ${Artist.TABLE} WHERE ${Artist.ID} = :id LIMIT 1")
    fun observeById(id: Long): Flow<ArtistEntity?>

    // Optional convenience (sometimes helpful)
    @Query("SELECT * FROM ${Artist.TABLE} WHERE ${Artist.ID} = :id LIMIT 1")
    suspend fun getById(id: Long): ArtistEntity?

    // “Top artists” list — uses sortName (NOT playCount; you don’t have that column)
    @Query(
        """
        SELECT * FROM ${Artist.TABLE}
        ORDER BY ${Artist.SORT_NAME} COLLATE NOCASE
        LIMIT :limit
        """
    )
    fun observeTopArtists(limit: Int): Flow<List<ArtistEntity>>

    /**
     * Suggestions — use the indexed normalized column so search scales as the table grows.
     *
     * NOTE: Prefix match keeps the query index-friendly; callers can pass partial names.
     */
    @Query(
        """
        SELECT * FROM ${Artist.TABLE}
        WHERE ${Artist.NAME_NORMALIZED} LIKE :normalizedPrefix || '%'
        ORDER BY ${Artist.SORT_NAME} COLLATE NOCASE
        LIMIT :limit
        """
    )
    fun searchByNormalizedPrefix(normalizedPrefix: String, limit: Int): Flow<List<ArtistEntity>>

    /** Backwards-compatible entry point for callers already using searchByName(query, limit). */
    fun searchByName(query: String, limit: Int): Flow<List<ArtistEntity>> =
        searchByNormalizedPrefix(normalizeArtistName(query), limit)

    @Query("""
    UPDATE ${Artist.TABLE}
    SET ${Artist.DISPLAY_NAME} = :displayName,
        ${Artist.SORT_NAME} = :sortName
    WHERE ${Artist.ID} = :id
""")
    suspend fun updateNames(id: Long, displayName: String, sortName: String): Int

    @Query("""
    UPDATE ${Album.TABLE}
    SET ${Album.ARTIST_ID} = :canonicalId
    WHERE ${Album.ARTIST_ID} = :duplicateId
""")
    suspend fun reassignAlbumsArtist(duplicateId: Long, canonicalId: Long): Int

    // Delete the duplicate artist row
    @Query("""
    DELETE FROM ${Artist.TABLE}
    WHERE ${Artist.ID} = :duplicateId
""")
    suspend fun deleteById(duplicateId: Long): Int


    //One atomic operation: repoint albums & delete duplicate artist
    @Transaction
    suspend fun mergeAndDeleteArtist(duplicateId: Long, canonicalId: Long): MergeArtistsResult {
        val moved = reassignAlbumsArtist(duplicateId, canonicalId)
        val deleted = deleteById(duplicateId)
        return MergeArtistsResult(albumsMoved = moved, artistsDeleted = deleted)
    }
}

data class MergeArtistsResult(
    val albumsMoved: Int,
    val artistsDeleted: Int
)