// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/v2/GenreEntityV2.kt
package com.zak.pressmark.data.local.entity.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.v2.DbSchemaV2

@Entity(
    tableName = DbSchemaV2.Genre.TABLE,
    indices = [
        Index(value = [DbSchemaV2.Genre.NAME_NORMALIZED], unique = true),
    ],
)
data class GenreEntityV2(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DbSchemaV2.Genre.ID) val id: Long = 0,
    @ColumnInfo(name = DbSchemaV2.Genre.NAME_NORMALIZED) val nameNormalized: String,
    @ColumnInfo(name = DbSchemaV2.Genre.NAME_DISPLAY) val nameDisplay: String,
)
