package com.zak.pressmark.feature.refinepressing.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.refinepressing.vm.RefinePressingEvent
import com.zak.pressmark.feature.refinepressing.vm.RefinePressingViewModel
import com.zak.pressmark.feature.refinepressing.screen.RefinePressingScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RefinePressingRoute(
    workId: String,
    onBack: () -> Unit,
) {
    val vm: RefinePressingViewModel = hiltViewModel()
    val state = vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.events.collectLatest { event ->
            when (event) {
                RefinePressingEvent.Applied -> onBack()
            }
        }
    }

    RefinePressingScreen(
        state = state.value,
        onBack = onBack,
        onRefresh = vm::refresh,
        onApplyCandidate = vm::applyCandidate,
    )
}
