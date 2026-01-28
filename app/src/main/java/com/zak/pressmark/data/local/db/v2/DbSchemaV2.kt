package com.zak.pressmark.data.local.db.v2

/**
 * V2 schema constants for the canonical domain model:
 *   Work -> Release -> Pressing -> Variant
 *
 * We intentionally keep these tables isolated from legacy tables by using v2_* names.
 * Once the UI + pipeline are fully migrated, we can:
 *   1) delete legacy tables/entities/DAOs, and
 *   2) rename v2_* tables to final names (or keep them as-is).
 */
object DbSchemaV2 {

    object Work {
        const val TABLE = "v2_works"

        const val ID = "id"

        const val TITLE = "title"
        const val TITLE_NORMALIZED = "title_normalized"
        const val TITLE_SORT = "title_sort"

        const val ARTIST_LINE = "artist_line"
        const val ARTIST_NORMALIZED = "artist_normalized"
        const val ARTIST_SORT = "artist_sort"

        const val YEAR = "year"

        // Lists stored as JSON strings ("[]") until we introduce richer converters.
        const val GENRES_JSON = "genres_json"
        const val STYLES_JSON = "styles_json"

        const val PRIMARY_ARTWORK_URI = "primary_artwork_uri"

        // Common external anchors (keep small + pragmatic for now).
        const val DISCOGS_MASTER_ID = "discogs_master_id"
        const val MUSICBRAINZ_RELEASE_GROUP_ID = "musicbrainz_release_group_id"

        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
    }

    object Genre {
        const val TABLE = "v2_genres"
        const val ID = "id"
        const val NAME_NORMALIZED = "name_normalized"
        const val NAME_DISPLAY = "name_display"
    }

    object Style {
        const val TABLE = "v2_styles"
        const val ID = "id"
        const val NAME_NORMALIZED = "name_normalized"
        const val NAME_DISPLAY = "name_display"
    }

    object WorkGenre {
        const val TABLE = "v2_work_genres"
        const val WORK_ID = "work_id"
        const val GENRE_ID = "genre_id"
    }

    object WorkStyle {
        const val TABLE = "v2_work_styles"
        const val WORK_ID = "work_id"
        const val STYLE_ID = "style_id"
    }

    object Release {
        const val TABLE = "v2_releases"

        const val ID = "id"
        const val WORK_ID = "work_id"

        const val LABEL = "label"
        const val LABEL_NORMALIZED = "label_normalized"

        const val CATALOG_NO = "catalog_no"
        const val CATALOG_NO_NORMALIZED = "catalog_no_normalized"

        const val COUNTRY = "country"
        const val FORMAT = "format"
        const val RELEASE_YEAR = "release_year"
        const val RELEASE_TYPE = "release_type"

        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
    }

    object Pressing {
        const val TABLE = "v2_pressings"

        const val ID = "id"
        const val RELEASE_ID = "release_id"

        const val BARCODE = "barcode"
        const val BARCODE_NORMALIZED = "barcode_normalized"

        // Lists stored as JSON strings ("[]") until we introduce richer converters.
        const val RUNOUTS_JSON = "runouts_json"

        const val PRESSING_PLANT = "pressing_plant"

        // Sometimes differs from Release; keep optional duplication.
        const val LABEL = "label"
        const val CATALOG_NO = "catalog_no"
        const val COUNTRY = "country"
        const val FORMAT = "format"
        const val RELEASE_YEAR = "release_year"

        // Provider anchors (pragmatic for lookup / reconciliation).
        const val DISCOGS_RELEASE_ID = "discogs_release_id"
        const val MUSICBRAINZ_RELEASE_ID = "musicbrainz_release_id"

        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
    }

    object Variant {
        const val TABLE = "v2_variants"

        const val ID = "id"
        const val WORK_ID = "work_id"
        const val PRESSING_ID = "pressing_id"

        const val VARIANT_KEY = "variant_key" // e.g. "default", "signed", "red-vinyl"

        const val NOTES = "notes"
        const val RATING = "rating"

        const val ADDED_AT = "added_at"
        const val LAST_PLAYED_AT = "last_played_at"
    }
}
