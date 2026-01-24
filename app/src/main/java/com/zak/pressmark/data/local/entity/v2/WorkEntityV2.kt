// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/v2/WorkEntityV2.kt
package com.zak.pressmark.data.local.entity.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.v2.DbSchemaV2

@Entity(
    tableName = DbSchemaV2.Work.TABLE,
    indices = [
        Index(value = [DbSchemaV2.Work.TITLE_NORMALIZED]),
        Index(value = [DbSchemaV2.Work.ARTIST_NORMALIZED]),
        Index(value = [DbSchemaV2.Work.YEAR]),
        Index(value = [DbSchemaV2.Work.UPDATED_AT]),
        Index(value = [DbSchemaV2.Work.DISCOGS_MASTER_ID]),
        Index(value = [DbSchemaV2.Work.MUSICBRAINZ_RELEASE_GROUP_ID]),
    ],
)
data class WorkEntityV2(
    @PrimaryKey
    @ColumnInfo(name = DbSchemaV2.Work.ID) val id: String,

    @ColumnInfo(name = DbSchemaV2.Work.TITLE) val title: String,
    @ColumnInfo(name = DbSchemaV2.Work.TITLE_NORMALIZED) val titleNormalized: String,

    @ColumnInfo(name = DbSchemaV2.Work.ARTIST_LINE) val artistLine: String,
    @ColumnInfo(name = DbSchemaV2.Work.ARTIST_NORMALIZED) val artistNormalized: String,

    @ColumnInfo(name = DbSchemaV2.Work.YEAR) val year: Int? = null,

    // Store as JSON string until we standardize list converters for V2.
    @ColumnInfo(name = DbSchemaV2.Work.GENRES_JSON) val genresJson: String = "[]",
    @ColumnInfo(name = DbSchemaV2.Work.STYLES_JSON) val stylesJson: String = "[]",

    @ColumnInfo(name = DbSchemaV2.Work.PRIMARY_ARTWORK_URI) val primaryArtworkUri: String? = null,

    // External anchors (nullable, provider-specific)
    @ColumnInfo(name = DbSchemaV2.Work.DISCOGS_MASTER_ID) val discogsMasterId: Long? = null,
    @ColumnInfo(name = DbSchemaV2.Work.MUSICBRAINZ_RELEASE_GROUP_ID) val musicBrainzReleaseGroupId: String? = null,

    @ColumnInfo(name = DbSchemaV2.Work.CREATED_AT) val createdAt: Long,
    @ColumnInfo(name = DbSchemaV2.Work.UPDATED_AT) val updatedAt: Long,
)
