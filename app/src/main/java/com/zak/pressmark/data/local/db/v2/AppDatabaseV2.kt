package com.zak.pressmark.data.local.db.v2

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zak.pressmark.data.local.dao.v2.PressingDaoV2
import com.zak.pressmark.data.local.dao.v2.ReleaseDaoV2
import com.zak.pressmark.data.local.dao.v2.VariantDaoV2
import com.zak.pressmark.data.local.dao.v2.WorkDaoV2
import com.zak.pressmark.data.local.entity.v2.PressingEntityV2
import com.zak.pressmark.data.local.entity.v2.ReleaseEntityV2
import com.zak.pressmark.data.local.entity.v2.VariantEntityV2
import com.zak.pressmark.data.local.entity.v2.WorkEntityV2

@Database(
    entities = [
        WorkEntityV2::class,
        ReleaseEntityV2::class,
        PressingEntityV2::class,
        VariantEntityV2::class,
    ],
    version = 1,
    exportSchema = true,
)
// @TypeConverters(RoomConvertersV2::class)
abstract class AppDatabaseV2 : RoomDatabase() {
    abstract fun workDaoV2(): WorkDaoV2
    abstract fun releaseDaoV2(): ReleaseDaoV2
    abstract fun pressingDaoV2(): PressingDaoV2
    abstract fun variantDaoV2(): VariantDaoV2
}
