package com.zak.pressmark.data.remote.discogs

import com.google.gson.annotations.SerializedName

data class DiscogsMarketplaceStats(
    @SerializedName("lowest_price") val lowestPrice: DiscogsMarketplacePrice?,
    @SerializedName("median_price") val medianPrice: DiscogsMarketplacePrice?,
    @SerializedName("highest_price") val highestPrice: DiscogsMarketplacePrice?,
    @SerializedName("last_sold_price") val lastSoldPrice: DiscogsMarketplacePrice?,
    @SerializedName("last_sold_date") val lastSoldDate: String?,
)

data class DiscogsMarketplacePrice(
    @SerializedName("value") val value: Double?,
    @SerializedName("currency") val currency: String?,
)
