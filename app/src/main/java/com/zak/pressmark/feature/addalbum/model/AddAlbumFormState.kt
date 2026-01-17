package com.zak.pressmark.feature.addalbum.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver

@Immutable
data class AddAlbumFormState(
    val title: String = "",
    val artist: String = "",
    val artistId: Long? = null,
    val releaseYear: String = "",
    val label: String = "",
    val catalogNo: String = "",

    // ✅ NEW
    val format: String = "",
)

val AddAlbumFormStateSaver: Saver<AddAlbumFormState, Any> = mapSaver(
    save = { state ->
        mapOf(
            "title" to state.title,
            "artist" to state.artist,
            "artistId" to state.artistId,
            "releaseYear" to state.releaseYear,
            "label" to state.label,
            "catalogNo" to state.catalogNo,

            // ✅ NEW
            "format" to state.format,
        )
    },
    restore = { restored ->
        @Suppress("UNCHECKED_CAST")
        val map = restored
        AddAlbumFormState(
            title = map["title"] as? String ?: "",
            artist = map["artist"] as? String ?: "",
            artistId = map["artistId"] as? Long,
            releaseYear = map["releaseYear"] as? String ?: "",
            label = map["label"] as? String ?: "",
            catalogNo = map["catalogNo"] as? String ?: "",

            // ✅ NEW
            format = map["format"] as? String ?: "",
        )
    }
)
