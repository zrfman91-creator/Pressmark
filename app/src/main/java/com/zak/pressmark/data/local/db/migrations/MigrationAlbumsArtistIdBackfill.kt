package com.zak.pressmark.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v4 -> v5:
 * - add albums.artist_id
 * - backfill artists from DISTINCT albums.artist
 * - set albums.artist_id to the matching artists.id
 *
 * Notes:
 * - Keeps albums.artist TEXT for now (safe for alpha). Can remove later with a table rebuild migration.
 * - Artist type defaults to BAND for backfilled artists.
 */
object MigrationAlbumsArtistIdBackfill {

    private const val FROM_VERSION = 4
    private const val TO_VERSION = 5

    val MIGRATION: Migration = object : Migration(FROM_VERSION, TO_VERSION) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1) Add new nullable column
            db.execSQL("ALTER TABLE albums ADD COLUMN artist_id INTEGER")

            // 2) Insert canonical artist rows from existing album.artist values
            // Normalize: lower(trim()) for uniqueness.
            db.execSQL(
                """
                INSERT OR IGNORE INTO artists(display_name, sort_name, name_normalized, artist_type)
                SELECT DISTINCT
                    TRIM(artist) AS display_name,
                    TRIM(artist) AS sort_name,
                    LOWER(TRIM(artist)) AS name_normalized,
                    'BAND' AS artist_type
                FROM albums
                WHERE artist IS NOT NULL AND TRIM(artist) <> ''
                """.trimIndent()
            )

            // 3) Backfill albums.artist_id by joining on normalized artist name
            // Do not overwrite if artist_id is already populated.
            db.execSQL(
                """
                UPDATE albums
                SET artist_id = (
                    SELECT artists.id
                    FROM artists
                    WHERE artists.name_normalized = LOWER(TRIM(albums.artist))
                    LIMIT 1
                )
                WHERE artist_id IS NULL
                  AND albums.artist IS NOT NULL
                  AND TRIM(albums.artist) <> ''
                """.trimIndent()
            )

            // Indices
            db.execSQL("CREATE INDEX IF NOT EXISTS index_albums_artist_id ON albums(artist_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_albums_artist_title ON albums(artist, title)")
        }
    }
}
