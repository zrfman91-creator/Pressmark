package com.zak.pressmark.app

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.StateFlow

object NavResultKeys {
    const val SavedAlbumId: String = "saved_album_id"
}

fun SavedStateHandle.savedAlbumIdFlow(): StateFlow<String?> =
    getStateFlow(NavResultKeys.SavedAlbumId, null)

fun SavedStateHandle.setSavedAlbumId(albumId: String) {
    set(NavResultKeys.SavedAlbumId, albumId)
}

fun SavedStateHandle.clearSavedAlbumId() {
    set<String?>(NavResultKeys.SavedAlbumId, null)
}
