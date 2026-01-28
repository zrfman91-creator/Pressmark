package com.zak.pressmark.data.local.db.v2

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * App-local Room database singleton provider.
 *
 * Notes:
 * - Uses applicationContext to avoid leaking Activities.
 * - Uses fallbackToDestructiveMigration for development. This will wipe and
 *   rebuild the database on schema changes, which is ideal for a clean start.
 *   This should be removed in favor of proper migrations for a production release.
 */
object DatabaseProviderV2 {

    private const val DB_NAME = "pressmark_v2.db"

    @Volatile
    private var INSTANCE: AppDatabaseV2? = null

    fun get(context: Context): AppDatabaseV2 {
        return INSTANCE ?: synchronized(this) {                                   // Standard double-checked locking to ensure the INSTANCE is created only once.
            INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
        }
    }

    private fun build(appContext: Context): AppDatabaseV2 {
        return Room.databaseBuilder(appContext, AppDatabaseV2::class.java, DB_NAME) // Wipes and rebuilds the database instead of migrating if no migration object is available. Perfect for starting fresh in development.
            .addMigrations(MigrationsV2.MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = true)                 // The `true` parameter ensures all tables defined in old versions are dropped.
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)         // WAL is a good default, it allows reads and writes to happen concurrently.
            .build()
    }
}
