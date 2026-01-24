// FILE: app/src/main/java/com/zak/pressmark/data/remote/discogs/DiscogsApiService.kt
package com.zak.pressmark.data.remote.discogs

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscogsApiService {

    @GET("database/search")
    suspend fun searchReleases(
        @Query("type") type: String = "release",
        @Query("artist") artist: String? = null,
        @Query("release_title") releaseTitle: String? = null,
        @Query("label") label: String? = null,
        @Query("catno") catno: String? = null,
        @Query("barcode") barcode: String? = null,
        @Query("per_page") perPage: Int = 25,
        @Query("page") page: Int = 1,
    ): DiscogsSearchResponse

    @GET("releases/{release_id}")
    suspend fun getRelease(
        @Path("release_id") releaseId: Long,
    ): DiscogsRelease

    @GET("masters/{masterId}")
    suspend fun getMaster(
        @Path("masterId") masterId: Long,
    ): DiscogsMaster
}
