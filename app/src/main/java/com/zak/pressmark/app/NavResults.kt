// FILE: app/src/main/java/com/zak/pressmark/app/NavResults.kt
package com.zak.pressmark.app

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.StateFlow

object NavResultKeys {
    const val SAVED_ALBUM_ID: String = "SAVED_ALBUM_ID"
    const val CLEAR_ADD_ALBUM_FORM: String = "CLEAR_ADD_ALBUM_FORM"
}

fun SavedStateHandle.savedAlbumIdFlow(): StateFlow<String?> =
    getStateFlow(NavResultKeys.SAVED_ALBUM_ID, null)

fun SavedStateHandle.setSavedAlbumId(albumId: String) {
    set(NavResultKeys.SAVED_ALBUM_ID, albumId)
}

fun SavedStateHandle.clearSavedAlbumId() {
    set<String?>(NavResultKeys.SAVED_ALBUM_ID, null)
}

fun SavedStateHandle.clearAddAlbumFormFlow(): StateFlow<Boolean> =
    getStateFlow(NavResultKeys.CLEAR_ADD_ALBUM_FORM, false)

fun SavedStateHandle.requestClearAddAlbumForm() {
    set(NavResultKeys.CLEAR_ADD_ALBUM_FORM, true)
}

fun SavedStateHandle.consumeClearAddAlbumForm() {
    set(NavResultKeys.CLEAR_ADD_ALBUM_FORM, false)
}
