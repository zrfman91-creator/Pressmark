// FILE: app/src/main/java/com/zak/pressmark/data/local/model/AlbumWithArtistName.kt
package com.zak.pressmark.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.zak.pressmark.data.local.entity.AlbumEntity

/**
 * Room JOIN result:
 * - album columns come from AlbumEntity (@Embedded)
 * - artistDisplayName / artistSortName are projected aliases from AlbumDao queries
 */
data class AlbumWithArtistName(
    @Embedded
    val album: AlbumEntity,

    @ColumnInfo(name = "artistDisplayName")
    val artistDisplayName: String?,

    @ColumnInfo(name = "artistSortName")
    val artistSortName: String?,
) {
    val artist: String
        get() = artistDisplayName?.trim().takeIf { !it.isNullOrBlank() } ?: "Unknown Artist"
}
