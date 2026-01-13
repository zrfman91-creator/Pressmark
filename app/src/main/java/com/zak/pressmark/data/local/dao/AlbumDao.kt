// file: app/src/main/java/com/zak/pressmark/data/local/dao/AlbumDao.kt
package com.zak.pressmark.data.local.dao

import androidx.room.*
import com.zak.pressmark.data.local.db.DbSchema.Album
import com.zak.pressmark.data.local.db.DbSchema.Artist
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.local.entity.AlbumGenreCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query(
        """
        SELECT ${Album.TABLE}.*
        FROM ${Album.TABLE}
        LEFT JOIN ${Artist.TABLE} ON ${Artist.TABLE}.${Artist.ID} = ${Album.TABLE}.${Album.ARTIST_ID}
        ORDER BY 
            COALESCE(${Artist.TABLE}.${Artist.SORT_NAME}, ${Album.TABLE}.${Album.ARTIST}) COLLATE NOCASE,
            ${Album.TABLE}.${Album.TITLE} COLLATE NOCASE
        """
    )
    fun observeAll(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM ${Album.TABLE} WHERE ${Album.ID} = :id LIMIT 1")
    fun observeById(id: String): Flow<AlbumEntity?>

    @Query("SELECT * FROM ${Album.TABLE} WHERE ${Album.ID} = :id LIMIT 1")
    suspend fun getById(id: String): AlbumEntity?

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
        SELECT * FROM ${Album.TABLE}
        WHERE ${Album.ARTIST_ID} = :artistId
        ORDER BY ${Album.RELEASE_YEAR} IS NULL, ${Album.RELEASE_YEAR}, ${Album.TITLE} COLLATE NOCASE
        """
    )
    fun observeByArtistId(artistId: Long): Flow<List<AlbumEntity>>

    @Query(
        """
        UPDATE ${Album.TABLE} 
        SET ${Album.COVER_URI} = :coverUri, ${Album.DISCOGS_RELEASE_ID} = :discogsReleaseId 
        WHERE ${Album.ID} = :id
        """
    )

    suspend fun updateCover(id: String, coverUri: String?, discogsReleaseId: Long?): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkGenresToAlbum(joins: List<AlbumGenreCrossRef>)

    @Query("DELETE FROM album_genre_cross_ref WHERE albumId = :albumId")
    suspend fun clearAlbumGenres(albumId: String)
}
