package com.zak.pressmark.feature.catalogdetails.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.model.CatalogItemDetails
import com.zak.pressmark.data.repository.v1.CatalogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CatalogDetailsViewModel(
    catalogItemId: String,
    catalogRepository: CatalogRepository,
) : ViewModel() {
    val details: StateFlow<CatalogItemDetails?> =
        catalogRepository.observeCatalogItemDetails(catalogItemId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
