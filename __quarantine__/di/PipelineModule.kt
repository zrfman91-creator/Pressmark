package com.zak.pressmark.app.di

import android.content.Context
import com.zak.pressmark.core.ocr.MlKitTextExtractor
import com.zak.pressmark.core.ocr.TextExtractor
import com.zak.pressmark.data.local.dao.v1.InboxItemDao
import com.zak.pressmark.data.local.dao.v1.ProviderSnapshotDao
import com.zak.pressmark.data.local.db.v2.AppDatabaseV2
import com.zak.pressmark.data.local.db.v2.DatabaseProviderV2
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.data.local.dao.v1.CatalogItemPressingDao
import com.zak.pressmark.data.local.dao.v1.EvidenceArtifactDao
import com.zak.pressmark.data.local.dao.v1.VerificationEventDao
import com.zak.pressmark.data.remote.discogs.DiscogsApiProvider
import com.zak.pressmark.data.remote.http.HttpClients
import com.zak.pressmark.data.remote.provider.DiscogsMetadataProvider
import com.zak.pressmark.data.remote.provider.MetadataProvider
import com.zak.pressmark.data.repository.v1.CatalogSettingsRepository
import com.zak.pressmark.data.repository.v1.DefaultInboxRepository
import com.zak.pressmark.data.repository.v1.DevSettingsRepository
import com.zak.pressmark.data.repository.v1.InboxRepository
import com.zak.pressmark.data.repository.v1.IngestSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PipelineModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabaseV2 = DatabaseProviderV2.get(context)

    @Provides
    fun provideInboxItemDao(db: AppDatabaseV2): InboxItemDao = db.inboxItemDao()

    @Provides
    fun provideProviderSnapshotDao(db: AppDatabaseV2): ProviderSnapshotDao = db.providerSnapshotDao()

    @Provides
    fun provideCatalogItemPressingDao(db: AppDatabaseV2): CatalogItemPressingDao = db.catalogItemPressingDao()

    @Provides
    fun provideEvidenceArtifactDao(db: AppDatabaseV2): EvidenceArtifactDao = db.evidenceArtifactDao()

    @Provides
    fun provideVerificationEventDao(db: AppDatabaseV2): VerificationEventDao = db.verificationEventDao()

    @Provides
    @Singleton
    fun provideCatalogRepository(db: AppDatabaseV2): CatalogRepository = CatalogRepository(db)

    @Provides
    @Singleton
    fun provideInboxRepository(
        inboxItemDao: InboxItemDao,
        providerSnapshotDao: ProviderSnapshotDao,
        evidenceArtifactDao: EvidenceArtifactDao,
        verificationEventDao: VerificationEventDao,
        catalogItemPressingDao: CatalogItemPressingDao,
    ): InboxRepository = DefaultInboxRepository(
        inboxItemDao = inboxItemDao,
        providerSnapshotDao = providerSnapshotDao,
        evidenceArtifactDao = evidenceArtifactDao,
        verificationEventDao = verificationEventDao,
        catalogItemPressingDao = catalogItemPressingDao,
    )

    @Provides
    @Singleton
    fun provideTextExtractor(
        @ApplicationContext context: Context,
    ): TextExtractor = MlKitTextExtractor(context)

    @Provides
    @Singleton
    fun provideMetadataProvider(
        @ApplicationContext context: Context,
    ): MetadataProvider {
        val api = DiscogsApiProvider.create(
            token = BuildConfig.DISCOGS_TOKEN.trim(),
            userAgent = "Pressmark/${BuildConfig.VERSION_NAME}",
            baseClient = HttpClients.cached(context.applicationContext),
        )
        return DiscogsMetadataProvider(api)
    }

    @Provides
    @Singleton
    fun provideIngestSettingsRepository(
        @ApplicationContext context: Context,
    ): IngestSettingsRepository = IngestSettingsRepository(context)

    @Provides
    @Singleton
    fun provideDevSettingsRepository(
        @ApplicationContext context: Context,
    ): DevSettingsRepository = DevSettingsRepository(context)

    @Provides
    @Singleton
    fun provideCatalogSettingsRepository(
        @ApplicationContext context: Context,
    ): CatalogSettingsRepository = CatalogSettingsRepository(context)

}
