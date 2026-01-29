package com.zak.pressmark.feature.artistdetails.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.app.PressmarkRoutes
import com.zak.pressmark.data.repository.v2.ArtistCompletionRepository
import com.zak.pressmark.data.repository.v2.CanonicalWorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ArtistDetailsUiState(
    val artistName: String = "",
    val ownedCount: Int = 0,
    val totalCount: Int = 0,
    val missing: List<ArtistCompletionRepository.MissingWork> = emptyList(),
    val isComplete: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ArtistDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val canonicalWorkRepositoryV2: CanonicalWorkRepositoryV2,
    private val artistCompletionRepository: ArtistCompletionRepository,
) : ViewModel() {

    private val artistName: String = checkNotNull(savedStateHandle[PressmarkRoutes.ARG_ARTIST_NAME])

    private val _uiState = MutableStateFlow(ArtistDetailsUiState(artistName = artistName))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val artistId = canonicalWorkRepositoryV2.getOrCreateArtistId(artistName)
            artistCompletionRepository.observeArtistCompletion(artistId)
                .collect { summary ->
                    _uiState.value = ArtistDetailsUiState(
                        artistName = artistName,
                        ownedCount = summary.ownedCount,
                        totalCount = summary.totalCount,
                        missing = summary.missing,
                        isComplete = summary.isComplete,
                        isLoading = false,
                    )
                }
        }
    }
}
