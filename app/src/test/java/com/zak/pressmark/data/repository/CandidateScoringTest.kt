package com.zak.pressmark.data.repository

import com.zak.pressmark.data.model.inbox.ProviderCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateScoringTest {
    @Test
    fun `barcode match yields high confidence`() {
        val candidate = ProviderCandidate(
            provider = "discogs",
            providerItemId = "1",
            title = "Test Title",
            artist = "Test Artist",
            year = null,
            label = null,
            catalogNo = null,
            formatSummary = null,
            thumbUrl = null,
            barcode = "123",
            rawJson = "{}",
        )

        val score = InboxPipeline.scoreCandidate(
            queryTitle = "Test Title",
            queryArtist = "Test Artist",
            queryCatalogNo = null,
            queryBarcode = "123",
            candidate = candidate,
        )

        assertTrue(score.confidence >= 50)
        assertEquals(true, score.reasonsJson.contains("barcode_match"))
    }

    @Test
    fun `auto commit requires high score and gap`() {
        val shouldCommit = InboxPipeline.shouldAutoCommit(
            topScore = 96,
            secondScore = 80,
            wasUndone = false,
        )
        val shouldNotCommitGap = InboxPipeline.shouldAutoCommit(
            topScore = 96,
            secondScore = 90,
            wasUndone = false,
        )

        assertTrue(shouldCommit)
        assertEquals(false, shouldNotCommitGap)
    }
}
