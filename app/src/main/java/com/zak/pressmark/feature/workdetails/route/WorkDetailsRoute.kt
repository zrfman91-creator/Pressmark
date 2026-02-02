package com.zak.pressmark.feature.workdetails.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zak.pressmark.feature.workdetails.screen.WorkDetailsScreen
import com.zak.pressmark.feature.workdetails.vm.WorkDetailsViewModel

@Composable
fun WorkDetailsRoute(
    onBack: () -> Unit,
    onRefinePressing: (String) -> Unit,
    vm: WorkDetailsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()

    WorkDetailsScreen(
        isMissing = state.isMissing,
        artworkUri = state.artworkUri,
        title = state.title,
        artistLine = state.artistLine,
        year = state.year,
        genres = state.genres,
        styles = state.styles,
        discogsMasterId = state.discogsMasterId,
        onBack = onBack,
        onRefinePressing = { onRefinePressing(state.workId) },
        onDeleteConfirmed = {
            vm.deleteWork()
            onBack()
        },
    )
}
