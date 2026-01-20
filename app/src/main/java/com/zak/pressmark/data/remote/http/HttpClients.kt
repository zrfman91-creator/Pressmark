// FILE: app/src/main/java/com/zak/pressmark/data/remote/http/HttpClients.kt
package com.zak.pressmark.data.remote.http

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Shared OkHttpClient(s) with an HTTP response cache.
 * Reuses instances so Discogs + Coil can share the same underlying cache.
 */
object HttpClients {

    private val clients = mutableMapOf<String, OkHttpClient>()

    fun cached(
        context: Context,
        cacheDirName: String = "http_cache",
        cacheSizeBytes: Long = 10L * 1024L * 1024L, // 10MB
    ): OkHttpClient {
        val key = "$cacheDirName:$cacheSizeBytes"
        return synchronized(this) {
            clients.getOrPut(key) {
                val dir = File(context.cacheDir, cacheDirName).apply { mkdirs() }
                val cache = Cache(dir, cacheSizeBytes)

                OkHttpClient.Builder()
                    .cache(cache)
                    .build()
            }
        }
    }
}
