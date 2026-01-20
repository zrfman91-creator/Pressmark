// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/ReleaseDao.kt
package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zak.pressmark.data.local.db.DbSchema.Artist
import com.zak.pressmark.data.local.db.DbSchema.Artwork
import com.zak.pressmark.data.local.db.DbSchema.Release
import com.zak.pressmark.data.local.db.DbSchema.ReleaseArtistCredit
import com.zak.pressmark.data.local.entity.ReleaseEntity
import com.zak.pressmark.data.local.model.ReleaseListRowFlat
import kotlinx.coroutines.flow.Flow

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
        WHERE ${Release.ID} = :releaseId
        LIMIT 1
        """
    )
    fun observeById(releaseId: String): Flow<ReleaseEntity?>

    @Query(
        """
        SELECT * FROM ${Release.TABLE}
        WHERE ${Release.TITLE} LIKE '%' || :query || '%'
        ORDER BY ${Release.ADDED_AT} DESC
        """
    )
    suspend fun searchByTitle(query: String): List<ReleaseEntity>

    // -----------------------------
    // Release list read-model (flat rows; group by release.id in higher layer)
    // -----------------------------

    /**
     * Main list query without N+1:
     * - ReleaseEntity columns (embedded)
     * - Primary-or-latest artwork (id + uri)
     * - Credit row (artistId/role/position/displayHint) + joined Artist.displayName
     *
     * Returns 0..N rows per release (one per credit). Releases with no credits still return 1 row
     * with credit columns null due to LEFT JOIN.
     */
    @Query(
        """
        SELECT
          r.*,

          aw.${Artwork.ID} AS artwork_id,
          aw.${Artwork.URI} AS artwork_uri,

          c.${ReleaseArtistCredit.ARTIST_ID} AS credit_artist_id,
          c.${ReleaseArtistCredit.ROLE} AS credit_role,
          c.${ReleaseArtistCredit.POSITION} AS credit_position,
          c.${ReleaseArtistCredit.DISPLAY_HINT} AS credit_display_hint,

          a.${Artist.DISPLAY_NAME} AS artist_display_name

        FROM ${Release.TABLE} r

        LEFT JOIN ${Artwork.TABLE} aw
          ON aw.${Artwork.ID} = (
            SELECT a2.${Artwork.ID}
            FROM ${Artwork.TABLE} a2
            WHERE a2.${Artwork.RELEASE_ID} = r.${Release.ID}
            ORDER BY a2.${Artwork.IS_PRIMARY} DESC, a2.${Artwork.ID} DESC
            LIMIT 1
          )

        LEFT JOIN ${ReleaseArtistCredit.TABLE} c
          ON c.${ReleaseArtistCredit.RELEASE_ID} = r.${Release.ID}

        LEFT JOIN ${Artist.TABLE} a
          ON a.${Artist.ID} = c.${ReleaseArtistCredit.ARTIST_ID}

        ORDER BY
          r.${Release.ADDED_AT} DESC,
          c.${ReleaseArtistCredit.POSITION} ASC,
          c.${ReleaseArtistCredit.ID} ASC
        """
    )
    suspend fun listReleaseRowsFlat(): List<ReleaseListRowFlat>

    /**
     * Live (reactive) version of [listReleaseRowsFlat].
     *
     * Room will re-emit when any observed table in this query changes
     * (Release, Artwork, ReleaseArtistCredit, Artist).
     */
    @Query(
        """
        SELECT
          r.*,

          aw.${Artwork.ID} AS artwork_id,
          aw.${Artwork.URI} AS artwork_uri,

          c.${ReleaseArtistCredit.ARTIST_ID} AS credit_artist_id,
          c.${ReleaseArtistCredit.ROLE} AS credit_role,
          c.${ReleaseArtistCredit.POSITION} AS credit_position,
          c.${ReleaseArtistCredit.DISPLAY_HINT} AS credit_display_hint,

          a.${Artist.DISPLAY_NAME} AS artist_display_name

        FROM ${Release.TABLE} r

        LEFT JOIN ${Artwork.TABLE} aw
          ON aw.${Artwork.ID} = (
            SELECT a2.${Artwork.ID}
            FROM ${Artwork.TABLE} a2
            WHERE a2.${Artwork.RELEASE_ID} = r.${Release.ID}
            ORDER BY a2.${Artwork.IS_PRIMARY} DESC, a2.${Artwork.ID} DESC
            LIMIT 1
          )

        LEFT JOIN ${ReleaseArtistCredit.TABLE} c
          ON c.${ReleaseArtistCredit.RELEASE_ID} = r.${Release.ID}

        LEFT JOIN ${Artist.TABLE} a
          ON a.${Artist.ID} = c.${ReleaseArtistCredit.ARTIST_ID}

        ORDER BY
          r.${Release.ADDED_AT} DESC,
          c.${ReleaseArtistCredit.POSITION} ASC,
          c.${ReleaseArtistCredit.ID} ASC
        """
    )
    fun observeReleaseRowsFlat(): Flow<List<ReleaseListRowFlat>>
}
