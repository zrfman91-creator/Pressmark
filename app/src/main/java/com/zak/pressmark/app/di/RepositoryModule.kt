// FILE: app/src/main/java/com/zak/pressmark/app/di/RepositoryModule.kt
package com.zak.pressmark.app.di

import com.zak.pressmark.data.local.dao.v2.PressingDaoV2
import com.zak.pressmark.data.local.dao.v2.ReleaseDaoV2
import com.zak.pressmark.data.local.dao.v2.VariantDaoV2
import com.zak.pressmark.data.local.dao.v2.WorkDaoV2
import com.zak.pressmark.data.local.db.v2.AppDatabaseV2
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideWorkRepositoryV2(
        db: AppDatabaseV2,
        workDao: WorkDaoV2,
        releaseDao: ReleaseDaoV2,
        pressingDao: PressingDaoV2,
        variantDao: VariantDaoV2,
    ): WorkRepositoryV2 {
        return WorkRepositoryV2(
            db = db,
            workDao = workDao,
            releaseDao = releaseDao,
            pressingDao = pressingDao,
            variantDao = variantDao,
        )
    }
}
