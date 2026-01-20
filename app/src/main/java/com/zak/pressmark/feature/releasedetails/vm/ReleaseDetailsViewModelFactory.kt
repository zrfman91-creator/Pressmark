package com.zak.pressmark.feature.releasedetails.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.app.di.AppGraph

class ReleaseDetailsViewModelFactory(
    private val graph: AppGraph,
    private val releaseId: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(ReleaseDetailsViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        return ReleaseDetailsViewModel(
            releaseId = releaseId,
            releaseRepository = graph.releaseRepository,
        ) as T
    }
}
