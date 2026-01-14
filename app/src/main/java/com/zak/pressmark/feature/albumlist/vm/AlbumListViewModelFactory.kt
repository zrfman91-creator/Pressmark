package com.zak.pressmark.feature.albumlist.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository

class AlbumListViewModelFactory(
    private val albumRepo: AlbumRepository,
    private val artistRepo: ArtistRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(AlbumListViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return AlbumListViewModel(
            albumRepository = albumRepo,
            artistRepository = artistRepo,
        ) as T
    }
}
