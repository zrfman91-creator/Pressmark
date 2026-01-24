package com.zak.pressmark.data.local.entity.v1

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only verification/audit trail.
 */
@Entity(
    tableName = "verification_events",
    indices = [
        Index(value = ["catalog_item_id"]),
        Index(value = ["event_type"]),
        Index(value = ["created_at"])
    ]
)
data class VerificationEventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "catalog_item_id")
    val catalogItemId: Long,

    @ColumnInfo(name = "event_type")
    val eventType: VerificationEventType,

    @ColumnInfo(name = "provider")
    val provider: Provider? = null,

    @ColumnInfo(name = "provider_item_id")
    val providerItemId: String? = null,

    @ColumnInfo(name = "previous_release_id")
    val previousReleaseId: Long? = null,

    @ColumnInfo(name = "new_release_id")
    val newReleaseId: Long? = null,

    /**
     * Store reasons as JSON (or a compact string) for now.
     * This is intentionally flexible to avoid migrations during experimentation.
     */
    @ColumnInfo(name = "reasons_json")
    val reasonsJson: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
