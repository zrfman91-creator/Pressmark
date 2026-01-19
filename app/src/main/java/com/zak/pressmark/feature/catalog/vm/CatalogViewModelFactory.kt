// file: app/src/main/java/com/zak/pressmark/feature/albumlist/vm/AlbumListViewModelFactory.kt
package com.zak.pressmark.feature.catalog.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.local.repository.ReleaseRepository
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository

class CatalogViewModelFactory(
    private val albumRepo: AlbumRepository,
    private val artistRepo: ArtistRepository,
    private val releaseRepo: ReleaseRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(AlbumListViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return AlbumListViewModel(
            albumRepository = albumRepo,
            artistRepository = artistRepo,
            releaseRepository = releaseRepo,
        ) as T
    }
}
