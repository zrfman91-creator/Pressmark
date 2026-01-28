// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/v2/WorkDaoV2.kt
package com.zak.pressmark.data.local.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
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

    // --- Sort keys: ignore leading "The " (case-insensitive) ---
    // NOTE: ltrim() removes leading spaces; lower() makes it case-insensitive.
    // substr(..., 5) removes "the " (4 chars) => starting at position 5 (1-indexed).
    // We keep COLLATE NOCASE on the final expression for consistent ordering.

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY
          ${DbSchemaV2.Work.TITLE_SORT} ASC,
          ${DbSchemaV2.Work.ARTIST_SORT} ASC,
          ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByTitle(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY
          ${DbSchemaV2.Work.TITLE_SORT} DESC,
          ${DbSchemaV2.Work.ARTIST_SORT} ASC,
          ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByTitleDesc(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY
          ${DbSchemaV2.Work.ARTIST_SORT} ASC,
          ${DbSchemaV2.Work.TITLE_SORT} ASC,
          ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByArtist(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY
          ${DbSchemaV2.Work.ARTIST_SORT} DESC,
          ${DbSchemaV2.Work.TITLE_SORT} ASC,
          ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByArtistDesc(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.YEAR} IS NULL,
                 ${DbSchemaV2.Work.YEAR} DESC,
                 ${DbSchemaV2.Work.ARTIST_SORT} ASC,
                 ${DbSchemaV2.Work.TITLE_SORT} ASC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByYearDesc(): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT * FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.YEAR} IS NULL,
                 ${DbSchemaV2.Work.YEAR} ASC,
                 ${DbSchemaV2.Work.ARTIST_SORT} ASC,
                 ${DbSchemaV2.Work.TITLE_SORT} ASC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByYearAsc(): Flow<List<WorkEntityV2>>

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
        ORDER BY ${DbSchemaV2.Work.CREATED_AT} DESC,
                 ${DbSchemaV2.Work.TITLE_SORT} ASC,
                 ${DbSchemaV2.Work.ARTIST_SORT} ASC,
                 ${DbSchemaV2.Work.ID} ASC
        """
    )
    fun observeAllByCreatedDesc(): Flow<List<WorkEntityV2>>

    @RawQuery(observedEntities = [WorkEntityV2::class])
    fun observeWorksByQuery(query: SupportSQLiteQuery): Flow<List<WorkEntityV2>>

    @Query(
        """
        SELECT DISTINCT ${DbSchemaV2.Work.ARTIST_LINE} AS label,
                        ${DbSchemaV2.Work.ARTIST_SORT} AS sortKey
        FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.ARTIST_SORT} ASC, ${DbSchemaV2.Work.ARTIST_LINE} ASC
        """
    )
    fun observeArtistHeadings(): Flow<List<ArtistHeading>>

    @Query(
        """
        SELECT DISTINCT ${DbSchemaV2.Work.YEAR} AS year
        FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.YEAR} IS NULL, ${DbSchemaV2.Work.YEAR} ASC
        """
    )
    fun observeYearHeadingsAsc(): Flow<List<YearHeading>>

    @Query(
        """
        SELECT DISTINCT ${DbSchemaV2.Work.YEAR} AS year
        FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY ${DbSchemaV2.Work.YEAR} IS NULL DESC, ${DbSchemaV2.Work.YEAR} DESC
        """
    )
    fun observeYearHeadingsDesc(): Flow<List<YearHeading>>

    @Query(
        """
        SELECT DISTINCT
          CASE
            WHEN ${DbSchemaV2.Work.YEAR} IS NULL THEN NULL
            ELSE (${DbSchemaV2.Work.YEAR} / 10) * 10
          END AS decade
        FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY decade IS NULL, decade ASC
        """
    )
    fun observeDecadeHeadingsAsc(): Flow<List<DecadeHeading>>

    @Query(
        """
        SELECT DISTINCT
          CASE
            WHEN ${DbSchemaV2.Work.YEAR} IS NULL THEN NULL
            ELSE (${DbSchemaV2.Work.YEAR} / 10) * 10
          END AS decade
        FROM ${DbSchemaV2.Work.TABLE}
        ORDER BY decade IS NULL DESC, decade DESC
        """
    )
    fun observeDecadeHeadingsDesc(): Flow<List<DecadeHeading>>

    @Query(
        """
        SELECT label, normalized FROM (
            SELECT ${DbSchemaV2.Genre.NAME_DISPLAY} AS label,
                   ${DbSchemaV2.Genre.NAME_NORMALIZED} AS normalized
            FROM ${DbSchemaV2.Genre.TABLE}
            INNER JOIN ${DbSchemaV2.WorkGenre.TABLE}
              ON ${DbSchemaV2.WorkGenre.TABLE}.${DbSchemaV2.WorkGenre.GENRE_ID} = ${DbSchemaV2.Genre.TABLE}.${DbSchemaV2.Genre.ID}
            INNER JOIN ${DbSchemaV2.Work.TABLE}
              ON ${DbSchemaV2.Work.TABLE}.${DbSchemaV2.Work.ID} = ${DbSchemaV2.WorkGenre.TABLE}.${DbSchemaV2.WorkGenre.WORK_ID}
            GROUP BY ${DbSchemaV2.Genre.TABLE}.${DbSchemaV2.Genre.ID}
            UNION ALL
            SELECT 'Unknown genre' AS label, 'unknown genre' AS normalized
            WHERE EXISTS (
                SELECT 1 FROM ${DbSchemaV2.Work.TABLE}
                WHERE NOT EXISTS (
                    SELECT 1 FROM ${DbSchemaV2.WorkGenre.TABLE}
                    WHERE ${DbSchemaV2.WorkGenre.TABLE}.${DbSchemaV2.WorkGenre.WORK_ID} = ${DbSchemaV2.Work.TABLE}.${DbSchemaV2.Work.ID}
                )
            )
        )
        ORDER BY normalized ASC, label ASC
        """
    )
    fun observeGenreHeadings(): Flow<List<NamedHeading>>

    @Query(
        """
        SELECT label, normalized FROM (
            SELECT ${DbSchemaV2.Style.NAME_DISPLAY} AS label,
                   ${DbSchemaV2.Style.NAME_NORMALIZED} AS normalized
            FROM ${DbSchemaV2.Style.TABLE}
            INNER JOIN ${DbSchemaV2.WorkStyle.TABLE}
              ON ${DbSchemaV2.WorkStyle.TABLE}.${DbSchemaV2.WorkStyle.STYLE_ID} = ${DbSchemaV2.Style.TABLE}.${DbSchemaV2.Style.ID}
            INNER JOIN ${DbSchemaV2.Work.TABLE}
              ON ${DbSchemaV2.Work.TABLE}.${DbSchemaV2.Work.ID} = ${DbSchemaV2.WorkStyle.TABLE}.${DbSchemaV2.WorkStyle.WORK_ID}
            GROUP BY ${DbSchemaV2.Style.TABLE}.${DbSchemaV2.Style.ID}
            UNION ALL
            SELECT 'Unknown style' AS label, 'unknown style' AS normalized
            WHERE EXISTS (
                SELECT 1 FROM ${DbSchemaV2.Work.TABLE}
                WHERE NOT EXISTS (
                    SELECT 1 FROM ${DbSchemaV2.WorkStyle.TABLE}
                    WHERE ${DbSchemaV2.WorkStyle.TABLE}.${DbSchemaV2.WorkStyle.WORK_ID} = ${DbSchemaV2.Work.TABLE}.${DbSchemaV2.Work.ID}
                )
            )
        )
        ORDER BY normalized ASC, label ASC
        """
    )
    fun observeStyleHeadings(): Flow<List<NamedHeading>>

    @Query(
        """
        SELECT DISTINCT ${DbSchemaV2.Work.ARTIST_LINE} AS label,
                        ${DbSchemaV2.Work.ARTIST_SORT} AS sortKey
        FROM ${DbSchemaV2.Work.TABLE}
        WHERE (${DbSchemaV2.Work.YEAR} = :year OR (${DbSchemaV2.Work.YEAR} IS NULL AND :year IS NULL))
        ORDER BY ${DbSchemaV2.Work.ARTIST_SORT} ASC, ${DbSchemaV2.Work.ARTIST_LINE} ASC
        """
    )
    fun observeArtistHeadingsForYear(year: Int?): Flow<List<ArtistHeading>>

    @Query(
        """
        SELECT DISTINCT ${DbSchemaV2.Work.ARTIST_LINE} AS label,
                        ${DbSchemaV2.Work.ARTIST_SORT} AS sortKey
        FROM ${DbSchemaV2.Work.TABLE}
        WHERE ${DbSchemaV2.Work.YEAR} BETWEEN :startYear AND :endYear
        ORDER BY ${DbSchemaV2.Work.ARTIST_SORT} ASC, ${DbSchemaV2.Work.ARTIST_LINE} ASC
        """
    )
    fun observeArtistHeadingsForDecade(startYear: Int, endYear: Int): Flow<List<ArtistHeading>>

    @Query(
        """
        SELECT DISTINCT ${DbSchemaV2.Work.ARTIST_LINE} AS label,
                        ${DbSchemaV2.Work.ARTIST_SORT} AS sortKey
        FROM ${DbSchemaV2.Work.TABLE}
        WHERE ${DbSchemaV2.Work.YEAR} IS NULL
        ORDER BY ${DbSchemaV2.Work.ARTIST_SORT} ASC, ${DbSchemaV2.Work.ARTIST_LINE} ASC
        """
    )
    fun observeArtistHeadingsForUnknownYear(): Flow<List<ArtistHeading>>

    @Query(
        """
        SELECT DISTINCT ${DbSchemaV2.Work.ARTIST_LINE} AS label,
                        ${DbSchemaV2.Work.ARTIST_SORT} AS sortKey
        FROM ${DbSchemaV2.Work.TABLE}
        WHERE ${DbSchemaV2.Work.ID} IN (
            SELECT ${DbSchemaV2.WorkGenre.TABLE}.${DbSchemaV2.WorkGenre.WORK_ID}
            FROM ${DbSchemaV2.WorkGenre.TABLE}
            INNER JOIN ${DbSchemaV2.Genre.TABLE}
              ON ${DbSchemaV2.Genre.TABLE}.${DbSchemaV2.Genre.ID} = ${DbSchemaV2.WorkGenre.TABLE}.${DbSchemaV2.WorkGenre.GENRE_ID}
            WHERE ${DbSchemaV2.Genre.TABLE}.${DbSchemaV2.Genre.NAME_NORMALIZED} = :genreNormalized
        )
        ORDER BY ${DbSchemaV2.Work.ARTIST_SORT} ASC, ${DbSchemaV2.Work.ARTIST_LINE} ASC
        """
    )
    fun observeArtistHeadingsForGenre(genreNormalized: String): Flow<List<ArtistHeading>>

    @Query(
        """
        SELECT DISTINCT ${DbSchemaV2.Work.ARTIST_LINE} AS label,
                        ${DbSchemaV2.Work.ARTIST_SORT} AS sortKey
        FROM ${DbSchemaV2.Work.TABLE}
        WHERE NOT EXISTS (
            SELECT 1 FROM ${DbSchemaV2.WorkGenre.TABLE}
            WHERE ${DbSchemaV2.WorkGenre.TABLE}.${DbSchemaV2.WorkGenre.WORK_ID} = ${DbSchemaV2.Work.TABLE}.${DbSchemaV2.Work.ID}
        )
        ORDER BY ${DbSchemaV2.Work.ARTIST_SORT} ASC, ${DbSchemaV2.Work.ARTIST_LINE} ASC
        """
    )
    fun observeArtistHeadingsForUnknownGenre(): Flow<List<ArtistHeading>>

    @Query(
        """
        SELECT DISTINCT ${DbSchemaV2.Work.ARTIST_LINE} AS label,
                        ${DbSchemaV2.Work.ARTIST_SORT} AS sortKey
        FROM ${DbSchemaV2.Work.TABLE}
        WHERE ${DbSchemaV2.Work.ID} IN (
            SELECT ${DbSchemaV2.WorkStyle.TABLE}.${DbSchemaV2.WorkStyle.WORK_ID}
            FROM ${DbSchemaV2.WorkStyle.TABLE}
            INNER JOIN ${DbSchemaV2.Style.TABLE}
              ON ${DbSchemaV2.Style.TABLE}.${DbSchemaV2.Style.ID} = ${DbSchemaV2.WorkStyle.TABLE}.${DbSchemaV2.WorkStyle.STYLE_ID}
            WHERE ${DbSchemaV2.Style.TABLE}.${DbSchemaV2.Style.NAME_NORMALIZED} = :styleNormalized
        )
        ORDER BY ${DbSchemaV2.Work.ARTIST_SORT} ASC, ${DbSchemaV2.Work.ARTIST_LINE} ASC
        """
    )
    fun observeArtistHeadingsForStyle(styleNormalized: String): Flow<List<ArtistHeading>>

    @Query(
        """
        SELECT DISTINCT ${DbSchemaV2.Work.ARTIST_LINE} AS label,
                        ${DbSchemaV2.Work.ARTIST_SORT} AS sortKey
        FROM ${DbSchemaV2.Work.TABLE}
        WHERE NOT EXISTS (
            SELECT 1 FROM ${DbSchemaV2.WorkStyle.TABLE}
            WHERE ${DbSchemaV2.WorkStyle.TABLE}.${DbSchemaV2.WorkStyle.WORK_ID} = ${DbSchemaV2.Work.TABLE}.${DbSchemaV2.Work.ID}
        )
        ORDER BY ${DbSchemaV2.Work.ARTIST_SORT} ASC, ${DbSchemaV2.Work.ARTIST_LINE} ASC
        """
    )
    fun observeArtistHeadingsForUnknownStyle(): Flow<List<ArtistHeading>>
}

data class ArtistHeading(
    val label: String,
    val sortKey: String,
)

data class YearHeading(
    val year: Int?,
)

data class DecadeHeading(
    val decade: Int?,
)

data class NamedHeading(
    val label: String,
    val normalized: String,
)
