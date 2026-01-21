package com.zak.pressmark.data.work

import android.content.Context
import com.zak.pressmark.core.ocr.MlKitTextExtractor
import com.zak.pressmark.core.ocr.TextExtractor
import com.zak.pressmark.data.local.db.DatabaseProvider
import com.zak.pressmark.data.remote.provider.DiscogsProviderStub
import com.zak.pressmark.data.remote.provider.MetadataProvider
import com.zak.pressmark.data.repository.DefaultInboxRepository
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.IngestSettingsRepository

object PipelineDependencies {
    @Volatile
    private var inboxRepository: InboxRepository? = null

    @Volatile
    private var textExtractor: TextExtractor? = null

    @Volatile
    private var metadataProvider: MetadataProvider? = null

    @Volatile
    private var ingestSettingsRepository: IngestSettingsRepository? = null

    fun inboxRepository(context: Context): InboxRepository {
        return inboxRepository ?: synchronized(this) {
            inboxRepository ?: buildInboxRepository(context).also { inboxRepository = it }
        }
    }

    fun textExtractor(context: Context): TextExtractor {
        return textExtractor ?: synchronized(this) {
            textExtractor ?: MlKitTextExtractor(context.applicationContext).also { textExtractor = it }
        }
    }

    fun metadataProvider(): MetadataProvider {
        return metadataProvider ?: synchronized(this) {
            metadataProvider ?: DiscogsProviderStub().also { metadataProvider = it }
        }
    }

    fun ingestSettings(context: Context): IngestSettingsRepository {
        return ingestSettingsRepository ?: synchronized(this) {
            ingestSettingsRepository ?: IngestSettingsRepository(context.applicationContext)
                .also { ingestSettingsRepository = it }
        }
    }

    private fun buildInboxRepository(context: Context): InboxRepository {
        val db = DatabaseProvider.get(context.applicationContext)
        return DefaultInboxRepository(
            inboxItemDao = db.inboxItemDao(),
            providerSnapshotDao = db.providerSnapshotDao(),
        )
    }
}
