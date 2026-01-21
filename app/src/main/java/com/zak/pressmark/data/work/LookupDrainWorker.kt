package com.zak.pressmark.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zak.pressmark.data.local.db.DatabaseProvider
import com.zak.pressmark.data.model.inbox.InboxErrorCode
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.repository.InboxEligibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class LookupDrainWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = DatabaseProvider.get(applicationContext)
        val dao = db.inboxItemDao()
        val repo = PipelineDependencies.inboxRepository(applicationContext)
        val provider = PipelineDependencies.metadataProvider()

        val now = System.currentTimeMillis()
        val items = dao.fetchLookupEligible(
            status = LookupStatus.PENDING,
            now = now,
            limit = 10,
        )

        for (item in items) {
            if (!InboxEligibility.isLookupEligible(item, now)) continue
            dao.update(item.copy(lookupStatus = LookupStatus.IN_PROGRESS, lastTriedAt = now))

            val title = item.extractedTitle ?: item.rawTitle
            val artist = item.extractedArtist ?: item.rawArtist
            val catalogNo = item.extractedCatalogNo
            val barcode = item.barcode

            val candidates = runCatching {
                when {
                    !barcode.isNullOrBlank() -> provider.lookupByBarcode(barcode)
                    !catalogNo.isNullOrBlank() -> provider.lookupByCatalogNo(catalogNo, item.extractedLabel)
                    !title.isNullOrBlank() && !artist.isNullOrBlank() -> provider.searchByTitleArtist(title, artist)
                    else -> emptyList()
                }
            }

            if (candidates.isFailure) {
                val errorCode = mapError(candidates.exceptionOrNull())
                repo.applyLookupResults(item.id, emptyList(), errorCode)
                if (errorCode == InboxErrorCode.RATE_LIMIT) break
            } else {
                val list = candidates.getOrThrow()
                val errorCode = if (list.isEmpty()) InboxErrorCode.NO_MATCH else InboxErrorCode.NONE
                repo.applyLookupResults(item.id, list, errorCode)
            }
        }

        Result.success()
    }

    private fun mapError(error: Throwable?): InboxErrorCode {
        return when (error) {
            is IOException -> InboxErrorCode.OFFLINE
            is RateLimitException -> InboxErrorCode.RATE_LIMIT
            else -> InboxErrorCode.API_ERROR
        }
    }
}

class RateLimitException(message: String) : RuntimeException(message)
