// FILE: app/src/main/java/com/zak/pressmark/app/di/AppBuildInfoModule.kt
package com.zak.pressmark.app.di

import com.zak.pressmark.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppUserAgent

@Module
@InstallIn(SingletonComponent::class)
object AppBuildInfoModule {

    @Provides
    @Singleton
    @AppUserAgent
    fun provideAppUserAgent(): String {
        return "Pressmark/${BuildConfig.VERSION_NAME}"
    }
}
