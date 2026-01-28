// FILE: app/src/main/java/com/zak/pressmark/app/di/DatabaseModule.kt
package com.zak.pressmark.app.di

import android.content.Context
import androidx.room.Room
import com.zak.pressmark.data.local.dao.v2.PressingDaoV2
import com.zak.pressmark.data.local.dao.v2.ReleaseDaoV2
import com.zak.pressmark.data.local.dao.v2.VariantDaoV2
import com.zak.pressmark.data.local.dao.v2.WorkGenreStyleDaoV2
import com.zak.pressmark.data.local.dao.v2.WorkDaoV2
import com.zak.pressmark.data.local.db.v2.AppDatabaseV2
import com.zak.pressmark.data.local.db.v2.MigrationsV2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the V2 Room database + V2 DAOs.
 *
 * IMPORTANT: If your AppDatabaseV2 DAO getter names differ, rename them here.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "pressmark_v2.db"

    @Provides
    @Singleton
    fun provideAppDatabaseV2(
        @ApplicationContext context: Context,
    ): AppDatabaseV2 {
        return Room.databaseBuilder(context, AppDatabaseV2::class.java, DB_NAME)
            .addMigrations(MigrationsV2.MIGRATION_1_2)
            // Dev-friendly. Remove/replace with proper migrations when schema stabilizes.
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkDaoV2(db: AppDatabaseV2): WorkDaoV2 = db.workDaoV2()

    @Provides
    @Singleton
    fun provideReleaseDaoV2(db: AppDatabaseV2): ReleaseDaoV2 = db.releaseDaoV2()

    @Provides
    @Singleton
    fun providePressingDaoV2(db: AppDatabaseV2): PressingDaoV2 = db.pressingDaoV2()

    @Provides
    @Singleton
    fun provideVariantDaoV2(db: AppDatabaseV2): VariantDaoV2 = db.variantDaoV2()

    @Provides
    @Singleton
    fun provideWorkGenreStyleDaoV2(db: AppDatabaseV2): WorkGenreStyleDaoV2 = db.workGenreStyleDaoV2()
}
