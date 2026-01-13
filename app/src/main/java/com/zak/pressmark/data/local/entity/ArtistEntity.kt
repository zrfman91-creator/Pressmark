// file: app/src/main/java/com/zak/pressmark/data/local/entity/ArtistEntity.kt
package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema.Artist // Use new constant
import java.util.Locale

@Entity(
    tableName = Artist.TABLE, // Use new constant
    indices = [
        Index(value = [Artist.NAME_NORMALIZED], unique = true),
    ],
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Artist.ID)
    val id: Long = 0L,

    @ColumnInfo(name = Artist.DISPLAY_NAME)
    val displayName: String,

    @ColumnInfo(name = Artist.SORT_NAME)
    val sortName: String = displayName,

    @ColumnInfo(name = Artist.NAME_NORMALIZED)
    val nameNormalized: String,

    @ColumnInfo(name = Artist.ARTIST_TYPE)
    val artistType: String = ArtistType.BAND.name,
)
enum class ArtistType {
    PERSON,
    BAND,
}

fun canonicalDisplayName(raw: String): String =
    raw.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ")

fun normalizeArtistName(raw: String): String =
    canonicalDisplayName(raw).lowercase(Locale.US)
