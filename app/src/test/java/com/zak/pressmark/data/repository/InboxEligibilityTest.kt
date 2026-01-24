package com.zak.pressmark.data.repository

import com.zak.pressmark.data.local.entity.v1.InboxItemEntity
import com.zak.pressmark.data.model.inbox.InboxErrorCode
import com.zak.pressmark.data.model.inbox.InboxSourceType
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.model.inbox.OcrStatus
import com.zak.pressmark.data.repository.v1.InboxEligibility
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxEligibilityTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `ocr eligible when photo present and next time reached`() {
        val item = baseItem(
            ocrStatus = OcrStatus.NOT_STARTED,
            photoUris = listOf("content://photo"),
            nextOcrAt = now,
        )

        assertTrue(InboxEligibility.isOcrEligible(item, now))
    }

    @Test
    fun `ocr not eligible without photo`() {
        val item = baseItem(
            ocrStatus = OcrStatus.NOT_STARTED,
            photoUris = emptyList(),
            nextOcrAt = now,
        )

        assertFalse(InboxEligibility.isOcrEligible(item, now))
    }

    @Test
    fun `lookup eligible requires signals`() {
        val item = baseItem(
            lookupStatus = LookupStatus.PENDING,
            rawTitle = "Title",
            rawArtist = "Artist",
            nextLookupAt = now,
        )

        assertTrue(InboxEligibility.isLookupEligible(item, now))
    }

    @Test
    fun `lookup not eligible without signals`() {
        val item = baseItem(
            lookupStatus = LookupStatus.PENDING,
            nextLookupAt = now,
        )

        assertFalse(InboxEligibility.isLookupEligible(item, now))
    }

    private fun baseItem(
        ocrStatus: OcrStatus = OcrStatus.NOT_STARTED,
        lookupStatus: LookupStatus = LookupStatus.NOT_ELIGIBLE,
        rawTitle: String? = null,
        rawArtist: String? = null,
        photoUris: List<String> = emptyList(),
        nextOcrAt: Long? = null,
        nextLookupAt: Long? = null,
    ): InboxItemEntity {
        return InboxItemEntity(
            id = "id",
            sourceType = InboxSourceType.QUICK_ADD,
            createdAt = now,
            updatedAt = now,
            barcode = null,
            rawTitle = rawTitle,
            rawArtist = rawArtist,
            rawRowJson = null,
            photoUris = photoUris,
            ocrStatus = ocrStatus,
            lookupStatus = lookupStatus,
            errorCode = InboxErrorCode.NONE,
            retryCount = 0,
            nextOcrAt = nextOcrAt,
            nextLookupAt = nextLookupAt,
            lastTriedAt = null,
            extractedTitle = null,
            extractedArtist = null,
            extractedLabel = null,
            extractedCatalogNo = null,
            confidence = null,
            reasonsJson = null,
            wasUndone = false,
            committedProviderItemId = null,
        )
    }
}
