// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/AlbumEntity.kt
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
        Index(value = [Album.ARTIST_ID]),
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
    @ColumnInfo(name = Album.ARTIST_ID) val artistId: Long?,

    @ColumnInfo(name = Album.RELEASE_YEAR) val releaseYear: Int? = null,
    @ColumnInfo(name = Album.CATALOG_NO) val catalogNo: String? = null,
    @ColumnInfo(name = Album.LABEL) val label: String? = null,

    @ColumnInfo(name = Album.COVER_URI) val coverUri: String? = null,
    @ColumnInfo(name = Album.NOTES) val notes: String? = null,

    // Legacy (keep column for now)
    @ColumnInfo(name = Album.DISCOGS_RELEASE_ID) val discogsReleaseId: Long? = null,

    @ColumnInfo(name = Album.RATING) val rating: Int? = null,
    @ColumnInfo(name = Album.ADDED_AT) val addedAt: Long,
    @ColumnInfo(name = Album.LAST_PLAYED_AT) val lastPlayedAt: Long? = null,
    @ColumnInfo(name = Album.MASTER_ID) val masterId: Long? = null,
    @ColumnInfo(name = Album.FORMAT) val format: String? = null,

    // Canonical artwork provider fields
    @ColumnInfo(name = Album.ARTWORK_PROVIDER) val artworkProvider: String? = null,
    @ColumnInfo(name = Album.ARTWORK_PROVIDER_ITEM_ID) val artworkProviderItemId: String? = null,
) {
    @get:Ignore
    val persistedArtworkUrl: String?
        get() = coverUri?.trim()?.takeIf { it.isNotBlank() }

    @get:Ignore
    val hasPersistedArtwork: Boolean
        get() = !persistedArtworkUrl.isNullOrBlank()

    @get:Ignore
    val isDiscogsArtwork: Boolean
        get() = artworkProvider?.lowercase() == "discogs"

    /**
     * Canonical Discogs item id derived from provider fields.
     * Falls back to legacy discogsReleaseId for older rows.
     */
    @get:Ignore
    val discogsItemId: Long?
        get() = (if (isDiscogsArtwork) artworkProviderItemId else null)?.toLongOrNull()
            ?: discogsReleaseId

    @get:Ignore
    val artworkNotFound: Boolean
        get() = discogsItemId == -1L
}
