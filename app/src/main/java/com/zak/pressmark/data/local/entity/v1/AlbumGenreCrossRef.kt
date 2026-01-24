// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/AlbumGenreCrossRef.kt
package com.zak.pressmark.data.local.entity.v1

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

// New file: AlbumGenreCrossRef.kt
@Entity(
    tableName = "album_genre_cross_ref",
    primaryKeys = ["albumId", "genreId"],
    foreignKeys = [
        ForeignKey(entity = AlbumEntity::class, parentColumns = ["id"], childColumns = ["albumId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GenreEntity::class, parentColumns = ["id"], childColumns = ["genreId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class AlbumGenreCrossRef(
    val albumId: String,
    @ColumnInfo(index = true) val genreId: Long,
)
