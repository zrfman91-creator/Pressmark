package com.zak.pressmark.feature.devsettings.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.repository.v1.DevSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DevSettingsUiState(
    val ocrDebugOverlayEnabled: Boolean = false,
    val ocrLogCandidatesEnabled: Boolean = false,
)

class DevSettingsViewModel(
    private val repository: DevSettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<DevSettingsUiState> = combine(
        repository.observeOcrDebugOverlayEnabled(),
        repository.observeOcrLogCandidatesEnabled(),
    ) { overlayEnabled, logCandidatesEnabled ->
        DevSettingsUiState(
            ocrDebugOverlayEnabled = overlayEnabled,
            ocrLogCandidatesEnabled = logCandidatesEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DevSettingsUiState())

    fun setOcrDebugOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setOcrDebugOverlayEnabled(enabled)
        }
    }

    fun setOcrLogCandidatesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setOcrLogCandidatesEnabled(enabled)
        }
    }
}
