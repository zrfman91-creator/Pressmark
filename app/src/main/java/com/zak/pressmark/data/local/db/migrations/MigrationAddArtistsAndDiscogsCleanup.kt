package com.zak.pressmark.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v3 -> v4:
 * 1) Create artists table (canonical Artist records)
 * 2) Clean up legacy Discogs cover sentinel ("discogs:not_found")
 *
 * IMPORTANT: Keep these version numbers in sync with AppDatabase history.
 */
object MigrationAddArtistsAndDiscogsCleanup {

    private const val FROM_VERSION = 3
    private const val TO_VERSION = 4

    val MIGRATION: Migration = object : Migration(FROM_VERSION, TO_VERSION) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // --- Artists table ---
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS artists (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    display_name TEXT NOT NULL,
                    sort_name TEXT NOT NULL,
                    name_normalized TEXT NOT NULL,
                    artist_type TEXT NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_artists_name_normalized
                ON artists(name_normalized)
                """.trimIndent()
            )

            // --- Legacy Discogs sentinel cleanup ---
            // Prefer NULL over sentinels like -1 so app logic stays clean.
            db.execSQL(
                """
                UPDATE albums
                SET cover_uri = NULL,
                    discogs_release_id = NULL
                WHERE cover_uri = 'discogs:not_found'
                """.trimIndent()
            )
        }
    }
}
