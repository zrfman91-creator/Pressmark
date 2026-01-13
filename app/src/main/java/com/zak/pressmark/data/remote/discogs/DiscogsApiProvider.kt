// path: app/src/main/java/com/zak/pressmark/data/remote/discogs/DiscogsApiProvider.kt
package com.zak.pressmark.data.remote.discogs

import com.google.gson.Gson
import com.zak.pressmark.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DiscogsApiProvider {
    fun create(
        token: String = BuildConfig.DISCOGS_TOKEN.trim(),
        baseClient: OkHttpClient
    ): DiscogsApiService {
        val client = baseClient.newBuilder()
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url.newBuilder()
                    .addQueryParameter("token", token)
                    .build()
                val request = original.newBuilder().url(url).build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .client(client)
            .baseUrl("https://api.discogs.com/")
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(DiscogsApiService::class.java)
    }
}
