// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/v2/WorkGenreCrossRefEntityV2.kt
package com.zak.pressmark.data.local.entity.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.zak.pressmark.data.local.db.v2.DbSchemaV2

@Entity(
    tableName = DbSchemaV2.WorkGenre.TABLE,
    primaryKeys = [DbSchemaV2.WorkGenre.WORK_ID, DbSchemaV2.WorkGenre.GENRE_ID],
    foreignKeys = [
        ForeignKey(
            entity = WorkEntityV2::class,
            parentColumns = [DbSchemaV2.Work.ID],
            childColumns = [DbSchemaV2.WorkGenre.WORK_ID],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GenreEntityV2::class,
            parentColumns = [DbSchemaV2.Genre.ID],
            childColumns = [DbSchemaV2.WorkGenre.GENRE_ID],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = [DbSchemaV2.WorkGenre.WORK_ID]),
        Index(value = [DbSchemaV2.WorkGenre.GENRE_ID]),
    ],
)
data class WorkGenreCrossRefEntityV2(
    @ColumnInfo(name = DbSchemaV2.WorkGenre.WORK_ID) val workId: String,
    @ColumnInfo(name = DbSchemaV2.WorkGenre.GENRE_ID) val genreId: Long,
)
