package com.zak.pressmark.feature.scanconveyor.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.core.ocr.TextExtractor
import com.zak.pressmark.data.repository.InboxRepository

class InboxCoverCaptureViewModelFactory(
    private val inboxRepository: InboxRepository,
    private val textExtractor: TextExtractor,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InboxCoverCaptureViewModel(inboxRepository, textExtractor) as T
    }
}
