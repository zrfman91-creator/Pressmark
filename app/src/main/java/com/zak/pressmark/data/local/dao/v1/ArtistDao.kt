// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/ArtistDao.kt
package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

import com.zak.pressmark.data.local.db.DbSchema.Artist
import com.zak.pressmark.data.local.db.DbSchema.ReleaseArtistCredit
import com.zak.pressmark.data.local.entity.v1.ArtistEntity
import com.zak.pressmark.data.local.entity.v1.normalizeArtistName

@Dao
interface ArtistDao {

    // -------------------------------------------------
    // Basic CRUD / lookup (unchanged)
    // -------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ArtistEntity): Long

    @Query("SELECT * FROM ${Artist.TABLE} WHERE ${Artist.NAME_NORMALIZED} = :normalized LIMIT 1")
    suspend fun findByNormalizedName(normalized: String): ArtistEntity?

    @Query("SELECT * FROM ${Artist.TABLE} WHERE ${Artist.ID} = :id LIMIT 1")
    fun observeById(id: Long): Flow<ArtistEntity?>

    @Query("SELECT * FROM ${Artist.TABLE} WHERE ${Artist.ID} = :id LIMIT 1")
    suspend fun getById(id: Long): ArtistEntity?

    @Query(
        """
        SELECT * FROM ${Artist.TABLE}
        ORDER BY ${Artist.SORT_NAME} COLLATE NOCASE
        LIMIT :limit
        """
    )
    fun observeTopArtists(limit: Int): Flow<List<ArtistEntity>>

    @Query(
        """
        SELECT * FROM ${Artist.TABLE}
        WHERE ${Artist.NAME_NORMALIZED} LIKE :normalizedPrefix || '%'
        ORDER BY ${Artist.SORT_NAME} COLLATE NOCASE
        LIMIT :limit
        """
    )
    fun searchByNormalizedPrefix(
        normalizedPrefix: String,
        limit: Int
    ): Flow<List<ArtistEntity>>

    fun searchByName(query: String, limit: Int): Flow<List<ArtistEntity>> =
        searchByNormalizedPrefix(normalizeArtistName(query), limit)

    @Query(
        """
        UPDATE ${Artist.TABLE}
        SET ${Artist.DISPLAY_NAME} = :displayName,
            ${Artist.SORT_NAME} = :sortName
        WHERE ${Artist.ID} = :id
        """
    )
    suspend fun updateNames(id: Long, displayName: String, sortName: String): Int

    // -------------------------------------------------
    // Merge / delete support (NEW MODEL)
    // -------------------------------------------------

    /**
     * Reassign all release credits from duplicate artist → canonical artist.
     */
    @Query(
        """
        UPDATE ${ReleaseArtistCredit.TABLE}
        SET ${ReleaseArtistCredit.ARTIST_ID} = :canonicalId
        WHERE ${ReleaseArtistCredit.ARTIST_ID} = :duplicateId
        """
    )
    suspend fun reassignCredits(
        duplicateId: Long,
        canonicalId: Long
    ): Int

    /**
     * Remove duplicate credit rows that may result from a merge
     * (same release, same role, same artist after reassignment).
     */
    @Query(
        """
        DELETE FROM ${ReleaseArtistCredit.TABLE}
        WHERE ${ReleaseArtistCredit.ARTIST_ID} = :canonicalId
          AND ${ReleaseArtistCredit.ID} NOT IN (
              SELECT MIN(${ReleaseArtistCredit.ID})
              FROM ${ReleaseArtistCredit.TABLE}
              WHERE ${ReleaseArtistCredit.ARTIST_ID} = :canonicalId
              GROUP BY
                ${ReleaseArtistCredit.RELEASE_ID},
                ${ReleaseArtistCredit.ROLE},
                ${ReleaseArtistCredit.POSITION}
          )
        """
    )
    suspend fun dedupeCreditsForArtist(canonicalId: Long): Int

    /**
     * Count how many credits an artist still has.
     * Used to block deletes unless merged.
     */
    @Query(
        """
        SELECT COUNT(*) FROM ${ReleaseArtistCredit.TABLE}
        WHERE ${ReleaseArtistCredit.ARTIST_ID} = :artistId
        """
    )
    suspend fun countCredits(artistId: Long): Int

    /**
     * Delete an artist row by id.
     */
    @Query(
        """
        DELETE FROM ${Artist.TABLE}
        WHERE ${Artist.ID} = :artistId
        """
    )
    suspend fun deleteById(artistId: Long): Int

    /**
     * Atomic merge:
     * - Move all credits
     * - Dedupe collisions
     * - Delete duplicate artist
     */
    @Transaction
    suspend fun mergeArtist(
        duplicateId: Long,
        canonicalId: Long
    ): MergeArtistsResult {
        val moved = reassignCredits(duplicateId, canonicalId)
        val deduped = dedupeCreditsForArtist(canonicalId)
        val deleted = deleteById(duplicateId)
        return MergeArtistsResult(
            creditsMoved = moved,
            creditsDeduped = deduped,
            artistsDeleted = deleted
        )
    }
}

/**
 * Result payload for merge operations.
 * Useful for logging, snackbars, or debug UI.
 */
data class MergeArtistsResult(
    val creditsMoved: Int,
    val creditsDeduped: Int,
    val artistsDeleted: Int
)
