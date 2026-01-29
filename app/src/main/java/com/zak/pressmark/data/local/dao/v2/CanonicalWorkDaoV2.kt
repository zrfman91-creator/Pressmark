package com.zak.pressmark.data.local.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.v2.DbSchemaV2
import com.zak.pressmark.data.local.entity.v2.CanonicalWorkEntityV2
import kotlinx.coroutines.flow.Flow

@Dao
interface CanonicalWorkDaoV2 {

    data class CanonicalWorkSummary(
        val id: String,
        val title: String,
        val year: Int?,
        val formatType: String?,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(work: CanonicalWorkEntityV2)

    @Query("SELECT * FROM ${DbSchemaV2.CanonicalWork.TABLE} WHERE ${DbSchemaV2.CanonicalWork.DISCOGS_MASTER_ID} = :discogsMasterId LIMIT 1")
    suspend fun getByDiscogsMasterId(discogsMasterId: Long): CanonicalWorkEntityV2?

    @Query(
        """
        SELECT ${DbSchemaV2.CanonicalWork.ID} AS id,
               ${DbSchemaV2.CanonicalWork.TITLE} AS title,
               ${DbSchemaV2.CanonicalWork.YEAR} AS year,
               ${DbSchemaV2.CanonicalWork.FORMAT_TYPE} AS formatType
        FROM ${DbSchemaV2.CanonicalWork.TABLE}
        WHERE ${DbSchemaV2.CanonicalWork.ARTIST_ID} = :artistId
          AND ${DbSchemaV2.CanonicalWork.FORMAT_TYPE} IN (:formats)
          AND ${DbSchemaV2.CanonicalWork.RELEASE_TYPE} = :releaseType
        ORDER BY ${DbSchemaV2.CanonicalWork.YEAR} IS NULL,
                 ${DbSchemaV2.CanonicalWork.YEAR} ASC,
                 ${DbSchemaV2.CanonicalWork.TITLE} ASC
        """
    )
    fun observeStudioWorksForArtist(
        artistId: String,
        formats: List<String>,
        releaseType: String,
    ): Flow<List<CanonicalWorkSummary>>

    @Query(
        """
        SELECT COUNT(*)
        FROM ${DbSchemaV2.CanonicalWork.TABLE}
        WHERE ${DbSchemaV2.CanonicalWork.ARTIST_ID} = :artistId
          AND ${DbSchemaV2.CanonicalWork.FORMAT_TYPE} IN (:formats)
          AND ${DbSchemaV2.CanonicalWork.RELEASE_TYPE} = :releaseType
        """
    )
    fun observeStudioTotalCount(
        artistId: String,
        formats: List<String>,
        releaseType: String,
    ): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT w.${DbSchemaV2.Work.ID})
        FROM ${DbSchemaV2.Work.TABLE} w
        INNER JOIN ${DbSchemaV2.CanonicalWork.TABLE} cw
          ON cw.${DbSchemaV2.CanonicalWork.ID} = w.${DbSchemaV2.Work.CANONICAL_WORK_ID}
        WHERE cw.${DbSchemaV2.CanonicalWork.ARTIST_ID} = :artistId
          AND cw.${DbSchemaV2.CanonicalWork.FORMAT_TYPE} IN (:formats)
          AND cw.${DbSchemaV2.CanonicalWork.RELEASE_TYPE} = :releaseType
        """
    )
    fun observeOwnedStudioCount(
        artistId: String,
        formats: List<String>,
        releaseType: String,
    ): Flow<Int>

    @Query(
        """
        SELECT cw.${DbSchemaV2.CanonicalWork.ID} AS id,
               cw.${DbSchemaV2.CanonicalWork.TITLE} AS title,
               cw.${DbSchemaV2.CanonicalWork.YEAR} AS year,
               cw.${DbSchemaV2.CanonicalWork.FORMAT_TYPE} AS formatType
        FROM ${DbSchemaV2.CanonicalWork.TABLE} cw
        LEFT JOIN ${DbSchemaV2.Work.TABLE} w
          ON w.${DbSchemaV2.Work.CANONICAL_WORK_ID} = cw.${DbSchemaV2.CanonicalWork.ID}
        WHERE cw.${DbSchemaV2.CanonicalWork.ARTIST_ID} = :artistId
          AND cw.${DbSchemaV2.CanonicalWork.FORMAT_TYPE} IN (:formats)
          AND cw.${DbSchemaV2.CanonicalWork.RELEASE_TYPE} = :releaseType
          AND w.${DbSchemaV2.Work.ID} IS NULL
        ORDER BY cw.${DbSchemaV2.CanonicalWork.YEAR} IS NULL,
                 cw.${DbSchemaV2.CanonicalWork.YEAR} ASC,
                 cw.${DbSchemaV2.CanonicalWork.TITLE} ASC
        """
    )
    fun observeMissingStudioWorksForArtist(
        artistId: String,
        formats: List<String>,
        releaseType: String,
    ): Flow<List<CanonicalWorkSummary>>
}
