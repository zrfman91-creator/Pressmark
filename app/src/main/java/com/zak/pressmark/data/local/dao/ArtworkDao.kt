// file: app/src/main/java/com/zak/pressmark/data/local/dao/ArtworkDao.kt
package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.zak.pressmark.data.local.db.DbSchema.Artwork
import com.zak.pressmark.data.local.entity.ArtworkEntity

@Dao
interface ArtworkDao {

    // -----------------------------
    // Writes
    // -----------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artwork: ArtworkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ArtworkEntity>): List<Long>

    @Query("DELETE FROM ${Artwork.TABLE} WHERE ${Artwork.ID} = :artworkId")
    suspend fun deleteById(artworkId: Long)

    @Query("DELETE FROM ${Artwork.TABLE} WHERE ${Artwork.RELEASE_ID} = :releaseId")
    suspend fun deleteByReleaseId(releaseId: String)

    // -----------------------------
    // Reads
    // -----------------------------

    @Query(
        """
        SELECT * FROM ${Artwork.TABLE}
        WHERE ${Artwork.RELEASE_ID} = :releaseId
        ORDER BY ${Artwork.IS_PRIMARY} DESC, ${Artwork.ID} DESC
        """
    )
    suspend fun artworksForRelease(releaseId: String): List<ArtworkEntity>

    /**
     * Primary artwork used for list-row thumbnail.
     * If no primary exists, this returns the most recent artwork (fallback).
     */
    @Query(
        """
        SELECT * FROM ${Artwork.TABLE}
        WHERE ${Artwork.RELEASE_ID} = :releaseId
        ORDER BY ${Artwork.IS_PRIMARY} DESC, ${Artwork.ID} DESC
        LIMIT 1
        """
    )
    suspend fun primaryOrLatestForRelease(releaseId: String): ArtworkEntity?

    @Query(
        """
        SELECT * FROM ${Artwork.TABLE}
        WHERE ${Artwork.RELEASE_ID} = :releaseId
          AND ${Artwork.IS_PRIMARY} = 1
        LIMIT 1
        """
    )
    suspend fun primaryForRelease(releaseId: String): ArtworkEntity?

    // -----------------------------
    // Primary enforcement helpers
    // -----------------------------

    @Query(
        """
        UPDATE ${Artwork.TABLE}
        SET ${Artwork.IS_PRIMARY} = 0
        WHERE ${Artwork.RELEASE_ID} = :releaseId
        """
    )
    suspend fun clearPrimaryForRelease(releaseId: String)

    @Query(
        """
        UPDATE ${Artwork.TABLE}
        SET ${Artwork.IS_PRIMARY} = 1
        WHERE ${Artwork.ID} = :artworkId
        """
    )
    suspend fun markPrimaryById(artworkId: Long)

    /**
     * Ensures only one primary artwork per release.
     */
    @Transaction
    suspend fun setPrimaryArtwork(releaseId: String, artworkId: Long) {
        clearPrimaryForRelease(releaseId)
        markPrimaryById(artworkId)
    }
}
