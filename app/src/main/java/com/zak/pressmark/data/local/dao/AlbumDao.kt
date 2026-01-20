// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/AlbumDao.kt
package com.zak.pressmark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zak.pressmark.data.local.db.DbSchema.Album
import com.zak.pressmark.data.local.db.DbSchema.Artist
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.local.entity.AlbumGenreCrossRef
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    // ==========================================================
    // Canonical reads (UI should use these)
    // ==========================================================

    @Query(
        """
        SELECT 
            ${Album.TABLE}.*,
            ${Artist.TABLE}.${Artist.DISPLAY_NAME} AS artistDisplayName,
            ${Artist.TABLE}.${Artist.SORT_NAME} AS artistSortName
        FROM ${Album.TABLE}
        LEFT JOIN ${Artist.TABLE} 
            ON ${Artist.TABLE}.${Artist.ID} = ${Album.TABLE}.${Album.ARTIST_ID}
        ORDER BY 
            COALESCE(${Artist.TABLE}.${Artist.SORT_NAME}, ${Artist.TABLE}.${Artist.DISPLAY_NAME}, '') COLLATE NOCASE,
            ${Album.TABLE}.${Album.TITLE} COLLATE NOCASE
        """
    )
    fun observeAllWithArtist(): Flow<List<AlbumWithArtistName>>

    @Query(
        """
        SELECT 
            ${Album.TABLE}.*,
            ${Artist.TABLE}.${Artist.DISPLAY_NAME} AS artistDisplayName,
            ${Artist.TABLE}.${Artist.SORT_NAME} AS artistSortName
        FROM ${Album.TABLE}
        LEFT JOIN ${Artist.TABLE} 
            ON ${Artist.TABLE}.${Artist.ID} = ${Album.TABLE}.${Album.ARTIST_ID}
        WHERE ${Album.TABLE}.${Album.ID} = :id
        LIMIT 1
        """
    )
    fun observeByIdWithArtist(id: String): Flow<AlbumWithArtistName?>

    @Query(
        """
        SELECT 
            ${Album.TABLE}.*,
            ${Artist.TABLE}.${Artist.DISPLAY_NAME} AS artistDisplayName,
            ${Artist.TABLE}.${Artist.SORT_NAME} AS artistSortName
        FROM ${Album.TABLE}
        LEFT JOIN ${Artist.TABLE} 
            ON ${Artist.TABLE}.${Artist.ID} = ${Album.TABLE}.${Album.ARTIST_ID}
        WHERE ${Album.TABLE}.${Album.ID} = :id
        LIMIT 1
        """
    )
    suspend fun getByIdWithArtist(id: String): AlbumWithArtistName?

    @Query(
        """
        SELECT 
            ${Album.TABLE}.*,
            ${Artist.TABLE}.${Artist.DISPLAY_NAME} AS artistDisplayName,
            ${Artist.TABLE}.${Artist.SORT_NAME} AS artistSortName
        FROM ${Album.TABLE}
        LEFT JOIN ${Artist.TABLE} 
            ON ${Artist.TABLE}.${Artist.ID} = ${Album.TABLE}.${Album.ARTIST_ID}
        WHERE ${Album.TABLE}.${Album.ARTIST_ID} = :artistId
        ORDER BY ${Album.TABLE}.${Album.RELEASE_YEAR} IS NULL, ${Album.TABLE}.${Album.RELEASE_YEAR}, ${Album.TABLE}.${Album.TITLE} COLLATE NOCASE
        """
    )
    fun observeByArtistIdWithArtist(artistId: Long): Flow<List<AlbumWithArtistName>>

    // ==========================================================
    // Legacy reads (keep temporarily for older UI code)
    // ==========================================================

    @Query(
        """
        SELECT ${Album.TABLE}.*
        FROM ${Album.TABLE}
        ORDER BY ${Album.TABLE}.${Album.TITLE} COLLATE NOCASE
        """
    )
    fun observeAll(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM ${Album.TABLE} WHERE ${Album.ID} = :id LIMIT 1")
    fun observeById(id: String): Flow<AlbumEntity?>

    @Query("SELECT * FROM ${Album.TABLE} WHERE ${Album.ID} = :id LIMIT 1")
    suspend fun getById(id: String): AlbumEntity?

    @Query(
        """
        SELECT * FROM ${Album.TABLE}
        WHERE ${Album.ARTIST_ID} = :artistId
        ORDER BY ${Album.RELEASE_YEAR} IS NULL, ${Album.RELEASE_YEAR}, ${Album.TITLE} COLLATE NOCASE
        """
    )
    fun observeByArtistId(artistId: Long): Flow<List<AlbumEntity>>

    // ==========================================================
    // Writes
    // ==========================================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(album: AlbumEntity)

    @Update
    suspend fun update(album: AlbumEntity)

    @Delete
    suspend fun delete(album: AlbumEntity)

    @Query(
        """
        SELECT * FROM ${Album.TABLE}
        WHERE ${Album.LABEL} IS NOT NULL AND ${Album.CATALOG_NO} IS NOT NULL
          AND LOWER(TRIM(${Album.LABEL})) = LOWER(TRIM(:label))
          AND LOWER(TRIM(${Album.CATALOG_NO})) = LOWER(TRIM(:catalogNo))
        LIMIT 1
        """
    )
    suspend fun findByLabelAndCatalogNo(label: String, catalogNo: String): AlbumEntity?

    @Query(
        """
    UPDATE ${Album.TABLE}
    SET ${Album.COVER_URI} = :coverUri,
        ${Album.DISCOGS_RELEASE_ID} = :discogsReleaseId,
        ${Album.ARTWORK_PROVIDER} = :artworkProvider,
        ${Album.ARTWORK_PROVIDER_ITEM_ID} = :artworkProviderItemId
    WHERE ${Album.ID} = :id
    """
    )
    suspend fun updateCover(
        id: String,
        coverUri: String?,
        discogsReleaseId: Long?,
        artworkProvider: String?,
        artworkProviderItemId: String?,
    ): Int

    @Query(
        """
    UPDATE ${com.zak.pressmark.data.local.db.DbSchema.Album.TABLE}
    SET ${com.zak.pressmark.data.local.db.DbSchema.Album.ARTWORK_PROVIDER} = 'discogs',
        ${com.zak.pressmark.data.local.db.DbSchema.Album.ARTWORK_PROVIDER_ITEM_ID} =
            CAST(${com.zak.pressmark.data.local.db.DbSchema.Album.DISCOGS_RELEASE_ID} AS TEXT)
    WHERE ${com.zak.pressmark.data.local.db.DbSchema.Album.ARTWORK_PROVIDER} IS NULL
      AND ${com.zak.pressmark.data.local.db.DbSchema.Album.DISCOGS_RELEASE_ID} IS NOT NULL
    """
    )
    suspend fun backfillArtworkProviderFromLegacyDiscogs(): Int

    @Query(
        """
    UPDATE ${Album.TABLE}
    SET
        ${Album.RELEASE_YEAR} =
            CASE WHEN ${Album.RELEASE_YEAR} IS NULL THEN :releaseYear ELSE ${Album.RELEASE_YEAR} END,
        ${Album.CATALOG_NO} =
            CASE WHEN ${Album.CATALOG_NO} IS NULL OR ${Album.CATALOG_NO} = '' THEN :catalogNo ELSE ${Album.CATALOG_NO} END,
        ${Album.LABEL} =
            CASE WHEN ${Album.LABEL} IS NULL OR ${Album.LABEL} = '' THEN :label ELSE ${Album.LABEL} END,
        ${Album.FORMAT} =
            CASE WHEN ${Album.FORMAT} IS NULL OR ${Album.FORMAT} = '' THEN :format ELSE ${Album.FORMAT} END
    WHERE ${Album.ID} = :id
    """
    )
    suspend fun fillMissingFields(
        id: String,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkGenresToAlbum(joins: List<AlbumGenreCrossRef>)

    @Query("DELETE FROM album_genre_cross_ref WHERE albumId = :albumId")
    suspend fun clearAlbumGenres(albumId: String)
}
