package com.zak.pressmark.feature.capturecover.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.repository.ReleaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CaptureCoverUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
)

sealed interface CaptureCoverEffect {
    data object Done : CaptureCoverEffect
}

class CaptureCoverFlowViewModel(
    private val releaseRepository: ReleaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureCoverUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CaptureCoverEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    fun saveCover(
        releaseId: String,
        coverUri: String?,
    ) {
        if (releaseId.isBlank()) {
            _effects.tryEmit(CaptureCoverEffect.Done)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = CaptureCoverUiState(isSaving = true)

            runCatching {
                releaseRepository.setLocalCover(
                    releaseId = releaseId,
                    coverUri = coverUri,
                )
            }.onFailure { t ->
                // Best-effort: capture flow should not trap the user.
                _uiState.value = CaptureCoverUiState(
                    isSaving = false,
                    error = t.message ?: "Failed to save cover",
                )
            }

            _effects.tryEmit(CaptureCoverEffect.Done)
        }
    }
}
