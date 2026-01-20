package com.zak.pressmark.feature.artworkpicker.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.repository.ReleaseRepository

class ArtworkPickerViewModelFactory(
    private val releaseRepository: ReleaseRepository,
    private val discogsApi: DiscogsApiService,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscogsCoverSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscogsCoverSearchViewModel(
                releaseRepository = releaseRepository,
                discogsApi = discogsApi,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
