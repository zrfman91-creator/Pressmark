package com.zak.pressmark.app.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.data.remote.discogs.DiscogsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiscogsModule {

    private const val BASE_URL = "https://api.discogs.com/"

    @Provides
    @Singleton
    fun provideDiscogsGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    @Named("discogsOkHttp")
    fun provideDiscogsOkHttpClient(): OkHttpClient {
        val authAndUa = Interceptor { chain ->
            val req = chain.request()
            val newReq = req.newBuilder()
                .header("User-Agent", "Pressmark/Android (${BuildConfig.APPLICATION_ID})")
                .apply {
                    val token = BuildConfig.DISCOGS_TOKEN
                    if (token.isNotBlank()) {
                        header("Authorization", "Discogs token=$token")
                    }
                }
                .build()
            chain.proceed(newReq)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor(authAndUa)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideDiscogsApi(
        gson: Gson,
        @Named("discogsOkHttp") okHttpClient: OkHttpClient,
    ): DiscogsApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(DiscogsApi::class.java)
    }
}
