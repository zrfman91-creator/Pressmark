// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/ReleaseDaoV2.kt
package com.zak.pressmark.data.local.dao.v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zak.pressmark.data.local.db.DbSchema.Artist
import com.zak.pressmark.data.local.db.DbSchema.Artwork
import com.zak.pressmark.data.local.db.DbSchema.Release
import com.zak.pressmark.data.local.db.DbSchema.ReleaseArtistCredit
import com.zak.pressmark.data.local.entity.v1.ReleaseEntity
import com.zak.pressmark.data.local.model.ReleaseListRowFlat
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDao {

    // Writes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(release: ReleaseEntity)

    @Update
    suspend fun update(release: ReleaseEntity)

    @Query(
        """
        UPDATE ${Release.TABLE}
        SET ${Release.TITLE} = :title,
            ${Release.RELEASE_YEAR} = :releaseYear,
            ${Release.LABEL} = :label,
            ${Release.CATALOG_NO} = :catalogNo,
            ${Release.FORMAT} = :format,
            ${Release.BARCODE} = :barcode,
            ${Release.COUNTRY} = :country,
            ${Release.RELEASE_TYPE} = :releaseType,
            ${Release.NOTES} = :notes,
            ${Release.RATING} = :rating,
            ${Release.LAST_PLAYED_AT} = :lastPlayedAt
        WHERE ${Release.ID} = :releaseId
        """
    )
    suspend fun updateReleaseDetails(
        releaseId: String,
        title: String,
        releaseYear: Int?,
        label: String?,
        catalogNo: String?,
        format: String?,
        barcode: String?,
        country: String?,
        releaseType: String?,
        notes: String?,
        rating: Int?,
        lastPlayedAt: Long?,
    ): Int

    @Query("DELETE FROM ${Release.TABLE} WHERE ${Release.ID} = :releaseId")
    suspend fun deleteById(releaseId: String)

    // Reads
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
        WHERE ${Release.DISCOGS_RELEASE_ID} = :discogsReleaseId
        LIMIT 1
        """
    )
    suspend fun getByDiscogsReleaseId(discogsReleaseId: Long): ReleaseEntity?

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


    // Release list read-model (flat rows; group by release.id in higher layer)
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
