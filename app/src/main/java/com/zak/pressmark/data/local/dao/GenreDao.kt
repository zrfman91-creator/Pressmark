// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/GenreDao.kt
package com.zak.pressmark.data.local.dao

import androidx.room.*
import com.zak.pressmark.data.local.db.DbSchema.Album
import com.zak.pressmark.data.local.db.DbSchema.Artist
import com.zak.pressmark.data.local.entity.GenreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenreDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(genres: List<GenreEntity>): List<Long>

    @Query("SELECT id FROM genres WHERE name IN (:names)")
    suspend fun getIdsByNames(names: List<String>): List<Long>
}