package com.zak.pressmark.data.repository

import com.zak.pressmark.data.model.inbox.ProviderCandidate
import com.zak.pressmark.data.repository.v1.InboxPipeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateScoringTest {

    @Test
    fun barcode_only_match_yields_auto_commit_grade_confidence() {
        val candidate = ProviderCandidate(
            provider = "discogs",
            providerItemId = "1",
            title = "Some Title",
            artist = "Some Artist",
            year = null,
            label = null,
            catalogNo = null,
            formatSummary = null,
            thumbUrl = null,
            barcode = "012345678905",
            rawJson = "{}",
        )

        val score = InboxPipeline.scoreCandidate(
            queryTitle = null,
            queryArtist = null,
            queryCatalogNo = null,
            queryBarcode = "012345678905",
            candidate = candidate,
        )

        assertTrue(score.confidence >= 95)
        assertEquals(true, score.reasonsJson.contains("barcode_match"))
    }

    @Test
    fun barcode_match_yields_high_confidence() {
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

        assertTrue(score.confidence >= 95)
        assertEquals(true, score.reasonsJson.contains("barcode_match"))
    }

    @Test
    fun auto_commit_requires_high_score_and_gap() {
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
