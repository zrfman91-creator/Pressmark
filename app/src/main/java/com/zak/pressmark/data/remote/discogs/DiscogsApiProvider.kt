// FILE: app/src/main/java/com/zak/pressmark/data/remote/discogs/DiscogsApiProvider.kt
package com.zak.pressmark.data.remote.discogs

import com.zak.pressmark.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DiscogsApiProvider {

    private const val BASE_URL = "https://api.discogs.com/"

    fun create(
        token: String = BuildConfig.DISCOGS_TOKEN.trim(),
        userAgent: String = "Pressmark/${BuildConfig.VERSION_NAME}",
        baseClient: OkHttpClient,
    ): DiscogsApiService {
        // Apply Discogs auth via header (never as a query param).
        val authAndUa = Interceptor { chain ->
            val req = chain.request()
            val builder = req.newBuilder()
                .header("User-Agent", userAgent)

            if (token.isNotBlank()) {
                builder.header("Authorization", "Discogs token=$token")
            }

            chain.proceed(builder.build())
        }

        val client = baseClient.newBuilder()
            .addInterceptor(authAndUa)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(DiscogsApiService::class.java)
    }
}
