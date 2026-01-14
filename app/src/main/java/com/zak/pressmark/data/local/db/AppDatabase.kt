package com.zak.pressmark.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zak.pressmark.data.local.dao.AlbumDao
import com.zak.pressmark.data.local.dao.ArtistDao
import com.zak.pressmark.data.local.dao.GenreDao
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.data.local.entity.AlbumGenreCrossRef
import com.zak.pressmark.data.local.entity.GenreEntity


@Database(
    entities = [
        AlbumEntity::class,
        ArtistEntity::class,
        GenreEntity::class,
        AlbumGenreCrossRef::class
    ],
    version = 8,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun genreDao(): GenreDao //
}

