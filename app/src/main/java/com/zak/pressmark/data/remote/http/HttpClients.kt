// FILE: app/src/main/java/com/zak/pressmark/data/remote/http/HttpClients.kt
package com.zak.pressmark.data.remote.http

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttpClient(s) with an HTTP response cache.
 *
 * Single source of truth for base OkHttp configuration (cache + timeouts).
 */
object HttpClients {

    private val clients = mutableMapOf<String, OkHttpClient>()

    fun base(
        context: Context,
        cacheDirName: String = "okhttp_cache",
        cacheSizeBytes: Long = 20L * 1024L * 1024L, // 20MB
        connectTimeoutSeconds: Long = 10,
        readTimeoutSeconds: Long = 20,
        writeTimeoutSeconds: Long = 20,
        callTimeoutSeconds: Long = 30,
    ): OkHttpClient {
        val key = buildString {
            append(cacheDirName).append(':')
            append(cacheSizeBytes).append(':')
            append(connectTimeoutSeconds).append(':')
            append(readTimeoutSeconds).append(':')
            append(writeTimeoutSeconds).append(':')
            append(callTimeoutSeconds)
        }

        return synchronized(this) {
            clients.getOrPut(key) {
                val dir = File(context.cacheDir, cacheDirName).apply { mkdirs() }
                val cache = Cache(dir, cacheSizeBytes)

                OkHttpClient.Builder()
                    .cache(cache)
                    .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                    .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                    .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                    .callTimeout(callTimeoutSeconds, TimeUnit.SECONDS)
                    .build()
            }
        }
    }

    /**
     * Backwards compatible alias for older call sites.
     */
    fun cached(
        context: Context,
        cacheDirName: String = "okhttp_cache",
        cacheSizeBytes: Long = 20L * 1024L * 1024L,
    ): OkHttpClient = base(
        context = context,
        cacheDirName = cacheDirName,
        cacheSizeBytes = cacheSizeBytes,
    )
}
