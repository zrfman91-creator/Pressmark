package com.zak.pressmark.data.remote.discogs

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscogsApi {

    @GET("database/search")
    suspend fun search(
        @Query("type") type: String = "master",
        @Query("artist") artist: String,
        @Query("release_title") releaseTitle: String,
        @Query("year") year: Int? = null,
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
    ): DiscogsSearchResponse

    @GET("masters/{masterId}")
    suspend fun getMaster(
        @Path("masterId") masterId: Long,
    ): DiscogsMasterResponse
}
