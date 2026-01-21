package com.zak.pressmark.data.repository

import com.zak.pressmark.data.model.inbox.InboxErrorCode
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class InboxPipelineBackoffTest {
    @Test
    fun `backoff grows with retries`() {
        val base = InboxPipeline.computeBackoffMillis(
            errorCode = InboxErrorCode.API_ERROR,
            retryCount = 0,
            random = Random(0),
        )
        val later = InboxPipeline.computeBackoffMillis(
            errorCode = InboxErrorCode.API_ERROR,
            retryCount = 3,
            random = Random(0),
        )

        assertTrue(later > base)
    }
}
