package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema.Album
import com.zak.pressmark.data.local.db.DbSchema.Artist

@Entity(
    tableName = Album.TABLE,
    indices = [
        Index(value = [Album.ARTIST_ID], name = "index_album_artist_id"),
        Index(value = ["artist_id", "title"], name = "index_albums_artist_title"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = [Artist.ID],
            childColumns = [Album.ARTIST_ID],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class AlbumEntity(
    @PrimaryKey
    @ColumnInfo(name = Album.ID) val id: String,
    @ColumnInfo(name = Album.TITLE) val title: String,

    // ✅ Must be nullable if FK uses SET_NULL
    @ColumnInfo(name = Album.ARTIST_ID) val artistId: Long?,

    @ColumnInfo(name = Album.RELEASE_YEAR) val releaseYear: Int? = null,
    @ColumnInfo(name = Album.CATALOG_NO) val catalogNo: String? = null,
    @ColumnInfo(name = Album.LABEL) val label: String? = null,
    @ColumnInfo(name = Album.COVER_URI) val coverUri: String? = null,
    @ColumnInfo(name = Album.DISCOGS_RELEASE_ID) val discogsReleaseId: Long? = null,
    @ColumnInfo(name = Album.GENRE) val genre: String? = null,
    @ColumnInfo(name = Album.STYLES) val styles: String? = null,
    @ColumnInfo(name = Album.NOTES) val notes: String? = null,
    @ColumnInfo(name = Album.TRACKLIST) val tracklist: String? = null,
    @ColumnInfo(name = Album.RATING) val rating: Int? = null,
    @ColumnInfo(name = Album.ADDED_AT) val addedAt: Long,
    @ColumnInfo(name = Album.LAST_PLAYED_AT) val lastPlayedAt: Long? = null,
    @ColumnInfo(name = Album.MASTER_ID) val masterId: Long? = null,
    @ColumnInfo(name = Album.FORMAT) val format: String? = null,
) {
    @get:Ignore
    val persistedArtworkUrl: String?
        get() = coverUri?.trim()?.takeIf { it.isNotBlank() }

    @get:Ignore
    val hasPersistedArtwork: Boolean
        get() = !persistedArtworkUrl.isNullOrBlank()
}
