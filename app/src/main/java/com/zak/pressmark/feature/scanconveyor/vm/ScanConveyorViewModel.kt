package com.zak.pressmark.feature.scanconveyor.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.ReleaseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanConveyorViewModel(
    private val inboxRepository: InboxRepository,
    releaseRepository: ReleaseRepository,
) : ViewModel() {
    val inboxCount: StateFlow<Int> = inboxRepository.observeInboxCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val libraryCount: StateFlow<Int> = releaseRepository.observeReleaseSummaries()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun quickAdd(title: String, artist: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val id = inboxRepository.createQuickAdd(title, artist)
            onDone(id)
        }
    }

    fun addBarcode(barcode: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val id = inboxRepository.createBarcode(barcode)
            onDone(id)
        }
    }
}
