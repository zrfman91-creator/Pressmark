package com.zak.pressmark.app.di

import com.zak.pressmark.core.analytics.LogcatUxEventLogger
import com.zak.pressmark.core.analytics.UxEventLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideUxEventLogger(): UxEventLogger = LogcatUxEventLogger()
}
