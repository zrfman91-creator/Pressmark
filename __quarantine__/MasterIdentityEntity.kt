package com.zak.pressmark.data.local.entity.v1

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Master-first identity shared metadata + artwork.
 * Unique by (provider, master_id).
 */
@Entity(
    tableName = "master_identities",
    indices = [
        Index(value = ["provider", "provider_master_id"], unique = true),
        Index(value = ["title_sort"]),
        Index(value = ["artist_sort"])
    ]
)
data class MasterIdentityEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "provider")
    val provider: Provider,

    @ColumnInfo(name = "provider_master_id")
    val providerMasterId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist_line")
    val artistLine: String,

    @ColumnInfo(name = "year")
    val year: Int? = null,

    /**
     * Store list-ish fields as JSON until a richer model is necessary.
     * Keep DB additive and avoid premature join explosion.
     */
    @ColumnInfo(name = "genres_json")
    val genresJson: String? = null,

    @ColumnInfo(name = "styles_json")
    val stylesJson: String? = null,

    @ColumnInfo(name = "artwork_uri")
    val artworkUri: String? = null,

    @ColumnInfo(name = "raw_json")
    val rawJson: String? = null,

    // Sort keys (normalized)
    @ColumnInfo(name = "title_sort")
    val titleSort: String,

    @ColumnInfo(name = "artist_sort")
    val artistSort: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
