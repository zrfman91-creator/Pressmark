// FILE: app/src/main/java/com/zak/pressmark/app/di/NetworkModule.kt
package com.zak.pressmark.app.di

import android.content.Context
import com.zak.pressmark.data.remote.http.HttpClients
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
    ): OkHttpClient {
        // Single source of truth for cache + timeouts lives in HttpClients.
        return HttpClients.base(
            context = context,
            cacheDirName = "okhttp_cache",
            cacheSizeBytes = 20L * 1024L * 1024L, // 20MB
            connectTimeoutSeconds = 10,
            readTimeoutSeconds = 20,
            writeTimeoutSeconds = 20,
            callTimeoutSeconds = 30,
        )
    }
}
