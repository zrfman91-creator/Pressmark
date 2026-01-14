package com.zak.pressmark.feature.artworkpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.repository.AlbumRepository

class ArtworkPickerViewModelFactory(
    private val albumRepository: AlbumRepository,
    private val discogsApi: DiscogsApiService,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscogsCoverSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscogsCoverSearchViewModel(
                albumRepository = albumRepository,
                discogsApi = discogsApi,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
