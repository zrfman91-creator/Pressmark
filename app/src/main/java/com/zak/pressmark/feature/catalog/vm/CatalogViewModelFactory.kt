// FILE: app/src/main/java/com/zak/pressmark/feature/albumlist/vm/AlbumListViewModelFactory.kt
package com.zak.pressmark.feature.catalog.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.CatalogRepository
import com.zak.pressmark.data.repository.CatalogSettingsRepository

class CatalogViewModelFactory(
    private val catalogRepository: CatalogRepository,
    private val catalogSettingsRepository: CatalogSettingsRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(AlbumListViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return AlbumListViewModel(
            catalogRepository = catalogRepository,
            catalogSettingsRepository = catalogSettingsRepository,
        ) as T
    }
}
