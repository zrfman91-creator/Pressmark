// file: app/src/androidTest/java/com/zak/pressmark/data/local/repository/ReleaseUpsertFromRawArtistSmokeTest.kt
package com.zak.pressmark.data.local.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zak.pressmark.data.local.db.AppDatabase
import com.zak.pressmark.data.local.entity.CreditRole
import com.zak.pressmark.data.local.entity.ReleaseEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleaseUpsertFromRawArtistSmokeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val db: AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private val repo = ReleaseRepository(db)

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertFromRawArtist_persistsParsedCreditsInOrder() = runBlocking {
        val releaseId = "r_test_1"
        val release = ReleaseEntity(
            id = releaseId,
            title = "Test Release",
            releaseYear = 1940,
            label = null,
            catalogNo = null,
            format = null,
            addedAt = 1_000L,
        )

        repo.upsertReleaseFromRawArtist(
            release = release,
            rawArtist = "So-and-so with His Orchestra",
        )

        val credits = db.releaseArtistCreditDao().creditsForRelease(releaseId)
        assertEquals(2, credits.size)

        assertEquals(1, credits[0].position)
        assertEquals(CreditRole.PRIMARY, credits[0].role)

        assertEquals(2, credits[1].position)
        assertEquals(CreditRole.ORCHESTRA, credits[1].role)
        assertEquals("with his orchestra", credits[1].displayHint)
    }

    @Test
    fun upsertFromRawArtist_persistsFeatured() = runBlocking {
        val releaseId = "r_test_2"
        val release = ReleaseEntity(
            id = releaseId,
            title = "Test Release 2",
            releaseYear = null,
            label = null,
            catalogNo = null,
            format = null,
            addedAt = 2_000L,
        )

        repo.upsertReleaseFromRawArtist(
            release = release,
            rawArtist = "X feat. Y",
        )

        val credits = db.releaseArtistCreditDao().creditsForRelease(releaseId)
        assertEquals(2, credits.size)

        assertEquals(CreditRole.PRIMARY, credits[0].role)
        assertEquals(CreditRole.FEATURED, credits[1].role)
    }
}
