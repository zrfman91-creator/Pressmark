// FILE: app/src/main/java/com/zak/pressmark/data/local/dao/v2/WorkGenreStyleDaoV2.kt
package com.zak.pressmark.data.local.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.v2.DbSchemaV2
import com.zak.pressmark.data.local.entity.v2.GenreEntityV2
import com.zak.pressmark.data.local.entity.v2.StyleEntityV2
import com.zak.pressmark.data.local.entity.v2.WorkGenreCrossRefEntityV2
import com.zak.pressmark.data.local.entity.v2.WorkStyleCrossRefEntityV2

@Dao
interface WorkGenreStyleDaoV2 {

    @Query("SELECT ${DbSchemaV2.Genre.ID} FROM ${DbSchemaV2.Genre.TABLE} WHERE ${DbSchemaV2.Genre.NAME_NORMALIZED} = :normalized LIMIT 1")
    suspend fun getGenreIdByNormalized(normalized: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenre(genre: GenreEntityV2): Long

    @Query("SELECT ${DbSchemaV2.Style.ID} FROM ${DbSchemaV2.Style.TABLE} WHERE ${DbSchemaV2.Style.NAME_NORMALIZED} = :normalized LIMIT 1")
    suspend fun getStyleIdByNormalized(normalized: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStyle(style: StyleEntityV2): Long

    @Query("DELETE FROM ${DbSchemaV2.WorkGenre.TABLE} WHERE ${DbSchemaV2.WorkGenre.WORK_ID} = :workId")
    suspend fun deleteWorkGenres(workId: String)

    @Query("DELETE FROM ${DbSchemaV2.WorkStyle.TABLE} WHERE ${DbSchemaV2.WorkStyle.WORK_ID} = :workId")
    suspend fun deleteWorkStyles(workId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkGenres(entries: List<WorkGenreCrossRefEntityV2>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkStyles(entries: List<WorkStyleCrossRefEntityV2>)
}
