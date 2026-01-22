package com.zak.pressmark.data.repository

import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.model.inbox.OcrStatus

object InboxEligibility {
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
}
