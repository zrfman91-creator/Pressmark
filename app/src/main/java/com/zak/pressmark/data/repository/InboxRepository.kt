package com.zak.pressmark.data.repository

import android.net.Uri
import com.zak.pressmark.data.local.dao.InboxItemDao
import com.zak.pressmark.data.local.dao.ProviderSnapshotDao
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.local.entity.ProviderSnapshotEntity
import com.zak.pressmark.data.model.inbox.CandidateScore
import com.zak.pressmark.data.model.inbox.InboxErrorCode
import com.zak.pressmark.data.model.inbox.InboxSourceType
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.model.inbox.OcrStatus
import com.zak.pressmark.data.model.inbox.ProviderCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface InboxRepository {
    fun observeInboxCount(): Flow<Int>
    fun observeInboxItems(): Flow<List<InboxItemEntity>>
    fun observeInboxItem(inboxItemId: String): Flow<InboxItemEntity?>
    fun observeTopCandidates(inboxItemId: String): Flow<List<ProviderSnapshotEntity>>

    suspend fun createQuickAdd(title: String, artist: String): String
    suspend fun createBarcode(barcode: String): String
    suspend fun createCoverCapture(photoUri: Uri): String

    suspend fun markOcrInProgress(inboxItemId: String)
    suspend fun applyOcrResult(
        inboxItemId: String,
        extractedFields: ExtractedFields,
        success: Boolean,
    )

    suspend fun applyLookupResults(
        inboxItemId: String,
        candidates: List<ProviderCandidate>,
        errorCode: InboxErrorCode,
    )

    suspend fun markCommitted(inboxItemId: String)
    suspend fun retryLookup(inboxItemId: String)
}

data class ExtractedFields(
    val title: String?,
    val artist: String?,
    val label: String?,
    val catalogNo: String?,
)

class DefaultInboxRepository(
    private val inboxItemDao: InboxItemDao,
    private val providerSnapshotDao: ProviderSnapshotDao,
) : InboxRepository {
    override fun observeInboxCount(): Flow<Int> = inboxItemDao.observeInboxCount()

    override fun observeInboxItems(): Flow<List<InboxItemEntity>> = inboxItemDao.observeAll()

    override fun observeInboxItem(inboxItemId: String): Flow<InboxItemEntity?> {
        return inboxItemDao.observeById(inboxItemId)
    }

    override fun observeTopCandidates(inboxItemId: String): Flow<List<ProviderSnapshotEntity>> {
        return providerSnapshotDao.observeForInboxItem(inboxItemId)
            .map { it.take(3) }
    }

    override suspend fun createQuickAdd(title: String, artist: String): String {
        return createInboxItem(
            sourceType = InboxSourceType.QUICK_ADD,
            rawTitle = title,
            rawArtist = artist,
            barcode = null,
            photoUris = emptyList(),
        )
    }

    override suspend fun createBarcode(barcode: String): String {
        return createInboxItem(
            sourceType = InboxSourceType.BARCODE,
            rawTitle = null,
            rawArtist = null,
            barcode = barcode,
            photoUris = emptyList(),
        )
    }

    override suspend fun createCoverCapture(photoUri: Uri): String {
        return createInboxItem(
            sourceType = InboxSourceType.COVER_PHOTO,
            rawTitle = null,
            rawArtist = null,
            barcode = null,
            photoUris = listOf(photoUri.toString()),
            ocrStatus = OcrStatus.NOT_STARTED,
            lookupStatus = LookupStatus.NOT_ELIGIBLE,
            nextOcrAt = System.currentTimeMillis(),
        )
    }

    override suspend fun markOcrInProgress(inboxItemId: String) {
        val item = inboxItemDao.getById(inboxItemId) ?: return
        inboxItemDao.update(
            item.copy(
                ocrStatus = OcrStatus.IN_PROGRESS,
                updatedAt = System.currentTimeMillis(),
                lastTriedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun applyOcrResult(
        inboxItemId: String,
        extractedFields: ExtractedFields,
        success: Boolean,
    ) {
        val item = inboxItemDao.getById(inboxItemId) ?: return
        val now = System.currentTimeMillis()
        val nextLookup = if (success) now else null
        val lookupStatus = if (success && InboxEligibility.isLookupEligible(item.copy(
                extractedTitle = extractedFields.title,
                extractedArtist = extractedFields.artist,
                extractedLabel = extractedFields.label,
                extractedCatalogNo = extractedFields.catalogNo,
                lookupStatus = LookupStatus.PENDING,
                nextLookupAt = nextLookup,
            ), now)
        ) {
            LookupStatus.PENDING
        } else {
            LookupStatus.NOT_ELIGIBLE
        }

        inboxItemDao.update(
            item.copy(
                updatedAt = now,
                ocrStatus = if (success) OcrStatus.DONE else OcrStatus.FAILED,
                errorCode = if (success) InboxErrorCode.NONE else InboxErrorCode.API_ERROR,
                extractedTitle = extractedFields.title,
                extractedArtist = extractedFields.artist,
                extractedLabel = extractedFields.label,
                extractedCatalogNo = extractedFields.catalogNo,
                lookupStatus = lookupStatus,
                nextLookupAt = nextLookup,
            )
        )
    }

    override suspend fun applyLookupResults(
        inboxItemId: String,
        candidates: List<ProviderCandidate>,
        errorCode: InboxErrorCode,
    ) {
        val item = inboxItemDao.getById(inboxItemId) ?: return
        val now = System.currentTimeMillis()
        providerSnapshotDao.deleteForInboxItem(inboxItemId)

        if (candidates.isEmpty()) {
            val nextAt = now + InboxPipeline.computeBackoffMillis(errorCode, item.retryCount)
            inboxItemDao.update(
                item.copy(
                    updatedAt = now,
                    lookupStatus = LookupStatus.FAILED,
                    errorCode = errorCode,
                    retryCount = item.retryCount + 1,
                    nextLookupAt = nextAt,
                )
            )
            return
        }

        val snapshots = candidates.map { candidate ->
            val score = InboxPipeline.scoreCandidate(
                queryTitle = item.extractedTitle ?: item.rawTitle,
                queryArtist = item.extractedArtist ?: item.rawArtist,
                queryCatalogNo = item.extractedCatalogNo,
                queryBarcode = item.barcode,
                candidate = candidate,
            )
            buildSnapshot(item.id, candidate, score)
        }.sortedByDescending { it.confidence }

        providerSnapshotDao.insertAll(snapshots)

        val top = snapshots.first()
        val status = if (top.confidence >= 85) LookupStatus.COMMITTED else LookupStatus.NEEDS_REVIEW

        inboxItemDao.update(
            item.copy(
                updatedAt = now,
                lookupStatus = status,
                errorCode = InboxErrorCode.NONE,
                confidence = top.confidence,
                reasonsJson = top.reasonsJson,
                nextLookupAt = null,
            )
        )
    }

    override suspend fun markCommitted(inboxItemId: String) {
        val item = inboxItemDao.getById(inboxItemId) ?: return
        inboxItemDao.update(
            item.copy(
                updatedAt = System.currentTimeMillis(),
                lookupStatus = LookupStatus.COMMITTED,
            )
        )
    }

    override suspend fun retryLookup(inboxItemId: String) {
        val item = inboxItemDao.getById(inboxItemId) ?: return
        inboxItemDao.update(
            item.copy(
                updatedAt = System.currentTimeMillis(),
                lookupStatus = LookupStatus.PENDING,
                nextLookupAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun createInboxItem(
        sourceType: InboxSourceType,
        rawTitle: String?,
        rawArtist: String?,
        barcode: String?,
        photoUris: List<String>,
        ocrStatus: OcrStatus = OcrStatus.NOT_STARTED,
        lookupStatus: LookupStatus = LookupStatus.PENDING,
        nextOcrAt: Long? = null,
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val item = InboxItemEntity(
            id = id,
            sourceType = sourceType,
            createdAt = now,
            updatedAt = now,
            barcode = barcode,
            rawTitle = rawTitle,
            rawArtist = rawArtist,
            rawRowJson = null,
            photoUris = photoUris,
            ocrStatus = ocrStatus,
            lookupStatus = lookupStatus,
            errorCode = InboxErrorCode.NONE,
            retryCount = 0,
            nextOcrAt = nextOcrAt,
            nextLookupAt = if (lookupStatus == LookupStatus.PENDING) now else null,
            lastTriedAt = null,
            extractedTitle = null,
            extractedArtist = null,
            extractedLabel = null,
            extractedCatalogNo = null,
            confidence = null,
            reasonsJson = null,
            wasUndone = false,
        )
        inboxItemDao.upsert(item)
        return id
    }

    private fun buildSnapshot(
        inboxItemId: String,
        candidate: ProviderCandidate,
        score: CandidateScore,
    ): ProviderSnapshotEntity {
        return ProviderSnapshotEntity(
            id = UUID.randomUUID().toString(),
            inboxItemId = inboxItemId,
            provider = candidate.provider,
            providerItemId = candidate.providerItemId,
            title = candidate.title,
            artist = candidate.artist,
            label = candidate.label,
            catalogNo = candidate.catalogNo,
            barcode = candidate.barcode,
            rawJson = candidate.rawJson,
            confidence = score.confidence,
            reasonsJson = score.reasonsJson,
        )
    }
}
