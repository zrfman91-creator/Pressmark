package com.zak.pressmark.feature.catalogdetails.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.app.di.AppGraph

class CatalogDetailsViewModelFactory(
    private val graph: AppGraph,
    private val catalogItemId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CatalogDetailsViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        return CatalogDetailsViewModel(
            catalogItemId = catalogItemId,
            catalogRepository = graph.catalogRepository,
        ) as T
    }
}
