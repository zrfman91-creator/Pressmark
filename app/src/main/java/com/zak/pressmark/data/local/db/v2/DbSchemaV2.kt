package com.zak.pressmark.data.local.db.v2

/**
 * IMPORTANT:
 * - All values referenced inside @Query strings must be compile-time constants => const val.
 * - Keep this schema authoritative for DAOs/entities that build SQL with string interpolation.
 */
object DbSchemaV2 {

    object Work {
        const val TABLE = "works_v2"

        const val ID = "id"

        const val TITLE = "title"
        const val TITLE_NORMALIZED = "title_normalized"
        const val TITLE_SORT = "title_sort"

        const val ARTIST_LINE = "artist_line"
        const val ARTIST_NORMALIZED = "artist_normalized"
        const val ARTIST_SORT = "artist_sort"

        const val YEAR = "year"

        const val GENRES_JSON = "genres_json"
        const val STYLES_JSON = "styles_json"

        const val PRIMARY_ARTWORK_URI = "primary_artwork_uri"
        const val MASTER_ARTWORK_URI = "master_artwork_uri"

        const val DISCOGS_MASTER_ID = "discogs_master_id"
        const val MUSICBRAINZ_RELEASE_GROUP_ID = "musicbrainz_release_group_id"

        const val CREATED_AT = "created_at"
        const val UPDATED_AT = "updated_at"
    }

    object Release {
        const val TABLE = "releases_v2"

        const val ID = "id"
        const val WORK_ID = "work_id"

        // These are used in VariantDao join/projection
        const val LABEL = "label"
        const val CATALOG_NO = "catalog_no"
        const val COUNTRY = "country"
        const val RELEASE_YEAR = "release_year"
        const val FORMAT = "format"
    }

    object Pressing {
        const val TABLE = "pressings_v2"

        const val ID = "id"
        const val RELEASE_ID = "release_id"

        // Used for barcode lookup and joins
        const val BARCODE_NORMALIZED = "barcode_normalized"
        const val UPDATED_AT = "updated_at"

        // Used in VariantDao join/projection (safe to keep even if nullable)
        const val DISCOGS_RELEASE_ID = "discogs_release_id"
        const val LABEL = "label"
        const val CATALOG_NO = "catalog_no"
        const val COUNTRY = "country"
        const val RELEASE_YEAR = "release_year"
        const val FORMAT = "format"

        // Some screens/queries may use these
        const val FORMAT_SUMMARY = "format_summary"
        const val ARTWORK_URI = "artwork_uri"
    }

    object Variant {
        const val TABLE = "variants_v2"

        const val WORK_ID = "work_id"
        const val PRESSING_ID = "pressing_id"

        // Used for stable identity of a refinement choice
        const val VARIANT_KEY = "variant_key"

        const val ADDED_AT = "added_at"

        // If you later reintroduce “selected” semantics, keep this column name stable:
        const val IS_SELECTED = "is_selected"

        // Optional (only if your table actually has it; safe to keep as a constant)
        const val ID = "id"
    }

    object Genre {
        const val TABLE = "genres_v2"

        const val ID = "id"
        const val NAME_DISPLAY = "name_display"
        const val NAME_NORMALIZED = "name_normalized"
    }

    object Style {
        const val TABLE = "styles_v2"

        const val ID = "id"
        const val NAME_DISPLAY = "name_display"
        const val NAME_NORMALIZED = "name_normalized"
    }

    object WorkGenre {
        const val TABLE = "work_genres_v2"
        const val WORK_ID = "work_id"
        const val GENRE_ID = "genre_id"
    }

    object WorkStyle {
        const val TABLE = "work_styles_v2"
        const val WORK_ID = "work_id"
        const val STYLE_ID = "style_id"
    }
}
