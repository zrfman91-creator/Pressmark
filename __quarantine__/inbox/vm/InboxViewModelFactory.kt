package com.zak.pressmark.feature.inbox.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.v1.InboxRepository

class InboxViewModelFactory(
    private val repository: InboxRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InboxViewModel(repository) as T
    }
}
