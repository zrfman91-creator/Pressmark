package com.zak.pressmark.feature.scanconveyor.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.ReleaseRepository

class ScanConveyorViewModelFactory(
    private val inboxRepository: InboxRepository,
    private val releaseRepository: ReleaseRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScanConveyorViewModel(inboxRepository, releaseRepository) as T
    }
}
