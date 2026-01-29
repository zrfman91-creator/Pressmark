package com.zak.pressmark.data.repository.v2

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.zak.pressmark.core.util.completion.CompletionRules
import com.zak.pressmark.data.local.db.v2.AppDatabaseV2
import com.zak.pressmark.data.local.entity.v2.ArtistEntityV2
import com.zak.pressmark.data.local.entity.v2.CanonicalWorkEntityV2
import com.zak.pressmark.data.local.entity.v2.WorkEntityV2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArtistCompletionRepositoryTest {

    private lateinit var db: AppDatabaseV2
    private lateinit var repository: ArtistCompletionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabaseV2::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ArtistCompletionRepository(db.canonicalWorkDaoV2())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun studioCompletionCountsOwnedAndMissing() = runBlocking {
        val now = System.currentTimeMillis()
        val artist = ArtistEntityV2(
            id = "artist:test",
            name = "Test Artist",
            nameNormalized = "test artist",
            discogsArtistId = null,
            createdAt = now,
            updatedAt = now,
        )
        db.artistDaoV2().upsert(artist)

        val studioLp = CanonicalWorkEntityV2(
            id = "cw:lp",
            artistId = artist.id,
            title = "Studio LP",
            titleNormalized = "studio lp",
            year = 2000,
            formatType = CompletionRules.FORMAT_LP,
            releaseType = CompletionRules.RELEASE_TYPE_STUDIO,
            discogsMasterId = 1L,
            createdAt = now,
            updatedAt = now,
        )
        val studioEp = CanonicalWorkEntityV2(
            id = "cw:ep",
            artistId = artist.id,
            title = "Studio EP",
            titleNormalized = "studio ep",
            year = 2002,
            formatType = CompletionRules.FORMAT_EP,
            releaseType = CompletionRules.RELEASE_TYPE_STUDIO,
            discogsMasterId = 2L,
            createdAt = now,
            updatedAt = now,
        )
        val liveLp = CanonicalWorkEntityV2(
            id = "cw:live",
            artistId = artist.id,
            title = "Live LP",
            titleNormalized = "live lp",
            year = 2001,
            formatType = CompletionRules.FORMAT_LP,
            releaseType = CompletionRules.RELEASE_TYPE_LIVE,
            discogsMasterId = 3L,
            createdAt = now,
            updatedAt = now,
        )

        db.canonicalWorkDaoV2().upsert(studioLp)
        db.canonicalWorkDaoV2().upsert(studioEp)
        db.canonicalWorkDaoV2().upsert(liveLp)

        val ownedWork = WorkEntityV2(
            id = "work:owned",
            title = "Studio LP",
            titleNormalized = "studio lp",
            titleSort = "studio lp",
            artistLine = "Test Artist",
            artistNormalized = "test artist",
            artistSort = "test artist",
            year = 2000,
            genresJson = "[]",
            stylesJson = "[]",
            primaryArtworkUri = null,
            canonicalWorkId = studioLp.id,
            discogsMasterId = 1L,
            musicBrainzReleaseGroupId = null,
            createdAt = now,
            updatedAt = now,
        )
        db.workDaoV2().upsert(ownedWork)

        val summary = repository.observeArtistCompletion(artist.id).first()

        assertEquals(1, summary.ownedCount)
        assertEquals(2, summary.totalCount)
        assertEquals(1, summary.missing.size)
        assertEquals("Studio EP", summary.missing.first().title)
        assertFalse(summary.isComplete)

        db.workDaoV2().upsert(
            ownedWork.copy(
                id = "work:owned-ep",
                title = "Studio EP",
                titleNormalized = "studio ep",
                titleSort = "studio ep",
                canonicalWorkId = studioEp.id,
                discogsMasterId = 2L,
            )
        )

        val completeSummary = repository.observeArtistCompletion(artist.id).first()
        assertEquals(2, completeSummary.ownedCount)
        assertEquals(2, completeSummary.totalCount)
        assertTrue(completeSummary.isComplete)
    }
}
