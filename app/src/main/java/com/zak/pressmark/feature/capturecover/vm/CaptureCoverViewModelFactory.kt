package com.zak.pressmark.feature.capturecover.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.AlbumRepository

class CaptureCoverFlowViewModelFactory(
    private val albumRepository: AlbumRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CaptureCoverFlowViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return CaptureCoverFlowViewModel(
            albumRepository = albumRepository,
        ) as T
    }
}
