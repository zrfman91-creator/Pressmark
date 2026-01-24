package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Visual/physical variants of a pressing (cover swaps, hype stickers, colored vinyl, etc.).
 */
@Entity(
    tableName = "catalog_variants",
    indices = [
        Index(value = ["catalog_item_id"]),
        Index(value = ["pressing_id"]),
        Index(value = ["variant_key"])
    ]
)
data class CatalogVariantEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "catalog_item_id")
    val catalogItemId: Long,

    @ColumnInfo(name = "pressing_id")
    val pressingId: Long,

    @ColumnInfo(name = "variant_key")
    val variantKey: String,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "artwork_uri")
    val artworkUri: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
