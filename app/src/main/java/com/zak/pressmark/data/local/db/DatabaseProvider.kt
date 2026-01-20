// FILE: app/src/main/java/com/zak/pressmark/data/local/db/DatabaseProvider.kt
package com.zak.pressmark.data.local.db

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
object DatabaseProvider {

    private const val DB_NAME = "pressmark.db"

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return instance ?: synchronized(this) {                                   // Standard double-checked locking to ensure the instance is created only once.
            instance ?: build(context.applicationContext).also { instance = it }
        }
    }

    private fun build(appContext: Context): AppDatabase {
        return Room.databaseBuilder(appContext, AppDatabase::class.java, DB_NAME) // Wipes and rebuilds the database instead of migrating if no migration object is available. Perfect for starting fresh in development.
            .fallbackToDestructiveMigration(dropAllTables = true)                 // The `true` parameter ensures all tables defined in old versions are dropped.
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)         // WAL is a good default, it allows reads and writes to happen concurrently.
            .build()
    }
}
