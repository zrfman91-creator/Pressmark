package com.zak.pressmark.app.di

import android.content.Context
import coil3.ImageLoader
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.data.local.db.AppDatabase
import com.zak.pressmark.data.local.db.DatabaseProvider
import com.zak.pressmark.data.remote.discogs.DiscogsApiProvider
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository
import com.zak.pressmark.feature.albumlist.imageloading.AppImageLoader
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

    // --- API Service Singleton (keep for cover search feature) ---
    val discogsApiService: DiscogsApiService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DiscogsApiProvider.create(
            token = BuildConfig.DISCOGS_TOKEN.trim(),
            baseClient = okHttpClient,
        )
    }

    // --- Repositories (MUST MATCH repo constructors) ---
    val albumRepository: AlbumRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        // ✅ AlbumRepository(dao: AlbumDao)
        AlbumRepository(database.albumDao())
    }

    val artistRepository: ArtistRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ArtistRepository(database.artistDao())
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
