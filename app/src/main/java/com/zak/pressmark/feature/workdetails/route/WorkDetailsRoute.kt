// FILE: app/src/main/java/com/zak/pressmark/feature/workdetails/route/WorkDetailsRoute.kt
package com.zak.pressmark.feature.workdetails.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.workdetails.screen.WorkDetailsScreen
import com.zak.pressmark.feature.workdetails.vm.WorkDetailsViewModel

@Composable
fun WorkDetailsRoute(
    onBack: () -> Unit,
    onRefinePressing: (String) -> Unit,
    vm: WorkDetailsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    WorkDetailsScreen(
        isMissing = state.isMissing,
        masterArtworkUri = state.masterArtworkUri,
        title = state.title,
        artistLine = state.artistLine,
        year = state.year,
        genres = state.genres,
        styles = state.styles,
        discogsMasterId = state.discogsMasterId,

        selectedPressingLabel = state.selectedPressingLabel,
        selectedPressingCatalogNo = state.selectedPressingCatalogNo,
        selectedPressingCountry = state.selectedPressingCountry,
        selectedPressingYear = state.selectedPressingYear,
        selectedPressingFormat = state.selectedPressingFormat,
        selectedDiscogsReleaseId = state.selectedDiscogsReleaseId,
        selectedPressingArtworkUri = state.selectedPressingArtworkUri,

        onBack = onBack,
        onRefinePressing = { onRefinePressing(state.workId) },
        onDeleteConfirmed = { vm.deleteWork() ; onBack() },
    )
}
