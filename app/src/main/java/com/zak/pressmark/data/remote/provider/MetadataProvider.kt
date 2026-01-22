package com.zak.pressmark.data.remote.provider

import com.zak.pressmark.data.model.inbox.ProviderCandidate

interface MetadataProvider {
    suspend fun lookupByBarcode(barcode: String): List<ProviderCandidate>
    suspend fun lookupByCatalogNo(catalogNo: String, label: String? = null): List<ProviderCandidate>
    suspend fun searchByTitleArtist(title: String, artist: String): List<ProviderCandidate>
    suspend fun searchByTitleLabel(title: String, label: String? = null): List<ProviderCandidate>
}
