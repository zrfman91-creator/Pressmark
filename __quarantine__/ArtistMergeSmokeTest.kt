// FILE: app/src/androidTest/java/com/zak/pressmark/ArtistMergeSmokeTest.kt
package com.zak.pressmark

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zak.pressmark.data.local.db.v2.AppDatabaseV2
import com.zak.pressmark.data.local.entity.v2.ArtistEntity
import com.zak.pressmark.data.local.entity.v1.CreditRole
import com.zak.pressmark.data.local.entity.v1.ReleaseArtistCreditEntity
import com.zak.pressmark.data.local.entity.v1.ReleaseEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArtistMergeSmokeTest {

    private lateinit var db: AppDatabaseV2

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabaseV2::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun mergeArtist_movesCredits_dedupes_andDeletesDuplicate() = runBlocking {
        val artistDao = db.artistDao()
        val releaseDao = db.releaseDao()
        val creditDao = db.releaseArtistCreditDao()

        // Create canonical + duplicate artist
        val canonicalId = artistDao.insert(
            ArtistEntity(
                displayName = "Glenn Miller",
                sortName = "Miller, Glenn",
                nameNormalized = "glenn miller",
                artistType = "PERSON"
            )
        )
        val duplicateId = artistDao.insert(
            ArtistEntity(
                displayName = "GLENN  MILLER",
                sortName = "Miller, Glenn",
                nameNormalized = "glenn  miller", // intentionally "wrong" normalized for test
                artistType = "PERSON"
            )
        )

        // Create a Release
        val releaseId = "r1"
        releaseDao.insert(
            ReleaseEntity(
                id = releaseId,
                title = "The Golden Hits",
                addedAt = System.currentTimeMillis()
            )
        )

        // Add credits that will collide after merge (same role/position)
        val c1 = ReleaseArtistCreditEntity(
            releaseId = releaseId,
            artistId = canonicalId,
            role = CreditRole.PRIMARY,
            position = 1
        )
        val c2 = ReleaseArtistCreditEntity(
            releaseId = releaseId,
            artistId = duplicateId,
            role = CreditRole.PRIMARY,
            position = 1
        )

        creditDao.insertAll(listOf(c1, c2))

        // Sanity: both artists exist
        assertNotNull(artistDao.getById(canonicalId))
        assertNotNull(artistDao.getById(duplicateId))

        // Merge duplicate → canonical
        val result = artistDao.mergeArtist(duplicateId = duplicateId, canonicalId = canonicalId)

        // Duplicate artist should be deleted
        assertEquals(null, artistDao.getById(duplicateId))

        // Credits should now reference canonical only
        val credits = creditDao.creditsForRelease(releaseId)
        assertEquals(1, credits.size) // dedupe should keep only one credit row
        assertEquals(canonicalId, credits.first().artistId)

        // Result counters should be non-zero in expected places
        // moved could be 1, deduped could be 1, deleted should be 1
        assertEquals(1, result.artistsDeleted)
    }

    @Test
    fun deleteArtist_guardrail_countCredits_blocksWhenInUse() = runBlocking {
        val artistDao = db.artistDao()
        val releaseDao = db.releaseDao()
        val creditDao = db.releaseArtistCreditDao()

        val artistId = artistDao.insert(
            ArtistEntity(
                displayName = "Ella Fitzgerald",
                sortName = "Fitzgerald, Ella",
                nameNormalized = "ella fitzgerald",
                artistType = "PERSON"
            )
        )

        val releaseId = "r2"
        releaseDao.insert(
            ReleaseEntity(
                id = releaseId,
                title = "Ella & Louis",
                addedAt = System.currentTimeMillis()
            )
        )

        creditDao.insert(
            ReleaseArtistCreditEntity(
                releaseId = releaseId,
                artistId = artistId,
                role = CreditRole.PRIMARY,
                position = 1
            )
        )

        val creditCount = artistDao.countCredits(artistId)
        assertEquals(1, creditCount)

        // You *can* still delete via DAO, but repository policy should block it.
        // This test just confirms the guardrail signal exists:
        // countCredits > 0 means "blocked".
        // (Repository-level behavior is enforced in ArtistRepository.deleteArtistIfUnused)
    }
}
