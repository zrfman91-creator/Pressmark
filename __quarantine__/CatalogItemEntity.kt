package com.zak.pressmark.data.local.entity.v1

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Top-level, master-first catalog record.
 * This becomes the primary list/detail anchor after cutover.
 */
@Entity(
    tableName = "catalog_items",
    indices = [
        Index(value = ["master_identity_id"]),
        Index(value = ["state"]),
        Index(value = ["display_title_sort"]),
        Index(value = ["display_artist_sort"]),
        Index(value = ["created_at"])
    ]
)
data class CatalogItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "master_identity_id")
    val masterIdentityId: Long?,

    @ColumnInfo(name = "display_title")
    val displayTitle: String,

    @ColumnInfo(name = "display_artist_line")
    val displayArtistLine: String,

    @ColumnInfo(name = "primary_artwork_uri")
    val primaryArtworkUri: String? = null,

    @ColumnInfo(name = "state")
    val state: CatalogItemState = CatalogItemState.MASTER_ONLY,

    // Sort keys (normalized)
    @ColumnInfo(name = "display_title_sort")
    val displayTitleSort: String,

    @ColumnInfo(name = "display_artist_sort")
    val displayArtistSort: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
