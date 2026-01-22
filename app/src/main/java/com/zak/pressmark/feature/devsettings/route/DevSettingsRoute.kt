package com.zak.pressmark.feature.devsettings.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zak.pressmark.data.repository.DevSettingsRepository
import com.zak.pressmark.feature.devsettings.screen.DevSettingsScreen
import com.zak.pressmark.feature.devsettings.vm.DevSettingsViewModel
import com.zak.pressmark.feature.devsettings.vm.DevSettingsViewModelFactory

@Composable
fun DevSettingsRoute(
    repository: DevSettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val factory = DevSettingsViewModelFactory(repository)
    val vm: DevSettingsViewModel = viewModel(factory = factory)
    val uiState = vm.uiState.collectAsStateWithLifecycle().value

    DevSettingsScreen(
        uiState = uiState,
        onToggleOcrDebugOverlay = vm::setOcrDebugOverlayEnabled,
        onToggleOcrLogCandidates = vm::setOcrLogCandidatesEnabled,
        onBack = onBack,
        modifier = modifier,
    )
}
