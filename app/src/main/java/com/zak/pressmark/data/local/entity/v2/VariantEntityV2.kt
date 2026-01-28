// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/v2/VariantEntityV2.kt
package com.zak.pressmark.data.local.entity.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.v2.DbSchemaV2

@Entity(
    tableName = DbSchemaV2.Variant.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = WorkEntityV2::class,
            parentColumns = [DbSchemaV2.Work.ID],
            childColumns = [DbSchemaV2.Variant.WORK_ID],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PressingEntityV2::class,
            parentColumns = [DbSchemaV2.Pressing.ID],
            childColumns = [DbSchemaV2.Variant.PRESSING_ID],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = [DbSchemaV2.Variant.WORK_ID]),
        Index(value = [DbSchemaV2.Variant.PRESSING_ID]),
        Index(value = [DbSchemaV2.Variant.VARIANT_KEY]),
    ],
)
data class VariantEntityV2(
    @PrimaryKey
    @ColumnInfo(name = DbSchemaV2.Variant.ID) val id: String,

    @ColumnInfo(name = DbSchemaV2.Variant.WORK_ID) val workId: String,
    @ColumnInfo(name = DbSchemaV2.Variant.PRESSING_ID) val pressingId: String,

    @ColumnInfo(name = DbSchemaV2.Variant.VARIANT_KEY) val variantKey: String,

    @ColumnInfo(name = DbSchemaV2.Variant.NOTES) val notes: String? = null,
    @ColumnInfo(name = DbSchemaV2.Variant.RATING) val rating: Int? = null,

    @ColumnInfo(name = DbSchemaV2.Variant.ADDED_AT) val addedAt: Long,
    @ColumnInfo(name = DbSchemaV2.Variant.LAST_PLAYED_AT) val lastPlayedAt: Long? = null,
)
