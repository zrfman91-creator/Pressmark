package com.zak.pressmark.feature.catalog.model

import androidx.compose.runtime.Immutable

@Immutable
enum class AlbumGrouping {
    NONE,
    ARTIST,
    DECADE,
}

fun decadeLabel(decadeStart: Int?): String {
    return if (decadeStart == null) "Unknown Year" else "${decadeStart}–${decadeStart + 9}"
}
