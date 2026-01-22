// FILE: app/src/main/java/com/zak/pressmark/data/local/db/DbSchema.kt
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
     * Legacy table name retained for now.
     *
     * NOTE: We are moving toward a Release-first model (Release + ReleaseArtistCredit),
     * but leaving these constants in place temporarily to avoid breaking unrelated areas
     * while we implement bottom-up.
     */
    object Album {
        const val TABLE = "albums"

        const val ID = "id"
        const val TITLE = "title"
        const val ARTIST = "artist" // Legacy text column
        const val ARTIST_ID = "artist_id" // Legacy Foreign Key to Artist table (To Be Removed)

        const val COVER_URI = "cover_uri"
        const val DISCOGS_RELEASE_ID = "discogs_release_id"
        const val RELEASE_YEAR = "release_year"
        const val CATALOG_NO = "catalog_no"
        const val LABEL = "label"
        const val GENRE = "genre"
        const val STYLES = "styles"
        const val NOTES = "notes"
        const val TRACKLIST = "tracklist"

        const val RATING = "rating"
        const val ADDED_AT = "added_at"
        const val LAST_PLAYED_AT = "last_played_at"

        const val MASTER_ID = "master_id" // From Discogs
        const val FORMAT = "format"       // From Discogs (LP/12" etc.)
        const val ARTWORK_PROVIDER = "artwork_provider"
        const val ARTWORK_PROVIDER_ITEM_ID = "artwork_provider_item_id"
    }
    /**
     * Constants for the 'releases' table.
     *
     * This becomes the top-level entity (what you own/list/scan).
     */
    object Release {
        const val TABLE = "releases"

        const val ID = "id"
        const val TITLE = "title"

        const val RELEASE_YEAR = "release_year"
        const val GENRE = "genre"

        // Pressing/identifiers
        const val LABEL = "label"
        const val CATALOG_NO = "catalog_no"
        const val BARCODE = "barcode"
        const val COUNTRY = "country"

        /**
         * Classification
         * - release_type: Studio / Live / Compilation / Soundtrack / Greatest Hits / etc.
         * - format: LP / EP / 7" / 12" / CD / Cassette / etc.
         */
        const val RELEASE_TYPE = "release_type"
        const val FORMAT = "format"

        // Legacy / external ids
        const val DISCOGS_RELEASE_ID = "discogs_release_id"
        const val MASTER_ID = "master_id"

        // Artwork provider (canonical)
        const val ARTWORK_PROVIDER = "artwork_provider"
        const val ARTWORK_PROVIDER_ITEM_ID = "artwork_provider_item_id"

        // User metadata
        const val NOTES = "notes"
        const val RATING = "rating"
        const val ADDED_AT = "added_at"
        const val LAST_PLAYED_AT = "last_played_at"
    }

    /**
     * Join table between Release and Artist, with roles and ordering.
     * This is the key that replaces Album.artist_id and unlocks:
     * - multiple primary artists (duets/splits)
     * - orchestras/ensembles
     * - "with" and "feat." credits
     */
    object ReleaseArtistCredit {
        const val TABLE = "release_artist_credits"

        const val ID = "id"
        const val RELEASE_ID = "release_id"
        const val ARTIST_ID = "artist_id"

        /**
         * Credit role enum stored as string (recommended for readability).
         * Examples: PRIMARY, FEATURED, WITH, ORCHESTRA, ENSEMBLE, CONDUCTOR
         */
        const val ROLE = "role"

        /**
         * Display ordering (1..N) for rendering credit lines.
         */
        const val POSITION = "position"

        /**
         * Optional hint to preserve exact human phrasing when desired.
         * Example: "and his orchestra" / "and her orchestra" / "and the orchestra"
         */
        const val DISPLAY_HINT = "display_hint"
    }

    /**
     * Artwork rows allow multiple cover variants per release and multiple image kinds.
     * This replaces a single coverUri column when you want variants and richer capture
     * (front/back/label/runout).
     */
    object Artwork {
        const val TABLE = "artworks"

        const val ID = "id"
        const val RELEASE_ID = "release_id"

        const val URI = "uri"
        const val KIND = "kind" // COVER_FRONT, COVER_BACK, LABEL, RUNOUT, OTHER
        const val VARIANT_KEY = "variant_key" // "alt-1", "red-cover", etc.
        const val SOURCE = "source" // LOCAL, DISCOGS, OTHER
        const val IS_PRIMARY = "is_primary"

        const val WIDTH = "width"
        const val HEIGHT = "height"
        const val CREATED_AT = "created_at"
    }

    object InboxItem {
        const val TABLE = "inbox_items"

        const val ID = "id"
        const val SOURCE_TYPE = "source_type"
        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"

        const val BARCODE = "barcode"
        const val RAW_TITLE = "raw_title"
        const val RAW_ARTIST = "raw_artist"
        const val RAW_ROW_JSON = "raw_row_json"
        const val PHOTO_URIS_JSON = "photo_uris_json"

        const val OCR_STATUS = "ocr_status"
        const val LOOKUP_STATUS = "lookup_status"
        const val ERROR_CODE = "error_code"
        const val RETRY_COUNT = "retry_count"
        const val NEXT_OCR_AT = "next_ocr_at"
        const val NEXT_LOOKUP_AT = "next_lookup_at"
        const val LAST_TRIED_AT = "last_tried_at"

        const val EXTRACTED_TITLE = "extracted_title"
        const val EXTRACTED_ARTIST = "extracted_artist"
        const val EXTRACTED_LABEL = "extracted_label"
        const val EXTRACTED_CATNO = "extracted_catno"
        const val CONFIDENCE = "confidence"
        const val REASONS_JSON = "reasons_json"
        const val WAS_UNDONE = "was_undone"
        const val COMMITTED_PROVIDER_ITEM_ID = "committed_provider_item_id"
        const val IS_UNKNOWN = "is_unknown"
        const val DELETED_AT = "deleted_at"
        const val REFERENCE_PHOTO_URI = "reference_photo_uri"
    }

    object ProviderSnapshot {
        const val TABLE = "provider_snapshots"

        const val ID = "id"
        const val INBOX_ITEM_ID = "inbox_item_id"
        const val PROVIDER = "provider"
        const val PROVIDER_ITEM_ID = "provider_item_id"
        const val TITLE = "title"
        const val ARTIST = "artist"
        const val LABEL = "label"
        const val CATALOG_NO = "catalog_no"
        const val BARCODE = "barcode"
        const val RAW_JSON = "raw_json"
        const val CONFIDENCE = "confidence"
        const val REASONS_JSON = "reasons_json"
    }

    object ImportBatch {
        const val TABLE = "import_batches"

        const val ID = "id"
        const val CREATED_AT = "created_at"
        const val MAPPING_JSON = "mapping_json"
        const val TOTAL_COUNT = "total_count"
        const val SUCCESS_COUNT = "success_count"
        const val FAILURE_COUNT = "failure_count"
    }
}
