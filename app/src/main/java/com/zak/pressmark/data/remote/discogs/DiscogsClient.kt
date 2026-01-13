package com.zak.pressmark.data.remote.discogs

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DiscogsAuthInterceptor(
    private val token: String,
    private val userAgent: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .header("Authorization", "Discogs token=$token")
            .build()
        return chain.proceed(req)
    }
}

object DiscogsClient {

    fun create(
        token: String,
        userAgent: String,
        baseClient: OkHttpClient? = null,
    ): DiscogsApiService {
        val okHttp = (baseClient?.newBuilder() ?: OkHttpClient.Builder())
            .addInterceptor(DiscogsAuthInterceptor(token, userAgent))
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.discogs.com/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DiscogsApiService::class.java)
    }
}
