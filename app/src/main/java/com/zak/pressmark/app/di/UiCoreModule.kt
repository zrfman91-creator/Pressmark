// FILE: app/src/main/java/com/zak/pressmark/app/di/UiCoreModule.kt
package com.zak.pressmark.app.di

import android.content.Context
import coil3.ImageLoader
import com.zak.pressmark.core.util.AppImageLoader
import com.zak.pressmark.core.util.ocr.MlKitTextExtractor
import com.zak.pressmark.core.util.ocr.TextExtractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UiCoreModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return AppImageLoader.get(context.applicationContext)
    }

    @Provides
    @Singleton
    fun provideTextExtractor(@ApplicationContext context: Context): TextExtractor {
        return MlKitTextExtractor(context.applicationContext)
    }
}
