package com.zak.pressmark.feature.scanconveyor.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.remote.provider.MetadataProvider
import com.zak.pressmark.data.repository.CatalogRepository
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.ReleaseRepository

class ScanConveyorViewModelFactory(
    private val inboxRepository: InboxRepository,
    private val metadataProvider: MetadataProvider,
    private val releaseRepository: ReleaseRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScanConveyorViewModel(inboxRepository, metadataProvider, releaseRepository, catalogRepository) as T
    }
}
