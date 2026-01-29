package com.zak.pressmark.data.local.entity.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.v2.DbSchemaV2

@Entity(
    tableName = DbSchemaV2.CanonicalWork.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = ArtistEntityV2::class,
            parentColumns = [DbSchemaV2.Artist.ID],
            childColumns = [DbSchemaV2.CanonicalWork.ARTIST_ID],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = [DbSchemaV2.CanonicalWork.ARTIST_ID]),
        Index(value = [DbSchemaV2.CanonicalWork.TITLE_NORMALIZED]),
        Index(value = [DbSchemaV2.CanonicalWork.YEAR]),
        Index(value = [DbSchemaV2.CanonicalWork.DISCOGS_MASTER_ID]),
    ],
)
data class CanonicalWorkEntityV2(
    @PrimaryKey
    @ColumnInfo(name = DbSchemaV2.CanonicalWork.ID) val id: String,

    @ColumnInfo(name = DbSchemaV2.CanonicalWork.ARTIST_ID) val artistId: String,

    @ColumnInfo(name = DbSchemaV2.CanonicalWork.TITLE) val title: String,
    @ColumnInfo(name = DbSchemaV2.CanonicalWork.TITLE_NORMALIZED) val titleNormalized: String,

    @ColumnInfo(name = DbSchemaV2.CanonicalWork.YEAR) val year: Int? = null,
    @ColumnInfo(name = DbSchemaV2.CanonicalWork.FORMAT_TYPE) val formatType: String? = null,
    @ColumnInfo(name = DbSchemaV2.CanonicalWork.RELEASE_TYPE) val releaseType: String? = null,

    @ColumnInfo(name = DbSchemaV2.CanonicalWork.DISCOGS_MASTER_ID) val discogsMasterId: Long? = null,

    @ColumnInfo(name = DbSchemaV2.CanonicalWork.CREATED_AT) val createdAt: Long,
    @ColumnInfo(name = DbSchemaV2.CanonicalWork.UPDATED_AT) val updatedAt: Long,
)
