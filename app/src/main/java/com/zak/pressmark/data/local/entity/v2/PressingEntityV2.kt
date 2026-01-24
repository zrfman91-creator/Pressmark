// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/v2/PressingEntityV2.kt
package com.zak.pressmark.data.local.entity.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.v2.DbSchemaV2

@Entity(
    tableName = DbSchemaV2.Pressing.TABLE,
    indices = [
        Index(value = [DbSchemaV2.Pressing.RELEASE_ID]),
        Index(value = [DbSchemaV2.Pressing.BARCODE_NORMALIZED]),
        Index(value = [DbSchemaV2.Pressing.DISCOGS_RELEASE_ID]),
        Index(value = [DbSchemaV2.Pressing.MUSICBRAINZ_RELEASE_ID]),
        Index(value = [DbSchemaV2.Pressing.UPDATED_AT]),
    ],
)
data class PressingEntityV2(
    @PrimaryKey
    @ColumnInfo(name = DbSchemaV2.Pressing.ID) val id: String,

    @ColumnInfo(name = DbSchemaV2.Pressing.RELEASE_ID) val releaseId: String,

    @ColumnInfo(name = DbSchemaV2.Pressing.BARCODE) val barcode: String? = null,
    @ColumnInfo(name = DbSchemaV2.Pressing.BARCODE_NORMALIZED) val barcodeNormalized: String? = null,

    // Store as JSON string until we standardize list converters for V2.
    @ColumnInfo(name = DbSchemaV2.Pressing.RUNOUTS_JSON) val runoutsJson: String = "[]",

    @ColumnInfo(name = DbSchemaV2.Pressing.PRESSING_PLANT) val pressingPlant: String? = null,

    // Optional duplication (pressing-specific or provider-specific differences)
    @ColumnInfo(name = DbSchemaV2.Pressing.LABEL) val label: String? = null,
    @ColumnInfo(name = DbSchemaV2.Pressing.CATALOG_NO) val catalogNo: String? = null,
    @ColumnInfo(name = DbSchemaV2.Pressing.COUNTRY) val country: String? = null,
    @ColumnInfo(name = DbSchemaV2.Pressing.FORMAT) val format: String? = null,
    @ColumnInfo(name = DbSchemaV2.Pressing.RELEASE_YEAR) val releaseYear: Int? = null,

    // Provider anchors
    @ColumnInfo(name = DbSchemaV2.Pressing.DISCOGS_RELEASE_ID) val discogsReleaseId: Long? = null,
    @ColumnInfo(name = DbSchemaV2.Pressing.MUSICBRAINZ_RELEASE_ID) val musicBrainzReleaseId: String? = null,

    @ColumnInfo(name = DbSchemaV2.Pressing.CREATED_AT) val createdAt: Long,
    @ColumnInfo(name = DbSchemaV2.Pressing.UPDATED_AT) val updatedAt: Long,
)
