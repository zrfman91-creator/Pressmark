package com.zak.pressmark.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zak.pressmark.data.local.db.DatabaseProvider
import com.zak.pressmark.data.model.inbox.OcrStatus
import com.zak.pressmark.data.repository.ExtractedFields
import com.zak.pressmark.data.repository.OcrParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OcrDrainWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = DatabaseProvider.get(applicationContext)
        val dao = db.inboxItemDao()
        val repo = PipelineDependencies.inboxRepository(applicationContext)
        val extractor = PipelineDependencies.textExtractor(applicationContext)

        val now = System.currentTimeMillis()
        val items = dao.fetchOcrEligible(
            status = OcrStatus.NOT_STARTED,
            now = now,
            limit = 10,
        )

        for (item in items) {
            repo.markOcrInProgress(item.id)
            val uri = item.photoUris.firstOrNull() ?: continue
            val result = extractor.extract(android.net.Uri.parse(uri))
            if (result.isSuccess) {
                val ocrResult = result.getOrThrow()
                val extracted = OcrParser.parse(ocrResult.lines)
                repo.applyOcrResult(item.id, extracted, success = true)
            } else {
                repo.applyOcrResult(
                    item.id,
                    ExtractedFields(null, null, null, null),
                    success = false,
                )
            }
        }

        Result.success()
    }
}
