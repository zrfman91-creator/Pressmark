package com.zak.pressmark.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zak.pressmark.data.local.db.v2.DatabaseProviderV2
import com.zak.pressmark.data.model.inbox.OcrStatus
import com.zak.pressmark.data.repository.v1.ExtractedFields
import com.zak.pressmark.data.repository.v1.OcrParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OcrDrainWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = DatabaseProviderV2.get(applicationContext)
        val dao = db.inboxItemDao()
        val repo = PipelineDependencies.inboxRepository(applicationContext)
        val extractor = PipelineDependencies.textExtractor(applicationContext)

        val now = System.currentTimeMillis()
        val items = dao.fetchOcrEligible(
            status = OcrStatus.NOT_STARTED,
            now = now,
            limit = 10,
        )

        var shouldScheduleLookup = false
        for (item in items) {
            repo.markOcrInProgress(item.id)
            val uri = item.photoUris.firstOrNull()
            if (uri == null) {
                repo.applyOcrResult(
                    item.id,
                    ExtractedFields(null, null, null, null),
                    success = false,
                )
                continue
            }
            val result = extractor.extract(android.net.Uri.parse(uri))
            if (result.isSuccess) {
                val ocrResult = result.getOrThrow()
                val extracted = OcrParser.parse(ocrResult.lines)
                repo.applyOcrResult(item.id, extracted, success = true)
                shouldScheduleLookup = true
            } else {
                repo.applyOcrResult(
                    item.id,
                    ExtractedFields(null, null, null, null),
                    success = false,
                )
            }
        }

        if (shouldScheduleLookup) {
            InboxPipelineScheduler.enqueueLookupDrain(applicationContext)
        }

        val remaining = dao.fetchOcrEligible(
            status = OcrStatus.NOT_STARTED,
            now = System.currentTimeMillis(),
            limit = 1,
        )
        if (remaining.isNotEmpty()) {
            InboxPipelineScheduler.enqueueOcrDrain(applicationContext)
        }

        Result.success()
    }
}
