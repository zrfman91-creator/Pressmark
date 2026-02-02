package com.zak.pressmark.feature.refinepressing.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.refinepressing.screen.RefinePressingScreen
import com.zak.pressmark.feature.refinepressing.vm.RefinePressingViewModel

@Composable
fun RefinePressingRoute(
    onBack: () -> Unit,
    vm: RefinePressingViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    RefinePressingScreen(
        state = state,
        onRetry = vm::refresh,
        onBack = onBack,
    )
}
