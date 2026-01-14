// file: app/src/main/java/com/zak/pressmark/data/local/db/DbSchema.kt
package com.zak.pressmark.data.local.db

/**
 * Centralized definitions for database table and column names.
 * This helps prevent typos and makes refactoring schema names easier.
 */
object DbSchema {

    /**
     * Constants for the 'artists' table.
     */
    object Artist {
        const val TABLE = "artists"

        const val ID = "id"
        const val DISPLAY_NAME = "display_name"
        const val SORT_NAME = "sort_name"
        const val NAME_NORMALIZED = "name_normalized"
        const val ARTIST_TYPE = "artist_type"
    }

    /**
     * Constants for the 'albums' table.
     */
    object Album {
        const val TABLE = "albums"

        const val ID = "id"
        const val TITLE = "title"
        const val ARTIST = "artist" // Legacy text column
        const val ARTIST_ID = "artist_id" // Foreign key to Artist table
        const val COVER_URI = "cover_uri"
        const val DISCOGS_RELEASE_ID = "discogs_release_id"
        const val RELEASE_YEAR = "release_year"
        const val CATALOG_NO = "catalog_no"
        const val LABEL = "label"
        const val GENRE = "genre"
        const val STYLES = "styles"       // <-- Add this
        const val NOTES = "notes"         // <-- Add this
        const val TRACKLIST = "tracklist" // <-- Add this
        const val RATING = "rating"
        const val ADDED_AT = "added_at"
        const val LAST_PLAYED_AT = "last_played_at"
        const val MASTER_ID = "master_id" // From Discogs
        const val FORMAT = "format"       // From Discogs
        const val ARTWORK_PROVIDER = "artwork_provider"
        const val ARTWORK_PROVIDER_ITEM_ID = "artwork_provider_item_id"
    }
}
