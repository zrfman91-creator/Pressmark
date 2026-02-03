package com.zak.pressmark.feature.refinepressing.vm

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.app.PressmarkRoutes
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import com.zak.pressmark.domain.artwork.ArtworkResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

data class PressingCandidateUi(
    val discogsReleaseId: Long,
    val title: String,
    val year: Int?,
    val country: String?,
    val label: String?,
    val catalogNo: String?,
    val formatSummary: String?,
    val artworkUrl: String?,
)

data class RefinePressingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val candidates: List<PressingCandidateUi> = emptyList(),
    val applyingReleaseId: Long? = null,
)

sealed interface RefinePressingEvent {
    data object Applied : RefinePressingEvent
}

@HiltViewModel
class RefinePressingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val discogsApi: DiscogsApiService,
    private val workRepositoryV2: WorkRepositoryV2,
    private val artworkResolver: ArtworkResolver,
) : ViewModel() {

    private val workId: String = checkNotNull(savedStateHandle[PressmarkRoutes.ARG_WORK_ID])

    private val _uiState = MutableStateFlow(RefinePressingUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RefinePressingEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = _events.asSharedFlow()

    init {
        fetchCandidates()
    }

    fun retry() {
        fetchCandidates()
    }

    private fun fetchCandidates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null,
                candidates = emptyList(),
            )

            try {
                val work = workRepositoryV2.getWork(workId)
                if (work == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Work not found.",
                        successMessage = null,
                        candidates = emptyList(),
                    )
                    return@launch
                }

                // Option A: fast candidate list. Do NOT hydrate each candidate with getRelease().
                val results = discogsApi.searchReleases(
                    artist = work.artistLine,
                    releaseTitle = work.title,
                    perPage = 10,
                    page = 1,
                ).results

                val candidates = results.map { result ->
                    // Keep list lightweight: only what search already returns.
                    val artworkUrl = result.coverImage ?: result.thumb

                    PressingCandidateUi(
                        discogsReleaseId = result.id,
                        title = result.title,
                        year = result.year,
                        country = null,
                        label = null,
                        catalogNo = null,
                        formatSummary = null,
                        artworkUrl = artworkUrl,
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    successMessage = null,
                    candidates = candidates,
                )
            } catch (t: Throwable) {
                Log.e("RefinePressingViewModel", "Failed to fetch Discogs matches.", t)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapUserSafeError(t),
                    successMessage = null,
                    candidates = emptyList(),
                )
            }
        }
    }

    fun applyCandidate(candidate: PressingCandidateUi) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                applyingReleaseId = candidate.discogsReleaseId,
                errorMessage = null,
                successMessage = null,
            )
            try {
                // Only hydrate on user action (one call).
                val release = discogsApi.getRelease(candidate.discogsReleaseId)

                val label = release.labels?.firstOrNull()?.name
                val catalogNo = release.labels?.firstOrNull()?.catalogNo

                // Prefer full primary release image when persisting pressing art (via resolver).
                val persistedArt = artworkResolver
                    .resolveDiscogsReleaseArt(
                        releaseId = candidate.discogsReleaseId,
                        fallbackUrl = candidate.artworkUrl,
                    )
                    ?.url
                    ?: candidate.artworkUrl

                workRepositoryV2.applyDiscogsPressing(
                    workId = workId,
                    discogsReleaseId = candidate.discogsReleaseId,
                    label = label,
                    catalogNo = catalogNo,
                    country = release.country,
                    year = release.year,
                    artworkUrl = persistedArt,
                )

                _uiState.value = _uiState.value.copy(
                    applyingReleaseId = null,
                    successMessage = "Pressing saved.",
                )
                _events.emit(RefinePressingEvent.Applied)
            } catch (t: Throwable) {
                Log.e("RefinePressingViewModel", "Failed to apply Discogs pressing.", t)
                _uiState.value = _uiState.value.copy(
                    applyingReleaseId = null,
                    errorMessage = mapUserSafeError(t),
                )
            }
        }
    }

    private fun mapUserSafeError(t: Throwable): String {
        return when (t) {
            is IOException -> "No connection. Try again."
            is HttpException -> "Service error. Try again."
            else -> "Service error. Try again."
        }
    }
}
