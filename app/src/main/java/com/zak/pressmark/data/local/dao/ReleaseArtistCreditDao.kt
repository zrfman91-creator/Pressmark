// file: app/src/main/java/com/zak/pressmark/data/local/dao/ReleaseArtistCreditDao.kt
package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.zak.pressmark.data.local.db.DbSchema.Artist
import com.zak.pressmark.data.local.db.DbSchema.ReleaseArtistCredit
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.data.local.entity.ReleaseArtistCreditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseArtistCreditDao {

    // -----------------------------
    // Writes
    // -----------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credit: ReleaseArtistCreditEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(credits: List<ReleaseArtistCreditEntity>): List<Long>

    @Query("DELETE FROM ${ReleaseArtistCredit.TABLE} WHERE ${ReleaseArtistCredit.RELEASE_ID} = :releaseId")
    suspend fun deleteByReleaseId(releaseId: String)

    /**
     * Replace all credits for a release in a single transaction.
     * Use this from repository code to keep credits clean.
     */
    @Transaction
    suspend fun replaceCreditsForRelease(releaseId: String, credits: List<ReleaseArtistCreditEntity>) {
        deleteByReleaseId(releaseId)
        if (credits.isNotEmpty()) insertAll(credits)
    }

    // -----------------------------
    // Reads for rendering
    // -----------------------------

    /**
     * Get raw credit rows (ordered) for a release.
     */
    @Query(
        """
        SELECT * FROM ${ReleaseArtistCredit.TABLE}
        WHERE ${ReleaseArtistCredit.RELEASE_ID} = :releaseId
        ORDER BY ${ReleaseArtistCredit.POSITION} ASC, ${ReleaseArtistCredit.ID} ASC
        """
    )
    suspend fun creditsForRelease(releaseId: String): List<ReleaseArtistCreditEntity>

    @Query(
        """
        SELECT * FROM ${ReleaseArtistCredit.TABLE}
        WHERE ${ReleaseArtistCredit.RELEASE_ID} = :releaseId
        ORDER BY ${ReleaseArtistCredit.POSITION} ASC, ${ReleaseArtistCredit.ID} ASC
        """
    )
    fun observeCreditsForRelease(releaseId: String): Flow<List<ReleaseArtistCreditEntity>>

    /**
     * Get artists for a release (ordered), optionally filtered by role.
     * This is useful for building the "Artist line" display.
     */
    @Query(
        """
        SELECT a.* FROM ${Artist.TABLE} a
        INNER JOIN ${ReleaseArtistCredit.TABLE} c
          ON c.${ReleaseArtistCredit.ARTIST_ID} = a.${Artist.ID}
        WHERE c.${ReleaseArtistCredit.RELEASE_ID} = :releaseId
          AND (:role IS NULL OR c.${ReleaseArtistCredit.ROLE} = :role)
        ORDER BY c.${ReleaseArtistCredit.POSITION} ASC, c.${ReleaseArtistCredit.ID} ASC
        """
    )
    suspend fun artistsForRelease(
        releaseId: String,
        role: String? = null, // pass "PRIMARY", "ORCHESTRA", etc.
    ): List<ArtistEntity>

    @Query(
        """
        SELECT a.* FROM ${Artist.TABLE} a
        INNER JOIN ${ReleaseArtistCredit.TABLE} c
          ON c.${ReleaseArtistCredit.ARTIST_ID} = a.${Artist.ID}
        WHERE c.${ReleaseArtistCredit.RELEASE_ID} = :releaseId
          AND (:role IS NULL OR c.${ReleaseArtistCredit.ROLE} = :role)
        ORDER BY c.${ReleaseArtistCredit.POSITION} ASC, c.${ReleaseArtistCredit.ID} ASC
        """
    )
    fun observeArtistsForRelease(
        releaseId: String,
        role: String? = null, // pass "PRIMARY", "ORCHESTRA", etc.
    ): Flow<List<ArtistEntity>>

    // -----------------------------
    // Browse/Search support
    // -----------------------------

    /**
     * Your core rule:
     * A–Z browse shows ONLY artists who have at least one PRIMARY credit.
     */
    @Query(
        """
        SELECT DISTINCT a.* FROM ${Artist.TABLE} a
        INNER JOIN ${ReleaseArtistCredit.TABLE} c
          ON c.${ReleaseArtistCredit.ARTIST_ID} = a.${Artist.ID}
        WHERE c.${ReleaseArtistCredit.ROLE} = :primaryRole
        ORDER BY a.${Artist.SORT_NAME} COLLATE NOCASE ASC
        """
    )
    suspend fun browseArtistsPrimaryOnly(primaryRole: String = "PRIMARY"): List<ArtistEntity>

    /**
     * Search artists (includes orchestras/ensembles/etc.), but can optionally filter by role.
     * This enables: searching "orchestra" and finding Glenn Miller Orchestra even if it
     * does not appear in A–Z browse.
     */
    @Query(
        """
        SELECT DISTINCT a.* FROM ${Artist.TABLE} a
        LEFT JOIN ${ReleaseArtistCredit.TABLE} c
          ON c.${ReleaseArtistCredit.ARTIST_ID} = a.${Artist.ID}
        WHERE (
            a.${Artist.DISPLAY_NAME} LIKE '%' || :query || '%'
            OR a.${Artist.SORT_NAME} LIKE '%' || :query || '%'
            OR a.${Artist.NAME_NORMALIZED} LIKE '%' || :query || '%'
        )
          AND (:role IS NULL OR c.${ReleaseArtistCredit.ROLE} = :role)
        ORDER BY a.${Artist.SORT_NAME} COLLATE NOCASE ASC
        """
    )
    suspend fun searchArtists(
        query: String,
        role: String? = null,
    ): List<ArtistEntity>
}
