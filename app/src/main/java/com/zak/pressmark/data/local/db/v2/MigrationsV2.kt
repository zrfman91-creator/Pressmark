// FILE: app/src/main/java/com/zak/pressmark/data/local/db/v2/MigrationsV2.kt
package com.zak.pressmark.data.local.db.v2

import android.content.ContentValues
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray

object MigrationsV2 {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            addSortColumns(db)
            createGenreStyleTables(db)
            createWorkGenreStyleTables(db)
            addWorkIndexes(db)
            addForeignKeys(db)
            backfillSortColumnsAndGenres(db)
        }
    }

    private fun addSortColumns(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE ${DbSchemaV2.Work.TABLE}
            ADD COLUMN ${DbSchemaV2.Work.TITLE_SORT} TEXT NOT NULL DEFAULT ''
            """
                .trimIndent()
        )
        db.execSQL(
            """
            ALTER TABLE ${DbSchemaV2.Work.TABLE}
            ADD COLUMN ${DbSchemaV2.Work.ARTIST_SORT} TEXT NOT NULL DEFAULT ''
            """
                .trimIndent()
        )
    }

    private fun createGenreStyleTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${DbSchemaV2.Genre.TABLE} (
              ${DbSchemaV2.Genre.ID} INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              ${DbSchemaV2.Genre.NAME_NORMALIZED} TEXT NOT NULL,
              ${DbSchemaV2.Genre.NAME_DISPLAY} TEXT NOT NULL
            )
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_${DbSchemaV2.Genre.TABLE}_${DbSchemaV2.Genre.NAME_NORMALIZED}
            ON ${DbSchemaV2.Genre.TABLE} (${DbSchemaV2.Genre.NAME_NORMALIZED})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${DbSchemaV2.Style.TABLE} (
              ${DbSchemaV2.Style.ID} INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              ${DbSchemaV2.Style.NAME_NORMALIZED} TEXT NOT NULL,
              ${DbSchemaV2.Style.NAME_DISPLAY} TEXT NOT NULL
            )
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_${DbSchemaV2.Style.TABLE}_${DbSchemaV2.Style.NAME_NORMALIZED}
            ON ${DbSchemaV2.Style.TABLE} (${DbSchemaV2.Style.NAME_NORMALIZED})
            """
                .trimIndent()
        )
    }

    private fun createWorkGenreStyleTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${DbSchemaV2.WorkGenre.TABLE} (
              ${DbSchemaV2.WorkGenre.WORK_ID} TEXT NOT NULL,
              ${DbSchemaV2.WorkGenre.GENRE_ID} INTEGER NOT NULL,
              PRIMARY KEY (${DbSchemaV2.WorkGenre.WORK_ID}, ${DbSchemaV2.WorkGenre.GENRE_ID}),
              FOREIGN KEY (${DbSchemaV2.WorkGenre.WORK_ID}) REFERENCES ${DbSchemaV2.Work.TABLE} (${DbSchemaV2.Work.ID}) ON DELETE CASCADE,
              FOREIGN KEY (${DbSchemaV2.WorkGenre.GENRE_ID}) REFERENCES ${DbSchemaV2.Genre.TABLE} (${DbSchemaV2.Genre.ID}) ON DELETE CASCADE
            )
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.WorkGenre.TABLE}_${DbSchemaV2.WorkGenre.WORK_ID}
            ON ${DbSchemaV2.WorkGenre.TABLE} (${DbSchemaV2.WorkGenre.WORK_ID})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.WorkGenre.TABLE}_${DbSchemaV2.WorkGenre.GENRE_ID}
            ON ${DbSchemaV2.WorkGenre.TABLE} (${DbSchemaV2.WorkGenre.GENRE_ID})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${DbSchemaV2.WorkStyle.TABLE} (
              ${DbSchemaV2.WorkStyle.WORK_ID} TEXT NOT NULL,
              ${DbSchemaV2.WorkStyle.STYLE_ID} INTEGER NOT NULL,
              PRIMARY KEY (${DbSchemaV2.WorkStyle.WORK_ID}, ${DbSchemaV2.WorkStyle.STYLE_ID}),
              FOREIGN KEY (${DbSchemaV2.WorkStyle.WORK_ID}) REFERENCES ${DbSchemaV2.Work.TABLE} (${DbSchemaV2.Work.ID}) ON DELETE CASCADE,
              FOREIGN KEY (${DbSchemaV2.WorkStyle.STYLE_ID}) REFERENCES ${DbSchemaV2.Style.TABLE} (${DbSchemaV2.Style.ID}) ON DELETE CASCADE
            )
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.WorkStyle.TABLE}_${DbSchemaV2.WorkStyle.WORK_ID}
            ON ${DbSchemaV2.WorkStyle.TABLE} (${DbSchemaV2.WorkStyle.WORK_ID})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.WorkStyle.TABLE}_${DbSchemaV2.WorkStyle.STYLE_ID}
            ON ${DbSchemaV2.WorkStyle.TABLE} (${DbSchemaV2.WorkStyle.STYLE_ID})
            """
                .trimIndent()
        )
    }

    private fun addWorkIndexes(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Work.TABLE}_${DbSchemaV2.Work.TITLE_SORT}
            ON ${DbSchemaV2.Work.TABLE} (${DbSchemaV2.Work.TITLE_SORT})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Work.TABLE}_${DbSchemaV2.Work.ARTIST_SORT}
            ON ${DbSchemaV2.Work.TABLE} (${DbSchemaV2.Work.ARTIST_SORT})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Work.TABLE}_${DbSchemaV2.Work.ARTIST_NORMALIZED}_${DbSchemaV2.Work.TITLE_NORMALIZED}_${DbSchemaV2.Work.YEAR}
            ON ${DbSchemaV2.Work.TABLE} (
              ${DbSchemaV2.Work.ARTIST_NORMALIZED},
              ${DbSchemaV2.Work.TITLE_NORMALIZED},
              ${DbSchemaV2.Work.YEAR}
            )
            """
                .trimIndent()
        )
    }

    private fun addForeignKeys(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ${DbSchemaV2.Release.TABLE} RENAME TO ${DbSchemaV2.Release.TABLE}_old")
        db.execSQL(
            """
            CREATE TABLE ${DbSchemaV2.Release.TABLE} (
              ${DbSchemaV2.Release.ID} TEXT NOT NULL,
              ${DbSchemaV2.Release.WORK_ID} TEXT NOT NULL,
              ${DbSchemaV2.Release.LABEL} TEXT,
              ${DbSchemaV2.Release.LABEL_NORMALIZED} TEXT,
              ${DbSchemaV2.Release.CATALOG_NO} TEXT,
              ${DbSchemaV2.Release.CATALOG_NO_NORMALIZED} TEXT,
              ${DbSchemaV2.Release.COUNTRY} TEXT,
              ${DbSchemaV2.Release.FORMAT} TEXT,
              ${DbSchemaV2.Release.RELEASE_YEAR} INTEGER,
              ${DbSchemaV2.Release.RELEASE_TYPE} TEXT,
              ${DbSchemaV2.Release.CREATED_AT} INTEGER NOT NULL,
              ${DbSchemaV2.Release.UPDATED_AT} INTEGER NOT NULL,
              PRIMARY KEY (${DbSchemaV2.Release.ID}),
              FOREIGN KEY (${DbSchemaV2.Release.WORK_ID})
                REFERENCES ${DbSchemaV2.Work.TABLE} (${DbSchemaV2.Work.ID})
                ON DELETE CASCADE
            )
            """
                .trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO ${DbSchemaV2.Release.TABLE} (
              ${DbSchemaV2.Release.ID},
              ${DbSchemaV2.Release.WORK_ID},
              ${DbSchemaV2.Release.LABEL},
              ${DbSchemaV2.Release.LABEL_NORMALIZED},
              ${DbSchemaV2.Release.CATALOG_NO},
              ${DbSchemaV2.Release.CATALOG_NO_NORMALIZED},
              ${DbSchemaV2.Release.COUNTRY},
              ${DbSchemaV2.Release.FORMAT},
              ${DbSchemaV2.Release.RELEASE_YEAR},
              ${DbSchemaV2.Release.RELEASE_TYPE},
              ${DbSchemaV2.Release.CREATED_AT},
              ${DbSchemaV2.Release.UPDATED_AT}
            )
            SELECT
              ${DbSchemaV2.Release.ID},
              ${DbSchemaV2.Release.WORK_ID},
              ${DbSchemaV2.Release.LABEL},
              ${DbSchemaV2.Release.LABEL_NORMALIZED},
              ${DbSchemaV2.Release.CATALOG_NO},
              ${DbSchemaV2.Release.CATALOG_NO_NORMALIZED},
              ${DbSchemaV2.Release.COUNTRY},
              ${DbSchemaV2.Release.FORMAT},
              ${DbSchemaV2.Release.RELEASE_YEAR},
              ${DbSchemaV2.Release.RELEASE_TYPE},
              ${DbSchemaV2.Release.CREATED_AT},
              ${DbSchemaV2.Release.UPDATED_AT}
            FROM ${DbSchemaV2.Release.TABLE}_old
            """
                .trimIndent()
        )
        db.execSQL("DROP TABLE ${DbSchemaV2.Release.TABLE}_old")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Release.TABLE}_${DbSchemaV2.Release.WORK_ID}
            ON ${DbSchemaV2.Release.TABLE} (${DbSchemaV2.Release.WORK_ID})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Release.TABLE}_${DbSchemaV2.Release.LABEL_NORMALIZED}
            ON ${DbSchemaV2.Release.TABLE} (${DbSchemaV2.Release.LABEL_NORMALIZED})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Release.TABLE}_${DbSchemaV2.Release.CATALOG_NO_NORMALIZED}
            ON ${DbSchemaV2.Release.TABLE} (${DbSchemaV2.Release.CATALOG_NO_NORMALIZED})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Release.TABLE}_${DbSchemaV2.Release.RELEASE_YEAR}
            ON ${DbSchemaV2.Release.TABLE} (${DbSchemaV2.Release.RELEASE_YEAR})
            """
                .trimIndent()
        )

        db.execSQL("ALTER TABLE ${DbSchemaV2.Pressing.TABLE} RENAME TO ${DbSchemaV2.Pressing.TABLE}_old")
        db.execSQL(
            """
            CREATE TABLE ${DbSchemaV2.Pressing.TABLE} (
              ${DbSchemaV2.Pressing.ID} TEXT NOT NULL,
              ${DbSchemaV2.Pressing.RELEASE_ID} TEXT NOT NULL,
              ${DbSchemaV2.Pressing.BARCODE} TEXT,
              ${DbSchemaV2.Pressing.BARCODE_NORMALIZED} TEXT,
              ${DbSchemaV2.Pressing.RUNOUTS_JSON} TEXT NOT NULL,
              ${DbSchemaV2.Pressing.PRESSING_PLANT} TEXT,
              ${DbSchemaV2.Pressing.LABEL} TEXT,
              ${DbSchemaV2.Pressing.CATALOG_NO} TEXT,
              ${DbSchemaV2.Pressing.COUNTRY} TEXT,
              ${DbSchemaV2.Pressing.FORMAT} TEXT,
              ${DbSchemaV2.Pressing.RELEASE_YEAR} INTEGER,
              ${DbSchemaV2.Pressing.DISCOGS_RELEASE_ID} INTEGER,
              ${DbSchemaV2.Pressing.MUSICBRAINZ_RELEASE_ID} TEXT,
              ${DbSchemaV2.Pressing.CREATED_AT} INTEGER NOT NULL,
              ${DbSchemaV2.Pressing.UPDATED_AT} INTEGER NOT NULL,
              PRIMARY KEY (${DbSchemaV2.Pressing.ID}),
              FOREIGN KEY (${DbSchemaV2.Pressing.RELEASE_ID})
                REFERENCES ${DbSchemaV2.Release.TABLE} (${DbSchemaV2.Release.ID})
                ON DELETE CASCADE
            )
            """
                .trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO ${DbSchemaV2.Pressing.TABLE} (
              ${DbSchemaV2.Pressing.ID},
              ${DbSchemaV2.Pressing.RELEASE_ID},
              ${DbSchemaV2.Pressing.BARCODE},
              ${DbSchemaV2.Pressing.BARCODE_NORMALIZED},
              ${DbSchemaV2.Pressing.RUNOUTS_JSON},
              ${DbSchemaV2.Pressing.PRESSING_PLANT},
              ${DbSchemaV2.Pressing.LABEL},
              ${DbSchemaV2.Pressing.CATALOG_NO},
              ${DbSchemaV2.Pressing.COUNTRY},
              ${DbSchemaV2.Pressing.FORMAT},
              ${DbSchemaV2.Pressing.RELEASE_YEAR},
              ${DbSchemaV2.Pressing.DISCOGS_RELEASE_ID},
              ${DbSchemaV2.Pressing.MUSICBRAINZ_RELEASE_ID},
              ${DbSchemaV2.Pressing.CREATED_AT},
              ${DbSchemaV2.Pressing.UPDATED_AT}
            )
            SELECT
              ${DbSchemaV2.Pressing.ID},
              ${DbSchemaV2.Pressing.RELEASE_ID},
              ${DbSchemaV2.Pressing.BARCODE},
              ${DbSchemaV2.Pressing.BARCODE_NORMALIZED},
              ${DbSchemaV2.Pressing.RUNOUTS_JSON},
              ${DbSchemaV2.Pressing.PRESSING_PLANT},
              ${DbSchemaV2.Pressing.LABEL},
              ${DbSchemaV2.Pressing.CATALOG_NO},
              ${DbSchemaV2.Pressing.COUNTRY},
              ${DbSchemaV2.Pressing.FORMAT},
              ${DbSchemaV2.Pressing.RELEASE_YEAR},
              ${DbSchemaV2.Pressing.DISCOGS_RELEASE_ID},
              ${DbSchemaV2.Pressing.MUSICBRAINZ_RELEASE_ID},
              ${DbSchemaV2.Pressing.CREATED_AT},
              ${DbSchemaV2.Pressing.UPDATED_AT}
            FROM ${DbSchemaV2.Pressing.TABLE}_old
            """
                .trimIndent()
        )
        db.execSQL("DROP TABLE ${DbSchemaV2.Pressing.TABLE}_old")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Pressing.TABLE}_${DbSchemaV2.Pressing.RELEASE_ID}
            ON ${DbSchemaV2.Pressing.TABLE} (${DbSchemaV2.Pressing.RELEASE_ID})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Pressing.TABLE}_${DbSchemaV2.Pressing.BARCODE_NORMALIZED}
            ON ${DbSchemaV2.Pressing.TABLE} (${DbSchemaV2.Pressing.BARCODE_NORMALIZED})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Pressing.TABLE}_${DbSchemaV2.Pressing.DISCOGS_RELEASE_ID}
            ON ${DbSchemaV2.Pressing.TABLE} (${DbSchemaV2.Pressing.DISCOGS_RELEASE_ID})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Pressing.TABLE}_${DbSchemaV2.Pressing.MUSICBRAINZ_RELEASE_ID}
            ON ${DbSchemaV2.Pressing.TABLE} (${DbSchemaV2.Pressing.MUSICBRAINZ_RELEASE_ID})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Pressing.TABLE}_${DbSchemaV2.Pressing.UPDATED_AT}
            ON ${DbSchemaV2.Pressing.TABLE} (${DbSchemaV2.Pressing.UPDATED_AT})
            """
                .trimIndent()
        )

        db.execSQL("ALTER TABLE ${DbSchemaV2.Variant.TABLE} RENAME TO ${DbSchemaV2.Variant.TABLE}_old")
        db.execSQL(
            """
            CREATE TABLE ${DbSchemaV2.Variant.TABLE} (
              ${DbSchemaV2.Variant.ID} TEXT NOT NULL,
              ${DbSchemaV2.Variant.WORK_ID} TEXT NOT NULL,
              ${DbSchemaV2.Variant.PRESSING_ID} TEXT NOT NULL,
              ${DbSchemaV2.Variant.VARIANT_KEY} TEXT NOT NULL,
              ${DbSchemaV2.Variant.NOTES} TEXT,
              ${DbSchemaV2.Variant.RATING} INTEGER,
              ${DbSchemaV2.Variant.ADDED_AT} INTEGER NOT NULL,
              ${DbSchemaV2.Variant.LAST_PLAYED_AT} INTEGER,
              PRIMARY KEY (${DbSchemaV2.Variant.ID}),
              FOREIGN KEY (${DbSchemaV2.Variant.WORK_ID})
                REFERENCES ${DbSchemaV2.Work.TABLE} (${DbSchemaV2.Work.ID})
                ON DELETE CASCADE,
              FOREIGN KEY (${DbSchemaV2.Variant.PRESSING_ID})
                REFERENCES ${DbSchemaV2.Pressing.TABLE} (${DbSchemaV2.Pressing.ID})
                ON DELETE CASCADE
            )
            """
                .trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO ${DbSchemaV2.Variant.TABLE} (
              ${DbSchemaV2.Variant.ID},
              ${DbSchemaV2.Variant.WORK_ID},
              ${DbSchemaV2.Variant.PRESSING_ID},
              ${DbSchemaV2.Variant.VARIANT_KEY},
              ${DbSchemaV2.Variant.NOTES},
              ${DbSchemaV2.Variant.RATING},
              ${DbSchemaV2.Variant.ADDED_AT},
              ${DbSchemaV2.Variant.LAST_PLAYED_AT}
            )
            SELECT
              ${DbSchemaV2.Variant.ID},
              ${DbSchemaV2.Variant.WORK_ID},
              ${DbSchemaV2.Variant.PRESSING_ID},
              ${DbSchemaV2.Variant.VARIANT_KEY},
              ${DbSchemaV2.Variant.NOTES},
              ${DbSchemaV2.Variant.RATING},
              ${DbSchemaV2.Variant.ADDED_AT},
              ${DbSchemaV2.Variant.LAST_PLAYED_AT}
            FROM ${DbSchemaV2.Variant.TABLE}_old
            """
                .trimIndent()
        )
        db.execSQL("DROP TABLE ${DbSchemaV2.Variant.TABLE}_old")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Variant.TABLE}_${DbSchemaV2.Variant.WORK_ID}
            ON ${DbSchemaV2.Variant.TABLE} (${DbSchemaV2.Variant.WORK_ID})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Variant.TABLE}_${DbSchemaV2.Variant.PRESSING_ID}
            ON ${DbSchemaV2.Variant.TABLE} (${DbSchemaV2.Variant.PRESSING_ID})
            """
                .trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_${DbSchemaV2.Variant.TABLE}_${DbSchemaV2.Variant.VARIANT_KEY}
            ON ${DbSchemaV2.Variant.TABLE} (${DbSchemaV2.Variant.VARIANT_KEY})
            """
                .trimIndent()
        )
    }

    private fun backfillSortColumnsAndGenres(db: SupportSQLiteDatabase) {
        val workCursor = db.query(
            """
            SELECT ${DbSchemaV2.Work.ID},
                   ${DbSchemaV2.Work.TITLE},
                   ${DbSchemaV2.Work.ARTIST_LINE},
                   ${DbSchemaV2.Work.GENRES_JSON},
                   ${DbSchemaV2.Work.STYLES_JSON}
            FROM ${DbSchemaV2.Work.TABLE}
            """
                .trimIndent()
        )

        val genreCache = mutableMapOf<String, Long>()
        val styleCache = mutableMapOf<String, Long>()

        workCursor.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DbSchemaV2.Work.ID)
            val titleIndex = cursor.getColumnIndexOrThrow(DbSchemaV2.Work.TITLE)
            val artistIndex = cursor.getColumnIndexOrThrow(DbSchemaV2.Work.ARTIST_LINE)
            val genresIndex = cursor.getColumnIndexOrThrow(DbSchemaV2.Work.GENRES_JSON)
            val stylesIndex = cursor.getColumnIndexOrThrow(DbSchemaV2.Work.STYLES_JSON)

            while (cursor.moveToNext()) {
                val workId = cursor.getString(idIndex)
                val title = cursor.getString(titleIndex).orEmpty()
                val artistLine = cursor.getString(artistIndex).orEmpty()
                val genresJson = cursor.getString(genresIndex).orEmpty()
                val stylesJson = cursor.getString(stylesIndex).orEmpty()

                val titleSort = normalizeForSort(title, stripLeadingThe = true)
                val artistSort = normalizeForSort(artistLine, stripLeadingThe = true)

                val updateValues = ContentValues().apply {
                    put(DbSchemaV2.Work.TITLE_SORT, titleSort)
                    put(DbSchemaV2.Work.ARTIST_SORT, artistSort)
                }
                db.update(DbSchemaV2.Work.TABLE, 0, updateValues, "${DbSchemaV2.Work.ID} = ?", arrayOf(workId))

                val genres = parseJsonList(genresJson)
                val styles = parseJsonList(stylesJson)

                genres.forEach { raw ->
                    val normalized = normalizeName(raw)
                    if (normalized.isBlank()) return@forEach
                    val display = raw.trim().ifBlank { normalized }
                    val genreId = getOrCreateNamedId(
                        db = db,
                        table = DbSchemaV2.Genre.TABLE,
                        idColumn = DbSchemaV2.Genre.ID,
                        normalizedColumn = DbSchemaV2.Genre.NAME_NORMALIZED,
                        displayColumn = DbSchemaV2.Genre.NAME_DISPLAY,
                        normalized = normalized,
                        display = display,
                        cache = genreCache,
                    )
                    insertCrossRef(
                        db = db,
                        table = DbSchemaV2.WorkGenre.TABLE,
                        workColumn = DbSchemaV2.WorkGenre.WORK_ID,
                        refColumn = DbSchemaV2.WorkGenre.GENRE_ID,
                        workId = workId,
                        refId = genreId,
                    )
                }

                styles.forEach { raw ->
                    val normalized = normalizeName(raw)
                    if (normalized.isBlank()) return@forEach
                    val display = raw.trim().ifBlank { normalized }
                    val styleId = getOrCreateNamedId(
                        db = db,
                        table = DbSchemaV2.Style.TABLE,
                        idColumn = DbSchemaV2.Style.ID,
                        normalizedColumn = DbSchemaV2.Style.NAME_NORMALIZED,
                        displayColumn = DbSchemaV2.Style.NAME_DISPLAY,
                        normalized = normalized,
                        display = display,
                        cache = styleCache,
                    )
                    insertCrossRef(
                        db = db,
                        table = DbSchemaV2.WorkStyle.TABLE,
                        workColumn = DbSchemaV2.WorkStyle.WORK_ID,
                        refColumn = DbSchemaV2.WorkStyle.STYLE_ID,
                        workId = workId,
                        refId = styleId,
                    )
                }
            }
        }
    }

    private fun parseJsonList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i).trim()
                    if (value.isNotBlank()) add(value)
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun normalizeName(raw: String): String =
        raw.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun normalizeForSort(raw: String, stripLeadingThe: Boolean): String {
        val trimmed = raw.trim().lowercase().replace(Regex("\\s+"), " ")
        if (!stripLeadingThe) return trimmed
        return trimmed.removePrefix("the ").trimStart()
    }

    private fun getOrCreateNamedId(
        db: SupportSQLiteDatabase,
        table: String,
        idColumn: String,
        normalizedColumn: String,
        displayColumn: String,
        normalized: String,
        display: String,
        cache: MutableMap<String, Long>,
    ): Long {
        cache[normalized]?.let { return it }

        db.query(
            "SELECT $idColumn FROM $table WHERE $normalizedColumn = ? LIMIT 1",
            arrayOf(normalized),
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                cache[normalized] = id
                return id
            }
        }

        val values = ContentValues().apply {
            put(normalizedColumn, normalized)
            put(displayColumn, display)
        }
        val insertedId = db.insert(table, 0, values)
        val id = if (insertedId == -1L) {
            db.query(
                "SELECT $idColumn FROM $table WHERE $normalizedColumn = ? LIMIT 1",
                arrayOf(normalized),
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            }
        } else {
            insertedId
        }
        cache[normalized] = id
        return id
    }

    private fun insertCrossRef(
        db: SupportSQLiteDatabase,
        table: String,
        workColumn: String,
        refColumn: String,
        workId: String,
        refId: Long,
    ) {
        val values = ContentValues().apply {
            put(workColumn, workId)
            put(refColumn, refId)
        }
        db.insert(table, 0, values)
    }
}
