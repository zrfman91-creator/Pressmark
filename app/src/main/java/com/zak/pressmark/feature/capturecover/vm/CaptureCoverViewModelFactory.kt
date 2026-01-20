package com.zak.pressmark.feature.capturecover.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.ReleaseRepository

class CaptureCoverFlowViewModelFactory(
    private val releaseRepository: ReleaseRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CaptureCoverFlowViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return CaptureCoverFlowViewModel(
            releaseRepository = releaseRepository,
        ) as T
    }
}
