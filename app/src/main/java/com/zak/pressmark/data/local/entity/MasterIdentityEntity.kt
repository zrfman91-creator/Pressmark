package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema.MasterIdentity

@Entity(
    tableName = MasterIdentity.TABLE,
    indices = [
        Index(value = [MasterIdentity.PROVIDER, MasterIdentity.MASTER_ID], unique = true),
    ],
)
data class MasterIdentityEntity(
    @PrimaryKey
    @ColumnInfo(name = MasterIdentity.ID) val id: String,
    @ColumnInfo(name = MasterIdentity.PROVIDER) val provider: String,
    @ColumnInfo(name = MasterIdentity.MASTER_ID) val masterId: String?,
    @ColumnInfo(name = MasterIdentity.TITLE) val title: String,
    @ColumnInfo(name = MasterIdentity.ARTIST_LINE) val artistLine: String,
    @ColumnInfo(name = MasterIdentity.YEAR) val year: Int?,
    @ColumnInfo(name = MasterIdentity.GENRES) val genres: String?,
    @ColumnInfo(name = MasterIdentity.STYLES) val styles: String?,
    @ColumnInfo(name = MasterIdentity.ARTWORK_URI) val artworkUri: String?,
    @ColumnInfo(name = MasterIdentity.RAW_JSON) val rawJson: String?,
    @ColumnInfo(name = MasterIdentity.CREATED_AT) val createdAt: Long,
)
