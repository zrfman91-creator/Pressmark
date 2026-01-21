package com.zak.pressmark.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object InboxPipelineScheduler {
    private const val OCR_WORK = "ocr_drain_work"
    private const val LOOKUP_WORK = "lookup_drain_work"
    private const val SYNC_WORK = "sync_outbox_work"

    fun enqueueOcrDrain(context: Context) {
        val request = OneTimeWorkRequestBuilder<OcrDrainWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            OCR_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueLookupDrain(context: Context) {
        val request = OneTimeWorkRequestBuilder<LookupDrainWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            LOOKUP_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueSyncOutbox(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncOutboxWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
