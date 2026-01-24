// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/GenreDao.kt
package com.zak.pressmark.data.local.dao.v1

import androidx.room.*
import com.zak.pressmark.data.local.entity.v1.GenreEntity

@Dao
interface GenreDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(genres: List<GenreEntity>): List<Long>

    @Query("SELECT id FROM genres WHERE name IN (:names)")
    suspend fun getIdsByNames(names: List<String>): List<Long>
}