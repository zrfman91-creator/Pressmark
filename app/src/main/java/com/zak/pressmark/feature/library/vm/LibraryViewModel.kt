// FILE: app/src/main/java/com/zak/pressmark/feature/library/vm/LibraryViewModel.kt
package com.zak.pressmark.feature.library.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryItemUi(
    val workId: String,
    val title: String,
    val artistLine: String,
    val year: Int?,
    val artworkUri: String?,
)

data class LibraryUiState(
    val items: List<LibraryItemUi> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val workRepositoryV2: WorkRepositoryV2,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            workRepositoryV2.observeAllWorks()
                .collect { works ->
                    _uiState.value = LibraryUiState(
                        items = works.map { w ->
                            LibraryItemUi(
                                workId = w.id,
                                title = w.title,
                                artistLine = w.artistLine,
                                year = w.year,
                                artworkUri = w.primaryArtworkUri,
                            )
                        },
                    )
                }
        }
    }

    fun deleteWork(workId: String) {
        viewModelScope.launch {
            workRepositoryV2.deleteWork(workId)
        }
    }
}
