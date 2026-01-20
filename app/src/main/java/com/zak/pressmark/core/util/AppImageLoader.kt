// FILE: app/src/main/java/com/zak/pressmark/core/util/AppImageLoader.kt
package com.zak.pressmark.core.util

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.Path.Companion.toPath
import java.io.File

/**
 * Coil 3 app-wide ImageLoader (single source of truth for image caching).
 */
object AppImageLoader {

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        val appContext = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: build(appContext).also { instance = it }
        }
    }

    private fun build(context: Context): ImageLoader {
        val diskDir = File(context.cacheDir, "image_cache").apply { mkdirs() }

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context,0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(diskDir.absolutePath.toPath())
                    .maxSizeBytes(25L * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}