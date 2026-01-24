package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema.EvidenceArtifact

@Entity(
    tableName = EvidenceArtifact.TABLE,
    indices = [
        Index(value = [EvidenceArtifact.CATALOG_ITEM_ID]),
        Index(value = [EvidenceArtifact.TYPE]),
    ],
)
data class EvidenceArtifactEntity(
    @PrimaryKey
    @ColumnInfo(name = EvidenceArtifact.ID) val id: String,
    @ColumnInfo(name = EvidenceArtifact.CATALOG_ITEM_ID) val catalogItemId: String,
    @ColumnInfo(name = EvidenceArtifact.TYPE) val type: String,
    @ColumnInfo(name = EvidenceArtifact.RAW_VALUE) val rawValue: String?,
    @ColumnInfo(name = EvidenceArtifact.NORMALIZED_VALUE) val normalizedValue: String?,
    @ColumnInfo(name = EvidenceArtifact.SOURCE) val source: String?,
    @ColumnInfo(name = EvidenceArtifact.CONFIDENCE) val confidence: Int?,
    @ColumnInfo(name = EvidenceArtifact.PHOTO_URI) val photoUri: String?,
    @ColumnInfo(name = EvidenceArtifact.CREATED_AT) val createdAt: Long,
)
