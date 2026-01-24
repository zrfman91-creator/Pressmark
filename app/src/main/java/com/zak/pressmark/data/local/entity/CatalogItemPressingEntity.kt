package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema.CatalogItemPressing

@Entity(
    tableName = CatalogItemPressing.TABLE,
    indices = [
        Index(value = [CatalogItemPressing.CATALOG_ITEM_ID]),
        Index(value = [CatalogItemPressing.RELEASE_ID]),
    ],
)
data class CatalogItemPressingEntity(
    @PrimaryKey
    @ColumnInfo(name = CatalogItemPressing.ID) val id: String,
    @ColumnInfo(name = CatalogItemPressing.CATALOG_ITEM_ID) val catalogItemId: String,
    @ColumnInfo(name = CatalogItemPressing.RELEASE_ID) val releaseId: String?,
    @ColumnInfo(name = CatalogItemPressing.EVIDENCE_SCORE) val evidenceScore: Int?,
    @ColumnInfo(name = CatalogItemPressing.CREATED_AT) val createdAt: Long,
)
