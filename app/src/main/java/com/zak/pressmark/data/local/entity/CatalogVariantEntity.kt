package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema.CatalogVariant

@Entity(
    tableName = CatalogVariant.TABLE,
    indices = [
        Index(value = [CatalogVariant.CATALOG_ITEM_ID]),
        Index(value = [CatalogVariant.PRESSING_ID]),
    ],
)
data class CatalogVariantEntity(
    @PrimaryKey
    @ColumnInfo(name = CatalogVariant.ID) val id: String,
    @ColumnInfo(name = CatalogVariant.CATALOG_ITEM_ID) val catalogItemId: String,
    @ColumnInfo(name = CatalogVariant.PRESSING_ID) val pressingId: String,
    @ColumnInfo(name = CatalogVariant.VARIANT_KEY) val variantKey: String,
    @ColumnInfo(name = CatalogVariant.NOTES) val notes: String?,
    @ColumnInfo(name = CatalogVariant.CREATED_AT) val createdAt: Long,
)
