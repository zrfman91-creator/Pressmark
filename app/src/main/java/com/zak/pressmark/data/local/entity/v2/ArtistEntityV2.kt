package com.zak.pressmark.data.local.entity.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.v2.DbSchemaV2

@Entity(
    tableName = DbSchemaV2.Artist.TABLE,
    indices = [
        Index(value = [DbSchemaV2.Artist.NAME_NORMALIZED], unique = true),
        Index(value = [DbSchemaV2.Artist.DISCOGS_ARTIST_ID]),
    ],
)
data class ArtistEntityV2(
    @PrimaryKey
    @ColumnInfo(name = DbSchemaV2.Artist.ID) val id: String,

    @ColumnInfo(name = DbSchemaV2.Artist.NAME) val name: String,
    @ColumnInfo(name = DbSchemaV2.Artist.NAME_NORMALIZED) val nameNormalized: String,
    @ColumnInfo(name = DbSchemaV2.Artist.DISCOGS_ARTIST_ID) val discogsArtistId: Long? = null,

    @ColumnInfo(name = DbSchemaV2.Artist.CREATED_AT) val createdAt: Long,
    @ColumnInfo(name = DbSchemaV2.Artist.UPDATED_AT) val updatedAt: Long,
)
