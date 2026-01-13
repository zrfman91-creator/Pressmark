package com.zak.pressmark.app.di

import android.content.Context
import coil3.ImageLoader
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.data.local.db.AppDatabase
import com.zak.pressmark.data.local.db.DatabaseProvider
import com.zak.pressmark.data.remote.discogs.DiscogsApiProvider // Assuming you have a provider for the API
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository // Assuming you have this
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

    // --- API Service Singleton ---
    // Create the Discogs API service here to be injected into the repository
    internal val discogsApiService: DiscogsApiService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        // We assume you have a provider like this. If not, the DiscogsClient.create can be used.
        // This is cleaner as it centralizes API creation.
        DiscogsApiProvider.create(
            token = BuildConfig.DISCOGS_TOKEN.trim(),
            baseClient = okHttpClient,
        )
    }

    // --- Repositories ---
    val albumRepository: AlbumRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        // *** THE FIX IS HERE ***
        // Provide all four required dependencies to the constructor.
        AlbumRepository(
            albumDao = database.albumDao(),
            artistDao = database.artistDao(),
            genreDao = database.genreDao(),
            discogsApi = discogsApiService
        )
    }

    // Assuming you have an ArtistRepository, it would be created like this.
    // If you don't have one yet, this can be removed.
    val artistRepository: ArtistRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ArtistRepository(database.artistDao())
    }

    // --- UI Layer Dependencies ---
    val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppImageLoader.get(appContext)
    }

    private fun userAgent(): String =
        "Pressmark/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
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

// NOTE: I've moved NoopDiscogsArtworkRepository out as it's not directly used here.
// I also created a placeholder for DiscogsApiProvider.
object DiscogsApiProvider {
    fun create(token: String, baseClient: OkHttpClient): DiscogsApiService {
        // This should contain your Retrofit client creation logic
        // For example:
        // return Retrofit.Builder()
        //     .client(baseClient)
        //     .baseUrl("https://api.discogs.com/")
        //     .addConverterFactory(GsonConverterFactory.create())
        //     .build()
        //     .create(DiscogsApiService::class.java)

        // Using a placeholder to satisfy the compiler for now
        // You should replace this with your actual Retrofit client implementation
        TODO("Implement actual Retrofit client creation for DiscogsApiService")
    }
}
