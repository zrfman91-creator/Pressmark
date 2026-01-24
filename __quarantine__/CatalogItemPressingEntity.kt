package com.zak.pressmark.data.local.entity.v1

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Owned pressing record (may be unconfirmed initially).
 *
 * release_id is nullable until confirmed.
 * provider_release_id is stored so we can reference provider candidates even before linking an internal Release row.
 */
@Entity(
    tableName = "catalog_item_pressings",
    indices = [
        Index(value = ["catalog_item_id"]),
        Index(value = ["confirmed_release_id"]),
        Index(value = ["provider", "provider_release_id"])
    ]
)
data class CatalogItemPressingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "catalog_item_id")
    val catalogItemId: Long,

    /**
     * Internal Release row id when confirmed (nullable until link).
     * Keep this even if Release-first is decommissioned later; it remains a traceable compatibility edge.
     */
    @ColumnInfo(name = "confirmed_release_id")
    val confirmedReleaseId: Long? = null,

    @ColumnInfo(name = "provider")
    val provider: Provider? = null,

    @ColumnInfo(name = "provider_release_id")
    val providerReleaseId: String? = null,

    @ColumnInfo(name = "evidence_score")
    val evidenceScore: Double = 0.0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
