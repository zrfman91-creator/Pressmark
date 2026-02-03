package com.zak.pressmark.data.remote.discogs

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Single Discogs API surface for Pressmark.
 *
 * Keep this as the ONLY Retrofit interface in the Discogs package.
 */
interface DiscogsApiService {

    // ------------------------ Search (Database) ------------------------

    @GET("database/search")
    suspend fun searchDatabase(
        @Query("type") type: String? = null,          // "release" | "master" | etc.
        @Query("q") q: String? = null,                // optional free-text
        @Query("artist") artist: String? = null,
        @Query("release_title") releaseTitle: String? = null,
        @Query("year") year: Int? = null,
        @Query("label") label: String? = null,
        @Query("catno") catno: String? = null,
        @Query("barcode") barcode: String? = null,
        @Query("per_page") perPage: Int = 25,
        @Query("page") page: Int = 1,
    ): DiscogsSearchResponse

    /**
     * Convenience: your previous "masters search" used in manual ingest.
     */
    suspend fun searchMasters(
        artist: String,
        releaseTitle: String,
        year: Int?,
        perPage: Int = 10,
        page: Int = 1,
    ): DiscogsSearchResponse =
        searchDatabase(
            type = "master",
            artist = artist,
            releaseTitle = releaseTitle,
            year = year,
            perPage = perPage,
            page = page,
        )

    /**
     * Convenience: your previous "release search" used by barcode/artwork resolution.
     * Kept name to minimize churn.
     */
    suspend fun searchReleases(
        artist: String? = null,
        releaseTitle: String? = null,
        year: Int? = null,
        label: String? = null,
        catno: String? = null,
        barcode: String? = null,
        perPage: Int = 25,
        page: Int = 1,
    ): DiscogsSearchResponse =
        searchDatabase(
            type = "release",
            artist = artist,
            releaseTitle = releaseTitle,
            year = year,
            label = label,
            catno = catno,
            barcode = barcode,
            perPage = perPage,
            page = page,
        )

    // ------------------------ Details ------------------------

    @GET("releases/{release_id}")
    suspend fun getRelease(
        @Path("release_id") releaseId: Long,
    ): DiscogsRelease

    @GET("masters/{masterId}")
    suspend fun getMaster(
        @Path("masterId") masterId: Long,
    ): DiscogsMaster
}
