// FILE: app/src/androidTest/java/com/zak/pressmark/data/local/db/v2/AppDatabaseV2MigrationTest.kt
package com.zak.pressmark.data.local.db.v2

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseV2MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabaseV2::class.java,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_preservesWorkDataAndAddsSortColumns() {
        val db = helper.createDatabase(TEST_DB, 1)
        createV1Schema(db)
        seedV1Data(db)
        db.close()

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MigrationsV2.MIGRATION_1_2).close()
    }

    private fun createV1Schema(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${DbSchemaV2.Work.TABLE} (
              ${DbSchemaV2.Work.ID} TEXT NOT NULL,
              ${DbSchemaV2.Work.TITLE} TEXT NOT NULL,
              ${DbSchemaV2.Work.TITLE_NORMALIZED} TEXT NOT NULL,
              ${DbSchemaV2.Work.ARTIST_LINE} TEXT NOT NULL,
              ${DbSchemaV2.Work.ARTIST_NORMALIZED} TEXT NOT NULL,
              ${DbSchemaV2.Work.YEAR} INTEGER,
              ${DbSchemaV2.Work.GENRES_JSON} TEXT NOT NULL,
              ${DbSchemaV2.Work.STYLES_JSON} TEXT NOT NULL,
              ${DbSchemaV2.Work.PRIMARY_ARTWORK_URI} TEXT,
              ${DbSchemaV2.Work.DISCOGS_MASTER_ID} INTEGER,
              ${DbSchemaV2.Work.MUSICBRAINZ_RELEASE_GROUP_ID} TEXT,
              ${DbSchemaV2.Work.CREATED_AT} INTEGER NOT NULL,
              ${DbSchemaV2.Work.UPDATED_AT} INTEGER NOT NULL,
              PRIMARY KEY (${DbSchemaV2.Work.ID})
            )
            """
                .trimIndent()
        )
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
              PRIMARY KEY (${DbSchemaV2.Release.ID})
            )
            """
                .trimIndent()
        )
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
              PRIMARY KEY (${DbSchemaV2.Pressing.ID})
            )
            """
                .trimIndent()
        )
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
              PRIMARY KEY (${DbSchemaV2.Variant.ID})
            )
            """
                .trimIndent()
        )
    }

    private fun seedV1Data(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO ${DbSchemaV2.Work.TABLE} (
              ${DbSchemaV2.Work.ID},
              ${DbSchemaV2.Work.TITLE},
              ${DbSchemaV2.Work.TITLE_NORMALIZED},
              ${DbSchemaV2.Work.ARTIST_LINE},
              ${DbSchemaV2.Work.ARTIST_NORMALIZED},
              ${DbSchemaV2.Work.YEAR},
              ${DbSchemaV2.Work.GENRES_JSON},
              ${DbSchemaV2.Work.STYLES_JSON},
              ${DbSchemaV2.Work.PRIMARY_ARTWORK_URI},
              ${DbSchemaV2.Work.DISCOGS_MASTER_ID},
              ${DbSchemaV2.Work.MUSICBRAINZ_RELEASE_GROUP_ID},
              ${DbSchemaV2.Work.CREATED_AT},
              ${DbSchemaV2.Work.UPDATED_AT}
            ) VALUES (
              'work:test',
              'The Example',
              'the example',
              'The Artist',
              'the artist',
              1999,
              '["Rock","Indie"]',
              '["Shoegaze"]',
              NULL,
              NULL,
              NULL,
              1000,
              1000
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
            ) VALUES (
              'release:test',
              'work:test',
              'Label',
              'label',
              'CAT-1',
              'cat-1',
              'US',
              'Vinyl',
              1999,
              NULL,
              1000,
              1000
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
            ) VALUES (
              'pressing:test',
              'release:test',
              '12345',
              '12345',
              '[]',
              NULL,
              'Label',
              'CAT-1',
              'US',
              'Vinyl',
              1999,
              NULL,
              NULL,
              1000,
              1000
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
            ) VALUES (
              'variant:test',
              'work:test',
              'pressing:test',
              'default',
              NULL,
              NULL,
              1000,
              NULL
            )
            """
                .trimIndent()
        )
    }

    private companion object {
        private const val TEST_DB = "migration-test-v2"
    }
}
