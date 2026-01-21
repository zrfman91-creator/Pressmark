package com.zak.pressmark.feature.resolveinbox.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.InboxRepository

class ResolveInboxViewModelFactory(
    private val inboxItemId: String,
    private val repository: InboxRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ResolveInboxViewModel(inboxItemId, repository) as T
    }
}
