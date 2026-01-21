package com.zak.pressmark.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zak.pressmark.core.ocr.TextExtractor
import com.zak.pressmark.data.local.dao.InboxItemDao
import com.zak.pressmark.data.model.inbox.OcrStatus
import com.zak.pressmark.data.repository.ExtractedFields
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.OcrParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OcrDrainWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val inboxItemDao: InboxItemDao,
    private val inboxRepository: InboxRepository,
    private val textExtractor: TextExtractor,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val items = inboxItemDao.fetchOcrEligible(
            status = OcrStatus.NOT_STARTED,
            now = now,
            limit = 10,
        )

        for (item in items) {
            inboxRepository.markOcrInProgress(item.id)
            val uri = item.photoUris.firstOrNull() ?: continue
            val result = textExtractor.extract(android.net.Uri.parse(uri))
            if (result.isSuccess) {
                val ocrResult = result.getOrThrow()
                val extracted = OcrParser.parse(ocrResult.lines)
                inboxRepository.applyOcrResult(item.id, extracted, success = true)
            } else {
                inboxRepository.applyOcrResult(
                    item.id,
                    ExtractedFields(null, null, null, null),
                    success = false,
                )
            }
        }

        Result.success()
    }
}
