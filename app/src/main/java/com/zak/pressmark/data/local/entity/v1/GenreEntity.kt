// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/GenreEntity.kt
package com.zak.pressmark.data.local.entity.v1

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "genres", indices = [Index(value = ["name"], unique = true)])
data class GenreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
