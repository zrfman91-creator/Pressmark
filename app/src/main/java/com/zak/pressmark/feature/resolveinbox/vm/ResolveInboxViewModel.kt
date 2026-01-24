package com.zak.pressmark.feature.resolveinbox.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.v1.InboxItemEntity
import com.zak.pressmark.data.local.entity.v1.ProviderSnapshotEntity
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.ReleaseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResolveInboxViewModel(
    private val inboxItemId: String,
    private val repository: InboxRepository,
    private val releaseRepository: ReleaseRepository,
) : ViewModel() {
    val inboxItem: StateFlow<InboxItemEntity?> = repository.observeInboxItem(inboxItemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val topCandidates: StateFlow<List<ProviderSnapshotEntity>> = repository.observeTopCandidates(inboxItemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCandidateId = MutableStateFlow<String?>(null)
    val selectedCandidateId: StateFlow<String?> = _selectedCandidateId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _didCommit = MutableStateFlow(false)
    val didCommit: StateFlow<Boolean> = _didCommit.asStateFlow()

    fun selectCandidate(candidateId: String?) {
        _selectedCandidateId.value = candidateId
    }

    fun commitSelectedCandidate(
        candidates: List<ProviderSnapshotEntity>,
        onComplete: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val candidate = candidates.firstOrNull { it.id == _selectedCandidateId.value }
            if (candidate == null) {
                _errorMessage.value = "Pick a candidate first."
                return@launch
            }
            val releaseId = runCatching {
                releaseRepository.upsertFromProvider(
                    provider = candidate.provider,
                    providerItemId = candidate.providerItemId,
                )
            }.getOrNull()
            if (releaseId == null) {
                _errorMessage.value = "Failed to commit release."
                return@launch
            }
            val nextId = repository.getNextInboxItemId(inboxItemId)
            repository.markCommitted(
                inboxItemId = inboxItemId,
                committedProviderItemId = candidate.providerItemId,
                releaseId = releaseId,
            )
            _didCommit.value = true
            onComplete(nextId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun updateManualDetails(
        title: String?,
        artist: String?,
        label: String?,
        catalogNo: String?,
        format: String?,
    ) {
        viewModelScope.launch {
            repository.updateManualDetails(
                inboxItemId = inboxItemId,
                title = title,
                artist = artist,
                label = label,
                catalogNo = catalogNo,
                format = format,
            )
        }
    }
}
