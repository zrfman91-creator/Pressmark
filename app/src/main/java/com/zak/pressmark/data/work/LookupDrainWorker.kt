package com.zak.pressmark.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zak.pressmark.data.local.dao.InboxItemDao
import com.zak.pressmark.data.model.inbox.InboxErrorCode
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.remote.provider.MetadataProvider
import com.zak.pressmark.data.repository.InboxEligibility
import com.zak.pressmark.data.repository.InboxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

@HiltWorker
class LookupDrainWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val inboxItemDao: InboxItemDao,
    private val inboxRepository: InboxRepository,
    private val metadataProvider: MetadataProvider,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val items = inboxItemDao.fetchLookupEligible(
            status = LookupStatus.PENDING,
            now = now,
            limit = 10,
        )

        for (item in items) {
            if (!InboxEligibility.isLookupEligible(item, now)) continue
            inboxItemDao.update(item.copy(lookupStatus = LookupStatus.IN_PROGRESS, lastTriedAt = now))

            val title = item.extractedTitle ?: item.rawTitle
            val artist = item.extractedArtist ?: item.rawArtist
            val catalogNo = item.extractedCatalogNo
            val barcode = item.barcode

            val candidates = runCatching {
                when {
                    !barcode.isNullOrBlank() -> metadataProvider.lookupByBarcode(barcode)
                    !catalogNo.isNullOrBlank() ->
                        metadataProvider.lookupByCatalogNo(catalogNo, item.extractedLabel)
                    !title.isNullOrBlank() && !artist.isNullOrBlank() ->
                        metadataProvider.searchByTitleArtist(title, artist)
                    else -> emptyList()
                }
            }

            if (candidates.isFailure) {
                val errorCode = mapError(candidates.exceptionOrNull())
                inboxRepository.applyLookupResults(item.id, emptyList(), errorCode)
                if (errorCode == InboxErrorCode.RATE_LIMIT) break
            } else {
                val list = candidates.getOrThrow()
                val errorCode = if (list.isEmpty()) InboxErrorCode.NO_MATCH else InboxErrorCode.NONE
                inboxRepository.applyLookupResults(item.id, list, errorCode)
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
