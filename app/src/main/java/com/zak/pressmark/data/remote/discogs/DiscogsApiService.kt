// path: app/src/main/java/com/zak/pressmark/data/remote/discogs/DiscogsApi.kt
package com.zak.pressmark.data.remote.discogs

import com.zak.pressmark.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscogsApiService {
    // Searches the Discogs database for releases. Endpoint: GET /database/search
    @GET("database/search")
    suspend fun searchReleases(
        @Query("type") type: String = "release",
        @Query("artist") artist: String? = null,
        @Query("release_title") releaseTitle: String? = null,
        @Query("label") label: String? = null,
        @Query("catno") catno: String? = null,
        @Query("per_page") perPage: Int = 25,
        @Query("page") page: Int = 1,
        @Query("token") apiToken: String = BuildConfig.DISCOGS_TOKEN.trim()
    ): DiscogsSearchResponse

    @GET("releases/{release_id}")
    suspend fun getRelease(
        @Path("release_id") releaseId: Long,
        @Query("token") apiToken: String = BuildConfig.DISCOGS_TOKEN.trim()
    ): DiscogsRelease // Note: Uses the new DiscogsRelease data class
}
