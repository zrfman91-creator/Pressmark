// FILE: app/src/main/java/com/zak/pressmark/data/remote/discogs/DiscogsModels.kt
package com.zak.pressmark.data.remote.discogs

import com.google.gson.annotations.SerializedName

/**
 * Minimal models required to satisfy:
 * - /database/search (search)
 * - /releases/{id} (barcode -> release details)
 * - /masters/{id} (manual -> master details)
 *
 * Expand later as needed.
 */

// ------------------------ Release ------------------------

data class DiscogsRelease(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("master_id") val masterId: Long? = null,

    // Used by DiscogsReleaseMapper
    @SerializedName("formats") val formats: List<DiscogsFormat>? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("notes") val notes: String? = null,

    @SerializedName("genres") val genres: List<String>? = null,
    @SerializedName("styles") val styles: List<String>? = null,

    @SerializedName("artists") val artists: List<DiscogsArtist>? = null,
    @SerializedName("images") val images: List<DiscogsImage>? = null,
)

data class DiscogsArtist(
    @SerializedName("name") val name: String? = null,
)

data class DiscogsImage(
    @SerializedName("uri") val uri: String? = null,
    @SerializedName("uri150") val uri150: String? = null,
    @SerializedName("type") val type: String? = null,
)

data class DiscogsFormat(
    @SerializedName("name") val name: String? = null,
    @SerializedName("descriptions") val descriptions: List<String>? = null,
)

// ------------------------ Master ------------------------

data class DiscogsMaster(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("genres") val genres: List<String>? = null,
    @SerializedName("styles") val styles: List<String>? = null,
    @SerializedName("images") val images: List<DiscogsImage>? = null,
)

// ------------------------ Marketplace stats (optional) ------------------------

data class DiscogsMarketplaceStats(
    @SerializedName("num_for_sale") val numForSale: Int? = null,
    @SerializedName("lowest_price") val lowestPrice: DiscogsPrice? = null,
    @SerializedName("blocked_from_sale") val blockedFromSale: Boolean? = null,
)

data class DiscogsPrice(
    @SerializedName("value") val value: Double? = null,
    @SerializedName("currency") val currency: String? = null,
)
