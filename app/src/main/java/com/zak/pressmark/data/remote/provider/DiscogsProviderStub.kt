package com.zak.pressmark.data.remote.provider

import com.zak.pressmark.data.model.inbox.ProviderCandidate

class DiscogsProviderStub : MetadataProvider {
    override suspend fun lookupByBarcode(barcode: String): List<ProviderCandidate> {
        return emptyList() // TODO: Integrate Discogs lookup.
    }

    override suspend fun lookupByCatalogNo(catalogNo: String, label: String?): List<ProviderCandidate> {
        return emptyList() // TODO: Integrate Discogs lookup.
    }

    override suspend fun searchByTitleArtist(title: String, artist: String): List<ProviderCandidate> {
        return emptyList() // TODO: Integrate Discogs lookup.
    }
}
