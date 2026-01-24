package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted ingest evidence. Never deleted; used to refine/verify pressings later.
 */
@Entity(
    tableName = "evidence_artifacts",
    indices = [
        Index(value = ["catalog_item_id"]),
        Index(value = ["type"]),
        Index(value = ["normalized_value"])
    ]
)
data class EvidenceArtifactEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "catalog_item_id")
    val catalogItemId: Long,

    @ColumnInfo(name = "type")
    val type: EvidenceType,

    @ColumnInfo(name = "raw_value")
    val rawValue: String? = null,

    @ColumnInfo(name = "normalized_value")
    val normalizedValue: String? = null,

    @ColumnInfo(name = "source")
    val source: EvidenceSource,

    @ColumnInfo(name = "confidence")
    val confidence: Double = 0.0,

    /**
     * Optional local URI to the photo captured during ingest.
     * Can be null if you delete photos after commit; evidence still remains.
     */
    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
