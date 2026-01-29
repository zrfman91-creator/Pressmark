package com.zak.pressmark.data.local.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zak.pressmark.data.local.db.v2.DbSchemaV2
import com.zak.pressmark.data.local.entity.v2.ArtistEntityV2

@Dao
interface ArtistDaoV2 {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(artist: ArtistEntityV2)

    @Query("SELECT * FROM ${DbSchemaV2.Artist.TABLE} WHERE ${DbSchemaV2.Artist.ID} = :artistId LIMIT 1")
    suspend fun getById(artistId: String): ArtistEntityV2?

    @Query("SELECT * FROM ${DbSchemaV2.Artist.TABLE} WHERE ${DbSchemaV2.Artist.NAME_NORMALIZED} = :nameNormalized LIMIT 1")
    suspend fun getByNormalized(nameNormalized: String): ArtistEntityV2?
}
