package com.zak.pressmark.feature.inbox.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.InboxItemEntity
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.repository.InboxEligibility
import com.zak.pressmark.data.repository.InboxRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InboxFilter {
    ALL,
    NEEDS_REVIEW,
    FAILED,
    DRAFTS,
}

sealed interface InboxEffect {
    data class ShowUndoDelete(val inboxItemId: String) : InboxEffect
}

class InboxViewModel(
    private val repository: InboxRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(InboxFilter.ALL)
    private val _effects = MutableSharedFlow<InboxEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    val inboxItems: StateFlow<List<InboxItemEntity>> = combine(
        repository.observeInboxItems(),
        filter,
    ) { items, filterValue ->
        when (filterValue) {
            InboxFilter.ALL -> items
            InboxFilter.NEEDS_REVIEW -> items.filter { InboxEligibility.isNeedsReview(it) }
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

    fun deleteInboxItem(inboxItemId: String) {
        viewModelScope.launch {
            repository.softDeleteInboxItem(inboxItemId)
            Log.d("InboxTriage", "Soft deleted inbox item $inboxItemId")
            _effects.tryEmit(InboxEffect.ShowUndoDelete(inboxItemId))
        }
    }

    fun undoDeleteInboxItem(inboxItemId: String) {
        viewModelScope.launch {
            repository.undoSoftDeleteInboxItem(inboxItemId)
            Log.d("InboxTriage", "Undo delete for inbox item $inboxItemId")
        }
    }

    fun setUnknown(inboxItemId: String, isUnknown: Boolean) {
        viewModelScope.launch {
            repository.setUnknown(inboxItemId, isUnknown)
            Log.d("InboxTriage", "Set unknown=$isUnknown for inbox item $inboxItemId")
        }
    }
}
