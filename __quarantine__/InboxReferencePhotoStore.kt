package com.zak.pressmark.core.util

import android.net.Uri
import java.io.File

object InboxReferencePhotoStore {
    fun delete(referencePhotoUri: String?): Boolean {
        if (referencePhotoUri.isNullOrBlank()) return false
        return runCatching {
            val uri = Uri.parse(referencePhotoUri)
            val path = uri.path ?: return@runCatching false
            val file = File(path)
            if (file.exists()) file.delete() else false
        }.getOrDefault(false)
    }
}
