// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/v2/ReleaseEntityV2.kt
package com.zak.pressmark.data.local.entity.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.v2.DbSchemaV2

@Entity(
    tableName = DbSchemaV2.Release.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = WorkEntityV2::class,
            parentColumns = [DbSchemaV2.Work.ID],
            childColumns = [DbSchemaV2.Release.WORK_ID],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = [DbSchemaV2.Release.WORK_ID]),
        Index(value = [DbSchemaV2.Release.LABEL_NORMALIZED]),
        Index(value = [DbSchemaV2.Release.CATALOG_NO_NORMALIZED]),
        Index(value = [DbSchemaV2.Release.RELEASE_YEAR]),
    ],
)
data class ReleaseEntityV2(
    @PrimaryKey
    @ColumnInfo(name = DbSchemaV2.Release.ID) val id: String,

    @ColumnInfo(name = DbSchemaV2.Release.WORK_ID) val workId: String,

    @ColumnInfo(name = DbSchemaV2.Release.LABEL) val label: String? = null,
    @ColumnInfo(name = DbSchemaV2.Release.LABEL_NORMALIZED) val labelNormalized: String? = null,

    @ColumnInfo(name = DbSchemaV2.Release.CATALOG_NO) val catalogNo: String? = null,
    @ColumnInfo(name = DbSchemaV2.Release.CATALOG_NO_NORMALIZED) val catalogNoNormalized: String? = null,

    @ColumnInfo(name = DbSchemaV2.Release.COUNTRY) val country: String? = null,
    @ColumnInfo(name = DbSchemaV2.Release.FORMAT) val format: String? = null,
    @ColumnInfo(name = DbSchemaV2.Release.RELEASE_YEAR) val releaseYear: Int? = null,
    @ColumnInfo(name = DbSchemaV2.Release.RELEASE_TYPE) val releaseType: String? = null,

    @ColumnInfo(name = DbSchemaV2.Release.CREATED_AT) val createdAt: Long,
    @ColumnInfo(name = DbSchemaV2.Release.UPDATED_AT) val updatedAt: Long,
)
