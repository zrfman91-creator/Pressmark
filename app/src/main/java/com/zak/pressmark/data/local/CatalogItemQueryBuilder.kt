package com.zak.pressmark.data.local.query

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.zak.pressmark.data.local.entity.CatalogItemState
import com.zak.pressmark.data.util.Normalization

/**
 * Centralized query builder so list sorting/filtering stays inside SQLite.
 *
 * NOTE: This uses LIKE 'prefix%' for fast prefix search and index friendliness,
 * assuming display_title_sort / display_artist_sort are indexed.
 */
object CatalogItemQueryBuilder {

    enum class Sort {
        TITLE_ASC,
        TITLE_DESC,
        ARTIST_ASC,
        ARTIST_DESC,
        ADDED_DESC,
        ADDED_ASC,
        UPDATED_DESC,
        UPDATED_ASC
    }

    fun build(
        titlePrefix: String? = null,
        artistPrefix: String? = null,
        state: CatalogItemState? = null,
        sort: Sort = Sort.ADDED_DESC,
        limit: Int = 200,
        offset: Int = 0
    ): SupportSQLiteQuery {
        val where = StringBuilder("WHERE 1=1")
        val args = mutableListOf<Any>()

        if (!titlePrefix.isNullOrBlank()) {
            where.append(" AND display_title_sort LIKE ?")
            args += Normalization.sortKey(titlePrefix) + "%"
        }
        if (!artistPrefix.isNullOrBlank()) {
            where.append(" AND display_artist_sort LIKE ?")
            args += Normalization.sortKey(artistPrefix) + "%"
        }
        if (state != null) {
            where.append(" AND state = ?")
            args += state.name
        }

        val orderBy = when (sort) {
            Sort.TITLE_ASC -> "ORDER BY display_title_sort ASC"
            Sort.TITLE_DESC -> "ORDER BY display_title_sort DESC"
            Sort.ARTIST_ASC -> "ORDER BY display_artist_sort ASC"
            Sort.ARTIST_DESC -> "ORDER BY display_artist_sort DESC"
            Sort.ADDED_ASC -> "ORDER BY created_at ASC"
            Sort.ADDED_DESC -> "ORDER BY created_at DESC"
            Sort.UPDATED_ASC -> "ORDER BY updated_at ASC"
            Sort.UPDATED_DESC -> "ORDER BY updated_at DESC"
        }

        val sql = """
            SELECT * FROM catalog_items
            $where
            $orderBy
            LIMIT ? OFFSET ?
        """.trimIndent()

        args += limit
        args += offset

        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }
}
