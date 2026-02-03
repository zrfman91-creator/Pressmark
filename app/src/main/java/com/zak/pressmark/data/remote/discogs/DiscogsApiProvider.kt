package com.zak.pressmark.data.remote.discogs

import com.zak.pressmark.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

object DiscogsApiProvider {

    private const val BASE_URL = "https://api.discogs.com/"

    fun create(
        token: String = BuildConfig.DISCOGS_TOKEN.trim(),
        userAgent: String = "Pressmark/${BuildConfig.VERSION_NAME}",
        baseClient: OkHttpClient,
        debugLogging: Boolean = false,
    ): DiscogsApiService {
        val authAndUa = Interceptor { chain ->
            val req = chain.request()
            val builder = req.newBuilder()
                .header("User-Agent", userAgent)

            if (token.isNotBlank()) {
                // Discogs PAT header format
                builder.header("Authorization", "Discogs token=$token")
            }

            chain.proceed(builder.build())
        }

        val client = baseClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(authAndUa)
            .addInterceptor(RetryWithBackoffInterceptor(debugLogging))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(DiscogsApiService::class.java)
    }

    /**
     * Conservative retry for Discogs:
     * - 429 (rate limit) + Retry-After
     * - 502/503/504 transient
     * - IOException
     */
    private class RetryWithBackoffInterceptor(
        private val debugLogging: Boolean,
    ) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            var attempt = 0
            var lastException: IOException? = null

            while (attempt < 5) {
                try {
                    val response = chain.proceed(chain.request())
                    if (response.isSuccessful) return response

                    val code = response.code
                    if (code == 429 || code == 502 || code == 503 || code == 504) {
                        val delayMs = computeDelayMs(attempt, response)
                        if (debugLogging) {
                            android.util.Log.d("DiscogsApiProvider", "Retry $attempt for HTTP $code after ${delayMs}ms")
                        }
                        response.close()
                        Thread.sleep(delayMs)
                        attempt++
                        continue
                    }

                    return response
                } catch (io: IOException) {
                    lastException = io
                    val delayMs = computeDelayMs(attempt, null)
                    if (debugLogging) {
                        android.util.Log.d("DiscogsApiProvider", "Retry $attempt for IOException after ${delayMs}ms: ${io.message}")
                    }
                    Thread.sleep(delayMs)
                    attempt++
                }
            }

            throw lastException ?: IOException("Discogs request failed after retries")
        }

        private fun computeDelayMs(attempt: Int, response: Response?): Long {
            val retryAfterSeconds = response?.header("Retry-After")?.toLongOrNull()
            if (retryAfterSeconds != null && retryAfterSeconds > 0) {
                return min(retryAfterSeconds * 1000L, 15_000L)
            }

            val base = 500.0 * 2.0.pow(attempt.toDouble())
            return min(base.toLong(), 8000L)
        }
    }
}
