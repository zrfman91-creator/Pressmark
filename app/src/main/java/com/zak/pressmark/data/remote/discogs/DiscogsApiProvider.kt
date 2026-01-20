// FILE: app/src/main/java/com/zak/pressmark/data/remote/discogs/DiscogsApiProvider.kt
package com.zak.pressmark.data.remote.discogs

import com.zak.pressmark.BuildConfig
import okhttp3.OkHttpClient

object DiscogsApiProvider {
    fun create(
        token: String = BuildConfig.DISCOGS_TOKEN.trim(),
        userAgent: String = "Pressmark/${BuildConfig.VERSION_NAME}",
        baseClient: OkHttpClient,
    ): DiscogsApiService {
        // Apply Discogs auth *once* via an interceptor/header (never as a URL query param).
        // Discogs supports: Authorization: "Discogs token=<token>".
        return DiscogsClient.create(
            token = token,
            userAgent = userAgent,
            baseClient = baseClient,
        )
    }
}
