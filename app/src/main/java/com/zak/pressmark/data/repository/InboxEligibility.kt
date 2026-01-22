package com.zak.pressmark.data.repository

import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.model.inbox.OcrStatus
import com.zak.pressmark.data.model.inbox.ReasonCode

object InboxEligibility {
    private const val DEFAULT_CONFIDENCE_THRESHOLD = 70

    fun isOcrEligible(item: InboxItemEntity, now: Long): Boolean {
        return item.ocrStatus == OcrStatus.NOT_STARTED &&
            item.photoUris.isNotEmpty() &&
            (item.nextOcrAt ?: 0L) <= now
    }

    fun isLookupEligible(item: InboxItemEntity, now: Long): Boolean {
        val signalTitle = item.extractedTitle ?: item.rawTitle
        val signalArtist = item.extractedArtist ?: item.rawArtist
        val signalCatalogNo = item.extractedCatalogNo
        val signalBarcode = item.barcode

        val hasSignal = !signalBarcode.isNullOrBlank() ||
            !signalCatalogNo.isNullOrBlank() ||
            (!signalTitle.isNullOrBlank() && !signalArtist.isNullOrBlank()) ||
            (item.sourceType == com.zak.pressmark.data.model.inbox.InboxSourceType.COVER_PHOTO &&
                !signalTitle.isNullOrBlank())

        return item.lookupStatus == LookupStatus.PENDING &&
            hasSignal &&
            (item.nextLookupAt ?: 0L) <= now
    }

    fun isNeedsReview(item: InboxItemEntity): Boolean {
        if (item.isUnknown) return true

        val title = item.extractedTitle ?: item.rawTitle
        val artist = item.extractedArtist ?: item.rawArtist
        if (title.isNullOrBlank() || artist.isNullOrBlank()) return true

        val confidence = item.confidenceScore
        if (confidence != null && confidence < DEFAULT_CONFIDENCE_THRESHOLD) return true

        val reasons = ReasonCode.decode(item.confidenceReasonsJson)
        val reviewReasons = setOf(
            ReasonCode.LOW_SIGNAL,
            ReasonCode.MULTIPLE_CANDIDATES,
            ReasonCode.MISSING_TITLE,
            ReasonCode.MISSING_ARTIST,
            ReasonCode.WEAK_MATCH_TITLE,
            ReasonCode.WEAK_MATCH_ARTIST,
            ReasonCode.NO_API_MATCH,
        )
        return reasons.any { it in reviewReasons }
    }
}
