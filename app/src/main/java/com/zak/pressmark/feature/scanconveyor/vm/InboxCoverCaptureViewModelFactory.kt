package com.zak.pressmark.feature.scanconveyor.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.InboxRepository

class InboxCoverCaptureViewModelFactory(
    private val inboxRepository: InboxRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InboxCoverCaptureViewModel(inboxRepository) as T
    }
}
