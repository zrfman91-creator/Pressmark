// FILE: app/src/main/java/com/zak/pressmark/data/local/db/AppDatabase.kt
package com.zak.pressmark.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zak.pressmark.data.local.dao.AlbumDao
import com.zak.pressmark.data.local.dao.ArtistDao
import com.zak.pressmark.data.local.dao.GenreDao
import com.zak.pressmark.data.local.dao.ImportBatchDao
import com.zak.pressmark.data.local.dao.InboxItemDao
import com.zak.pressmark.data.local.dao.ProviderSnapshotDao
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.local.entity.AlbumGenreCrossRef
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.data.local.entity.ArtworkEntity
import com.zak.pressmark.data.local.entity.GenreEntity
import com.zak.pressmark.data.local.entity.ImportBatchEntity
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.local.entity.ProviderSnapshotEntity
import com.zak.pressmark.data.local.entity.ReleaseArtistCreditEntity
import com.zak.pressmark.data.local.entity.ReleaseEntity
import com.zak.pressmark.data.local.dao.ReleaseArtistCreditDao
import com.zak.pressmark.data.local.dao.ReleaseDao
import com.zak.pressmark.data.local.dao.ArtworkDao


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

        // --- Inbox pipeline --- //
        InboxItemEntity::class,
        ProviderSnapshotEntity::class,
        ImportBatchEntity::class,
    ],
    version = 12, // bump version (wipe anyway)
    exportSchema = true,
)
@TypeConverters(InboxTypeConverters::class)
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

    // --- Inbox pipeline --- //
    abstract fun inboxItemDao(): InboxItemDao
    abstract fun providerSnapshotDao(): ProviderSnapshotDao
    abstract fun importBatchDao(): ImportBatchDao
}
