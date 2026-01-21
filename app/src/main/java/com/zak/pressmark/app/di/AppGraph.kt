// FILE: app/src/main/java/com/zak/pressmark/app/di/AppGraph.kt
package com.zak.pressmark.app.di

import android.content.Context
import coil3.ImageLoader
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.core.util.AppImageLoader
import com.zak.pressmark.data.local.db.AppDatabase
import com.zak.pressmark.data.local.db.DatabaseProvider
import com.zak.pressmark.data.remote.discogs.DiscogsApiProvider
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.remote.musicbrainz.DefaultMusicBrainzArtworkRepository
import com.zak.pressmark.data.remote.musicbrainz.MusicBrainzArtworkApi
import com.zak.pressmark.data.remote.musicbrainz.MusicBrainzArtworkRepository
import com.zak.pressmark.data.remote.provider.DiscogsProviderStub
import com.zak.pressmark.data.remote.provider.MetadataProvider
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository
import com.zak.pressmark.data.repository.DefaultInboxRepository
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.IngestSettingsRepository
import com.zak.pressmark.data.repository.ReleaseRepository
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manual DI container (no Hilt).
 * Owns app-wide singletons.
 */
class AppGraph(
    context: Context,
) {
    private val appContext: Context = context.applicationContext

    // --- Core Singletons ---
    private val database: AppDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DatabaseProvider.get(appContext)
    }
    private val okHttpClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        buildOkHttp(appContext)
    }
    private val appUserAgent: String by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        "Pressmark/${BuildConfig.VERSION_NAME}"   // Used by rate-limited public APIs (Discogs / MusicBrainz).
    }

    // --- API Service Singleton (keep for cover search feature) ---
    val discogsApiService: DiscogsApiService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DiscogsApiProvider.create(
            token = BuildConfig.DISCOGS_TOKEN.trim(),
            userAgent = appUserAgent,
            baseClient = okHttpClient,
        )
    }

    // --- MusicBrainz (shared OkHttp config) ---
    private val musicBrainzApi: MusicBrainzArtworkApi by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MusicBrainzArtworkApi(
            userAgent = appUserAgent,
            client = okHttpClient,
            debugLogging = BuildConfig.DEBUG,
        )
    }

    val musicBrainzArtworkRepository: MusicBrainzArtworkRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DefaultMusicBrainzArtworkRepository(musicBrainzApi)
    }

    // --- Repositories (MUST MATCH repo constructors) ---
    val albumRepository: AlbumRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        // AlbumRepository(dao: AlbumDao)
        AlbumRepository(database.albumDao())
    }

    val artistRepository: ArtistRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ArtistRepository(database.artistDao())
    }

    val releaseRepository: ReleaseRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ReleaseRepository(
            db = database,
            discogsApiService = discogsApiService,
        )
    }

    val inboxRepository: InboxRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DefaultInboxRepository(
            inboxItemDao = database.inboxItemDao(),
            providerSnapshotDao = database.providerSnapshotDao(),
        )
    }

    val metadataProvider: MetadataProvider by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DiscogsProviderStub()
    }

    val ingestSettingsRepository: IngestSettingsRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        IngestSettingsRepository(appContext)
    }

    // --- UI Layer Dependencies ---
    val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppImageLoader.get(appContext)
    }
}

private fun buildOkHttp(context: Context): OkHttpClient {
    val cacheDir = File(context.cacheDir, "okhttp_cache").apply { mkdirs() }
    val cache = Cache(cacheDir, 20L * 1024L * 1024L) // 20MB

    return OkHttpClient.Builder()
        .cache(cache)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
}
