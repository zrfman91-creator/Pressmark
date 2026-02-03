// FILE: app/src/main/java/com/zak/pressmark/feature/workdetails/vm/WorkDetailsViewModel.kt
package com.zak.pressmark.feature.workdetails.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.app.PressmarkRoutes
import com.zak.pressmark.core.analytics.UxEventLogger
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkDetailsUiState(
    val workId: String = "",
    val title: String = "",
    val artistLine: String = "",
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val styles: List<String> = emptyList(),

    // Canonical/master artwork (Discogs master / work artwork).
    val masterArtworkUri: String? = null,

    // Owned/selected pressing artwork (Discogs release artwork).
    val selectedPressingArtworkUri: String? = null,

    val discogsMasterId: Long? = null,
    val isMissing: Boolean = false,

    // Chosen refinement (Variant: default) -> Pressing/Release details
    val selectedPressingLabel: String? = null,
    val selectedPressingCatalogNo: String? = null,
    val selectedPressingCountry: String? = null,
    val selectedPressingYear: Int? = null,
    val selectedPressingFormat: String? = null,
    val selectedDiscogsReleaseId: Long? = null,
)

@HiltViewModel
class WorkDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workRepositoryV2: WorkRepositoryV2,
    private val uxEventLogger: UxEventLogger,
) : ViewModel() {

    private val workId: String = checkNotNull(savedStateHandle[PressmarkRoutes.ARG_WORK_ID])

    private val _uiState = MutableStateFlow(WorkDetailsUiState(workId = workId))
    val uiState = _uiState.asStateFlow()

    init {
        // Base work info (MASTER/CANONICAL).
        viewModelScope.launch {
            workRepositoryV2.observeWork(workId).collect { work ->
                if (work == null) {
                    _uiState.update { it.copy(isMissing = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            workId = work.id,
                            title = work.title,
                            artistLine = work.artistLine,
                            year = work.year,
                            genres = parseJsonList(work.genresJson),
                            styles = parseJsonList(work.stylesJson),
                            masterArtworkUri = work.primaryArtworkUri,
                            discogsMasterId = work.discogsMasterId,
                            isMissing = false,
                        )
                    }
                }
            }
        }

        // Selected pressing refinement details (OWNED/SELECTED).
        viewModelScope.launch {
            workRepositoryV2.observeSelectedPressingDetails(workId).collect { details ->
                _uiState.update {
                    it.copy(
                        selectedPressingLabel = details?.label,
                        selectedPressingCatalogNo = details?.catalogNo,
                        selectedPressingCountry = details?.country,
                        selectedPressingYear = details?.year,
                        selectedPressingFormat = details?.format,
                        selectedDiscogsReleaseId = details?.discogsReleaseId,
                        selectedPressingArtworkUri = details?.artworkUri,
                    )
                }
            }
        }
    }

    fun deleteWork() {
        viewModelScope.launch {
            workRepositoryV2.deleteWork(workId)
            uxEventLogger.logEvent("pm_work_deleted", mapOf("source" to "details"))
        }
    }

    private fun parseJsonList(raw: String): List<String> {
        val trimmed = raw.trim().removePrefix("[").removeSuffix("]").trim()
        if (trimmed.isBlank()) return emptyList()
        return trimmed.split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }
}
