package com.zak.pressmark.feature.addalbum.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.AlbumRepository
import com.zak.pressmark.data.repository.ArtistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi

class AddAlbumViewModelFactory(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
) : ViewModelProvider.Factory {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddAlbumViewModel::class.java)) {
            return AddAlbumViewModel(
                albumRepository = albumRepository,
                artistRepository = artistRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
