package com.zak.pressmark.data.repository

import android.net.Uri
import com.zak.pressmark.data.local.dao.InboxItemDao
import com.zak.pressmark.data.local.dao.ProviderSnapshotDao
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.local.entity.ProviderSnapshotEntity
import com.zak.pressmark.data.model.inbox.CandidateScore
import com.zak.pressmark.data.model.inbox.CsvImportRow
import com.zak.pressmark.data.model.inbox.CsvImportSummary
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
    ): ProviderCandidate?

    suspend fun markCommitted(
        inboxItemId: String,
        committedProviderItemId: String?,
    )
    suspend fun retryLookup(inboxItemId: String)

    suspend fun updateManualDetails(
        inboxItemId: String,
        title: String?,
        artist: String?,
        label: String?,
        catalogNo: String?,
        format: String?,
    )

    suspend fun createCsvImport(rows: List<CsvImportRow>): CsvImportSummary
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
        val lookupStatus = if (success && InboxEligibility.isLookupEligible(
                item.copy(
                    extractedTitle = extractedFields.title,
                    extractedArtist = extractedFields.artist,
                    extractedLabel = extractedFields.label,
                    extractedCatalogNo = extractedFields.catalogNo,
                    lookupStatus = LookupStatus.PENDING,
                    nextLookupAt = nextLookup,
                ),
                now
            )
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
    ): ProviderCandidate? {
        val item = inboxItemDao.getById(inboxItemId) ?: return null
        val now = System.currentTimeMillis()
        providerSnapshotDao.deleteForInboxItem(inboxItemId)

        if (candidates.isEmpty()) {
            val shouldRetry = errorCode != InboxErrorCode.NO_MATCH
            val nextAt = if (shouldRetry) {
                now + InboxPipeline.computeBackoffMillis(errorCode, item.retryCount)
            } else {
                null
            }
            inboxItemDao.update(
                item.copy(
                    updatedAt = now,
                    lookupStatus = if (shouldRetry) LookupStatus.PENDING else LookupStatus.FAILED,
                    errorCode = errorCode,
                    retryCount = item.retryCount + 1,
                    nextLookupAt = nextAt,
                )
            )
            return null
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
        val second = snapshots.getOrNull(1)
        val shouldAutoCommit = InboxPipeline.shouldAutoCommit(
            topScore = top.confidence,
            secondScore = second?.confidence,
            wasUndone = item.wasUndone,
        )
        val status = LookupStatus.NEEDS_REVIEW

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
        return if (shouldAutoCommit) {
            candidates.firstOrNull { it.providerItemId == top.providerItemId }
        } else {
            null
        }
    }

    override suspend fun markCommitted(
        inboxItemId: String,
        committedProviderItemId: String?,
    ) {
        val item = inboxItemDao.getById(inboxItemId) ?: return
        inboxItemDao.update(
            item.copy(
                updatedAt = System.currentTimeMillis(),
                lookupStatus = LookupStatus.COMMITTED,
                committedProviderItemId = committedProviderItemId,
                errorCode = InboxErrorCode.NONE,
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

    override suspend fun updateManualDetails(
        inboxItemId: String,
        title: String?,
        artist: String?,
        label: String?,
        catalogNo: String?,
        format: String?,
    ) {
        val item = inboxItemDao.getById(inboxItemId) ?: return
        val now = System.currentTimeMillis()
        providerSnapshotDao.deleteForInboxItem(inboxItemId)
        val normalizedFormat = format?.trim()?.takeIf { it.isNotBlank() }
        val rawJson = if (item.rawRowJson == null && normalizedFormat != null) {
            org.json.JSONObject()
                .put("title", title)
                .put("artist", artist)
                .put("label", label)
                .put("catalogNo", catalogNo)
                .put("format", normalizedFormat)
                .toString()
        } else {
            item.rawRowJson
        }
        inboxItemDao.update(
            item.copy(
                updatedAt = now,
                rawTitle = title?.trim()?.takeIf { it.isNotBlank() },
                rawArtist = artist?.trim()?.takeIf { it.isNotBlank() },
                extractedLabel = label?.trim()?.takeIf { it.isNotBlank() },
                extractedCatalogNo = catalogNo?.trim()?.takeIf { it.isNotBlank() },
                rawRowJson = rawJson,
                lookupStatus = LookupStatus.PENDING,
                nextLookupAt = now,
                errorCode = InboxErrorCode.NONE,
                confidence = null,
                reasonsJson = null,
            )
        )
    }

    override suspend fun createCsvImport(rows: List<CsvImportRow>): CsvImportSummary {
        var imported = 0
        rows.forEach { row ->
            val title = row.title?.trim()?.takeIf { it.isNotBlank() }
            val artist = row.artist?.trim()?.takeIf { it.isNotBlank() }
            val barcode = row.barcode?.trim()?.takeIf { it.isNotBlank() }
            val catalogNo = row.catalogNo?.trim()?.takeIf { it.isNotBlank() }
            val label = row.label?.trim()?.takeIf { it.isNotBlank() }

            if (title == null && artist == null && barcode == null && catalogNo == null) {
                return@forEach
            }

            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val item = InboxItemEntity(
                id = id,
                sourceType = InboxSourceType.CSV_IMPORT,
                createdAt = now,
                updatedAt = now,
                barcode = barcode,
                rawTitle = title,
                rawArtist = artist,
                rawRowJson = row.rawJson,
                photoUris = emptyList(),
                ocrStatus = OcrStatus.NOT_STARTED,
                lookupStatus = LookupStatus.PENDING,
                errorCode = InboxErrorCode.NONE,
                retryCount = 0,
                nextOcrAt = null,
                nextLookupAt = now,
                lastTriedAt = null,
                extractedTitle = null,
                extractedArtist = null,
                extractedLabel = label,
                extractedCatalogNo = catalogNo,
                confidence = null,
                reasonsJson = null,
                wasUndone = false,
                committedProviderItemId = null,
            )
            inboxItemDao.upsert(item)
            imported += 1
        }

        return CsvImportSummary(
            totalRows = rows.size,
            importedRows = imported,
            skippedRows = rows.size - imported,
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
            committedProviderItemId = null,
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
