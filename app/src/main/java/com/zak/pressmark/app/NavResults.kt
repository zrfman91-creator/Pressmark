package com.zak.pressmark.app

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.StateFlow

object NavResultKeys {
    const val SavedAlbumId: String = "saved_album_id"
    const val ClearAddAlbumForm: String = "clear_add_album_form"
}

fun SavedStateHandle.savedAlbumIdFlow(): StateFlow<String?> =
    getStateFlow(NavResultKeys.SavedAlbumId, null)

fun SavedStateHandle.setSavedAlbumId(albumId: String) {
    set(NavResultKeys.SavedAlbumId, albumId)
}

fun SavedStateHandle.clearSavedAlbumId() {
    set<String?>(NavResultKeys.SavedAlbumId, null)
}

fun SavedStateHandle.clearAddAlbumFormFlow(): StateFlow<Boolean> =
    getStateFlow(NavResultKeys.ClearAddAlbumForm, false)

fun SavedStateHandle.requestClearAddAlbumForm() {
    set(NavResultKeys.ClearAddAlbumForm, true)
}

fun SavedStateHandle.consumeClearAddAlbumForm() {
    set(NavResultKeys.ClearAddAlbumForm, false)
}
