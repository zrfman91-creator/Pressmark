// file: app/src/main/java/com/zak/pressmark/data/local/db/AppDatabase.kt
package com.zak.pressmark.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zak.pressmark.data.local.dao.AlbumDao
import com.zak.pressmark.data.local.dao.ArtistDao
import com.zak.pressmark.data.local.dao.GenreDao
import com.zak.pressmark.data.local.entity.*
import com.zak.pressmark.data.local.dao.*


@Database(
    entities = [
        // --- Legacy (kept temporarily for bottom-up refactor) ---
        AlbumEntity::class,
        GenreEntity::class,
        AlbumGenreCrossRef::class,

        // --- Canonical entities ---
        ArtistEntity::class,

        // --- New Release-first model ---
        ReleaseEntity::class,
        ReleaseArtistCreditEntity::class,
        ArtworkEntity::class,
    ],
    version = 9, // bump version (wipe anyway)
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    // --- Legacy DAOs (do not remove yet) ---
    abstract fun albumDao(): AlbumDao
    abstract fun genreDao(): GenreDao

    // --- Canonical ---
    abstract fun artistDao(): ArtistDao

    // --- New DAOs (added in next steps) ---
    abstract fun releaseDao(): ReleaseDao
    abstract fun releaseArtistCreditDao(): ReleaseArtistCreditDao
    abstract fun artworkDao(): ArtworkDao
}
