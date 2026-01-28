// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/v2/WorkStyleCrossRefEntityV2.kt
package com.zak.pressmark.data.local.entity.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.zak.pressmark.data.local.db.v2.DbSchemaV2

@Entity(
    tableName = DbSchemaV2.WorkStyle.TABLE,
    primaryKeys = [DbSchemaV2.WorkStyle.WORK_ID, DbSchemaV2.WorkStyle.STYLE_ID],
    foreignKeys = [
        ForeignKey(
            entity = WorkEntityV2::class,
            parentColumns = [DbSchemaV2.Work.ID],
            childColumns = [DbSchemaV2.WorkStyle.WORK_ID],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StyleEntityV2::class,
            parentColumns = [DbSchemaV2.Style.ID],
            childColumns = [DbSchemaV2.WorkStyle.STYLE_ID],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = [DbSchemaV2.WorkStyle.WORK_ID]),
        Index(value = [DbSchemaV2.WorkStyle.STYLE_ID]),
    ],
)
data class WorkStyleCrossRefEntityV2(
    @ColumnInfo(name = DbSchemaV2.WorkStyle.WORK_ID) val workId: String,
    @ColumnInfo(name = DbSchemaV2.WorkStyle.STYLE_ID) val styleId: Long,
)
