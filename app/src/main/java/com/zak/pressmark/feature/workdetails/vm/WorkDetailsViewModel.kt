package com.zak.pressmark.feature.workdetails.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.app.PressmarkRoutes
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkDetailsUiState(
    val workId: String = "",
    val title: String = "",
    val artistLine: String = "",
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
    val artworkUri: String? = null,
    val discogsMasterId: Long? = null,
    val isMissing: Boolean = false,
)

@HiltViewModel
class WorkDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workRepositoryV2: WorkRepositoryV2,
) : ViewModel() {

    private val workId: String = checkNotNull(savedStateHandle[PressmarkRoutes.ARG_WORK_ID])

    private val _uiState = MutableStateFlow(WorkDetailsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            workRepositoryV2.observeWork(workId)
                .collect { work ->
                    if (work == null) {
                        _uiState.value = WorkDetailsUiState(isMissing = true)
                    } else {
                        _uiState.value = WorkDetailsUiState(
                            workId = work.id,
                            title = work.title,
                            artistLine = work.artistLine,
                            year = work.year,
                            genres = parseJsonList(work.genresJson),
                            styles = parseJsonList(work.stylesJson),
                            artworkUri = work.primaryArtworkUri,
                            discogsMasterId = work.discogsMasterId,
                            isMissing = false,
                        )
                    }
                }
        }
    }

    fun deleteWork() {
        viewModelScope.launch {
            workRepositoryV2.deleteWork(workId)
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
