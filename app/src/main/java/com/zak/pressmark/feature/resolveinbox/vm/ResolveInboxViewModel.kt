package com.zak.pressmark.feature.resolveinbox.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.local.entity.ProviderSnapshotEntity
import com.zak.pressmark.data.repository.InboxRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResolveInboxViewModel(
    private val inboxItemId: String,
    private val repository: InboxRepository,
) : ViewModel() {
    val inboxItem: StateFlow<InboxItemEntity?> = repository.observeInboxItem(inboxItemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val topCandidates: StateFlow<List<ProviderSnapshotEntity>> = repository.observeTopCandidates(inboxItemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun commit() {
        viewModelScope.launch {
            repository.markCommitted(inboxItemId)
        }
    }
}
