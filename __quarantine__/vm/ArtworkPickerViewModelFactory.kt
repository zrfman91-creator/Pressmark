package com.zak.pressmark.feature.artworkpicker.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.v1.ReleaseRepository

class ArtworkPickerViewModelFactory(
    private val releaseRepository: ReleaseRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscogsCoverSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscogsCoverSearchViewModel(
                releaseRepository = releaseRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
