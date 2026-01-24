// FILE: app/src/main/java/com/zak/pressmark/app/di/NetworkModule.kt
package com.zak.pressmark.app.di

import android.content.Context
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.data.remote.discogs.DiscogsApiProvider
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
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

    @Provides
    @Singleton
    fun provideDiscogsApiService(
        okHttpClient: OkHttpClient,
        @AppUserAgent userAgent: String,
    ): DiscogsApiService {
        return DiscogsApiProvider.create(
            token = BuildConfig.DISCOGS_TOKEN.trim(),
            userAgent = userAgent,
            baseClient = okHttpClient,
        )
    }
}
