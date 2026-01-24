package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema.CatalogItem

@Entity(
    tableName = CatalogItem.TABLE,
    indices = [
        Index(value = [CatalogItem.MASTER_IDENTITY_ID]),
        Index(value = [CatalogItem.DISPLAY_TITLE]),
        Index(value = [CatalogItem.DISPLAY_ARTIST_LINE]),
    ],
)
data class CatalogItemEntity(
    @PrimaryKey
    @ColumnInfo(name = CatalogItem.ID) val id: String,
    @ColumnInfo(name = CatalogItem.MASTER_IDENTITY_ID) val masterIdentityId: String,
    @ColumnInfo(name = CatalogItem.DISPLAY_TITLE) val displayTitle: String,
    @ColumnInfo(name = CatalogItem.DISPLAY_ARTIST_LINE) val displayArtistLine: String,
    @ColumnInfo(name = CatalogItem.PRIMARY_ARTWORK_URI) val primaryArtworkUri: String?,
    @ColumnInfo(name = CatalogItem.RELEASE_YEAR) val releaseYear: Int?,
    @ColumnInfo(name = CatalogItem.STATE) val state: String,
    @ColumnInfo(name = CatalogItem.ADDED_AT) val addedAt: Long,
    @ColumnInfo(name = CatalogItem.UPDATED_AT) val updatedAt: Long,
)
