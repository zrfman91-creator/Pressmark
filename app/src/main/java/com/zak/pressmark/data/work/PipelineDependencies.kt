package com.zak.pressmark.data.work

import android.content.Context
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.core.ocr.MlKitTextExtractor
import com.zak.pressmark.core.ocr.TextExtractor
import com.zak.pressmark.data.local.db.DatabaseProvider
import com.zak.pressmark.data.remote.discogs.DiscogsApiProvider
import com.zak.pressmark.data.remote.http.HttpClients
import com.zak.pressmark.data.remote.provider.DiscogsMetadataProvider
import com.zak.pressmark.data.remote.provider.MetadataProvider
import com.zak.pressmark.data.repository.CatalogRepository
import com.zak.pressmark.data.repository.DefaultInboxRepository
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.IngestSettingsRepository
import com.zak.pressmark.data.repository.ReleaseRepository

object PipelineDependencies {
    @Volatile
    private var inboxRepository: InboxRepository? = null

    @Volatile
    private var textExtractor: TextExtractor? = null

    @Volatile
    private var metadataProvider: MetadataProvider? = null

    @Volatile
    private var ingestSettingsRepository: IngestSettingsRepository? = null

    @Volatile
    private var releaseRepository: ReleaseRepository? = null

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

    fun metadataProvider(context: Context): MetadataProvider {
        return metadataProvider ?: synchronized(this) {
            metadataProvider ?: buildMetadataProvider(context).also { metadataProvider = it }
        }
    }

    fun ingestSettings(context: Context): IngestSettingsRepository {
        return ingestSettingsRepository ?: synchronized(this) {
            ingestSettingsRepository ?: IngestSettingsRepository(context.applicationContext)
                .also { ingestSettingsRepository = it }
        }
    }

    fun releaseRepository(context: Context): ReleaseRepository {
        return releaseRepository ?: synchronized(this) {
            releaseRepository ?: buildReleaseRepository(context).also { releaseRepository = it }
        }
    }


    private fun buildInboxRepository(context: Context): InboxRepository {
        val db = DatabaseProvider.get(context.applicationContext)
        return DefaultInboxRepository(
            inboxItemDao = db.inboxItemDao(),
            providerSnapshotDao = db.providerSnapshotDao(),
            evidenceArtifactDao = db.evidenceArtifactDao(),
            verificationEventDao = db.verificationEventDao(),
            catalogItemPressingDao = db.catalogItemPressingDao(),
        )
    }

    private fun buildMetadataProvider(context: Context): MetadataProvider {
        val api = DiscogsApiProvider.create(
            token = BuildConfig.DISCOGS_TOKEN.trim(),
            userAgent = "Pressmark/${BuildConfig.VERSION_NAME}",
            baseClient = HttpClients.cached(context.applicationContext),
        )
        return DiscogsMetadataProvider(api)
    }

    private fun buildReleaseRepository(context: Context): ReleaseRepository {
        val db = DatabaseProvider.get(context.applicationContext)
        val api = DiscogsApiProvider.create(
            token = BuildConfig.DISCOGS_TOKEN.trim(),
            userAgent = "Pressmark/${BuildConfig.VERSION_NAME}",
            baseClient = HttpClients.cached(context.applicationContext),
        )
        return ReleaseRepository(db = db, discogsApiService = api, catalogRepository = CatalogRepository(db))
    }
}
