package com.zak.pressmark.feature.inbox.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.repository.InboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InboxFilter {
    ALL,
    NEEDS_PICK,
    FAILED,
    DRAFTS,
}

class InboxViewModel(
    private val repository: InboxRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(InboxFilter.ALL)

    val inboxItems: StateFlow<List<InboxItemEntity>> = combine(
        repository.observeInboxItems(),
        filter,
    ) { items, filterValue ->
        when (filterValue) {
            InboxFilter.ALL -> items
            InboxFilter.NEEDS_PICK -> items.filter { it.lookupStatus == LookupStatus.NEEDS_REVIEW }
            InboxFilter.FAILED -> items.filter { it.lookupStatus == LookupStatus.FAILED }
            InboxFilter.DRAFTS -> items.filter {
                it.lookupStatus == LookupStatus.NOT_ELIGIBLE || it.lookupStatus == LookupStatus.PENDING
            }
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedFilter: StateFlow<InboxFilter> = filter

    fun setFilter(newFilter: InboxFilter) {
        filter.value = newFilter
    }

    fun retryFailed() {
        viewModelScope.launch {
            repository.observeInboxItems().first()
                .filter { it.lookupStatus == LookupStatus.FAILED }
                .forEach { repository.retryLookup(it.id) }
        }
    }
}
