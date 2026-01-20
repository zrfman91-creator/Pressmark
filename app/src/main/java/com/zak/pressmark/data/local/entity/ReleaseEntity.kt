// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/ReleaseEntity.kt
package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema.Release

/**
 * Top-level entity (Release-first model).
 *
 * A Release is the thing you own/list/scan (pressing/edition specific).
 *
 * NOTE: In this step we keep fields as nullable Strings/Ints where appropriate to
 * avoid introducing new Room TypeConverters. We can tighten this later (e.g. enums).
 */
@Entity(
    tableName = Release.TABLE,
    indices = [
        Index(value = [Release.TITLE]),
        Index(value = [Release.BARCODE]),
        Index(value = [Release.CATALOG_NO]),
        Index(value = [Release.DISCOGS_RELEASE_ID]),
    ],
)
data class ReleaseEntity(
    @PrimaryKey
    @ColumnInfo(name = Release.ID) val id: String,

    @ColumnInfo(name = Release.TITLE) val title: String,

    @ColumnInfo(name = Release.RELEASE_YEAR) val releaseYear: Int? = null,
    @ColumnInfo(name = Release.GENRE) val genre: String? = null,

    // Pressing / identifiers
    @ColumnInfo(name = Release.LABEL) val label: String? = null,
    @ColumnInfo(name = Release.CATALOG_NO) val catalogNo: String? = null,
    @ColumnInfo(name = Release.BARCODE) val barcode: String? = null,
    @ColumnInfo(name = Release.COUNTRY) val country: String? = null,

    // Classification
    // Examples: "STUDIO", "LIVE", "COMPILATION", "SOUNDTRACK", "GREATEST_HITS"
    @ColumnInfo(name = Release.RELEASE_TYPE) val releaseType: String? = null,

    // Examples: "LP", "EP", "7IN", "12IN", "CD", "CASSETTE"
    @ColumnInfo(name = Release.FORMAT) val format: String? = null,

    // External ids
    @ColumnInfo(name = Release.DISCOGS_RELEASE_ID) val discogsReleaseId: Long? = null,
    @ColumnInfo(name = Release.MASTER_ID) val masterId: Long? = null,

    // Canonical artwork provider fields (optional, but aligns with your existing pattern)
    @ColumnInfo(name = Release.ARTWORK_PROVIDER) val artworkProvider: String? = null,
    @ColumnInfo(name = Release.ARTWORK_PROVIDER_ITEM_ID) val artworkProviderItemId: String? = null,

    // User metadata (keep parity with AlbumEntity)
    @ColumnInfo(name = Release.NOTES) val notes: String? = null,
    @ColumnInfo(name = Release.RATING) val rating: Int? = null,
    @ColumnInfo(name = Release.ADDED_AT) val addedAt: Long,
    @ColumnInfo(name = Release.LAST_PLAYED_AT) val lastPlayedAt: Long? = null,
)