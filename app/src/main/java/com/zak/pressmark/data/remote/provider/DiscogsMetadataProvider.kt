package com.zak.pressmark.data.remote.provider

import com.google.gson.Gson
import com.zak.pressmark.data.model.inbox.ProviderCandidate
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.remote.discogs.DiscogsSearchResult
import retrofit2.HttpException

class DiscogsMetadataProvider(
    private val api: DiscogsApiService,
    private val gson: Gson = Gson(),
) : MetadataProvider {
    override suspend fun lookupByBarcode(barcode: String): List<ProviderCandidate> {
        val normalizedBarcode = barcode.trim().takeIf { it.isNotBlank() }
        return runSearch("barcode", normalizedBarcode) {
            api.searchReleases(barcode = barcode, perPage = 25).results
        }
    }

    override suspend fun lookupByCatalogNo(
        catalogNo: String,
        label: String?,
    ): List<ProviderCandidate> {
        return runSearch("catno") {
            api.searchReleases(
                catno = catalogNo,
                label = label,
                perPage = 25,
            ).results
        }
    }

    override suspend fun searchByTitleArtist(title: String, artist: String): List<ProviderCandidate> {
        return runSearch("title_artist") {
            api.searchReleases(
                artist = artist,
                releaseTitle = title,
                perPage = 25,
            ).results
        }
    }

    private suspend fun runSearch(
        searchLabel: String,
        fallbackBarcode: String? = null,
        block: suspend () -> List<DiscogsSearchResult>,
    ): List<ProviderCandidate> {
        return try {
            block().mapNotNull { it.toCandidate(gson, fallbackBarcode) }
        } catch (error: Throwable) {
            if (error is HttpException && error.code() == 429) {
                throw RateLimitException("Discogs rate limited on $searchLabel.")
            }
            throw error
        }
    }
}

private fun DiscogsSearchResult.toCandidate(
    gson: Gson,
    fallbackBarcode: String? = null,
): ProviderCandidate? {
    val rawTitle = title?.trim().orEmpty()
    if (rawTitle.isBlank()) return null

    val (artistName, releaseTitle) = splitDiscogsTitle(rawTitle)
    val labelName = label?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
    val yearInt = year?.trim()?.toIntOrNull()
    val thumb = thumb?.trim()?.takeIf { it.isNotBlank() }
    val summary = buildFormatSummary(yearInt, labelName, catno)
    val normalizedBarcode = fallbackBarcode?.trim()?.takeIf { it.isNotBlank() }

    return ProviderCandidate(
        provider = "discogs",
        providerItemId = id.toString(),
        title = releaseTitle,
        artist = artistName,
        year = yearInt,
        label = labelName,
        catalogNo = catno?.trim()?.takeIf { it.isNotBlank() },
        formatSummary = summary,
        thumbUrl = thumb,
        barcode = normalizedBarcode,
        rawJson = gson.toJson(this),
    )
}

private fun splitDiscogsTitle(rawTitle: String): Pair<String, String> {
    val parts = rawTitle.split(" - ", limit = 2)
    val artist = parts.getOrNull(0)?.trim().orEmpty()
    val title = parts.getOrNull(1)?.trim().orEmpty()
    if (title.isBlank()) {
        return "Unknown artist" to rawTitle.trim()
    }
    return artist.ifBlank { "Unknown artist" } to title
}

private fun buildFormatSummary(
    year: Int?,
    label: String?,
    catno: String?,
): String? {
    val parts = listOfNotNull(
        year?.toString(),
        label?.trim()?.takeIf { it.isNotBlank() },
        catno?.trim()?.takeIf { it.isNotBlank() },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}
