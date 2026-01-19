package com.zak.pressmark.feature.catalog.model

import androidx.compose.runtime.Immutable
import com.zak.pressmark.data.local.model.ReleaseListItem

@Immutable
enum class CatalogFilter {
    ALL,
    HAS_BARCODE,
    NO_BARCODE,
}

sealed interface CatalogListItem {
    val key: String

    @Immutable
    data class Header(
        override val key: String,
        val title: String,
        val subtitle: String,
    ) : CatalogListItem

    @Immutable
    data class ReleaseRow(
        val item: ReleaseListItem,
    ) : CatalogListItem {
        override val key: String = "release:${item.release.id}"
    }
}
