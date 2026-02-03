package com.zak.pressmark.app.di

import com.zak.pressmark.data.remote.discogs.DiscogsApiProvider
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiscogsModule {

    /**
     * Single source of truth for Discogs API service construction.
     *
     * IMPORTANT:
     * - Do NOT build Retrofit here.
     * - Do NOT build a Discogs-specific OkHttpClient here.
     * - Reuse the app-wide OkHttpClient and let DiscogsApiProvider add Discogs headers/retry.
     */
    @Provides
    @Singleton
    fun provideDiscogsApiService(
        baseOkHttpClient: OkHttpClient,
    ): DiscogsApiService {
        return DiscogsApiProvider.create(
            baseClient = baseOkHttpClient,
        )
    }
}
