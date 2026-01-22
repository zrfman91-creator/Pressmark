package com.zak.pressmark.app.di

import android.content.Context
import com.zak.pressmark.core.ocr.MlKitTextExtractor
import com.zak.pressmark.core.ocr.TextExtractor
import com.zak.pressmark.data.local.dao.InboxItemDao
import com.zak.pressmark.data.local.dao.ProviderSnapshotDao
import com.zak.pressmark.data.local.db.AppDatabase
import com.zak.pressmark.data.local.db.DatabaseProvider
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.data.remote.discogs.DiscogsApiProvider
import com.zak.pressmark.data.remote.http.HttpClients
import com.zak.pressmark.data.remote.provider.DiscogsMetadataProvider
import com.zak.pressmark.data.remote.provider.MetadataProvider
import com.zak.pressmark.data.repository.CatalogSettingsRepository
import com.zak.pressmark.data.repository.DefaultInboxRepository
import com.zak.pressmark.data.repository.DevSettingsRepository
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.IngestSettingsRepository
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
    ): AppDatabase = DatabaseProvider.get(context)

    @Provides
    fun provideInboxItemDao(db: AppDatabase): InboxItemDao = db.inboxItemDao()

    @Provides
    fun provideProviderSnapshotDao(db: AppDatabase): ProviderSnapshotDao = db.providerSnapshotDao()

    @Provides
    @Singleton
    fun provideInboxRepository(
        inboxItemDao: InboxItemDao,
        providerSnapshotDao: ProviderSnapshotDao,
    ): InboxRepository = DefaultInboxRepository(inboxItemDao, providerSnapshotDao)

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
