package com.zak.pressmark.feature.scanconveyor.vm

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.repository.InboxRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InboxCoverCaptureUiState(
    val isSaving: Boolean = false,
)

sealed interface InboxCoverCaptureEffect {
    data object Done : InboxCoverCaptureEffect
}

class InboxCoverCaptureViewModel(
    private val inboxRepository: InboxRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InboxCoverCaptureUiState())
    val uiState: StateFlow<InboxCoverCaptureUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<InboxCoverCaptureEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    fun saveCover(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = InboxCoverCaptureUiState(isSaving = true)
            inboxRepository.createCoverCapture(uri)
            _uiState.value = InboxCoverCaptureUiState(isSaving = false)
            _effects.tryEmit(InboxCoverCaptureEffect.Done)
        }
    }
}
