package com.zak.pressmark.feature.resolveinbox.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.ReleaseRepository

class ResolveInboxViewModelFactory(
    private val inboxItemId: String,
    private val repository: InboxRepository,
    private val releaseRepository: ReleaseRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ResolveInboxViewModel(inboxItemId, repository, releaseRepository) as T
    }
}
