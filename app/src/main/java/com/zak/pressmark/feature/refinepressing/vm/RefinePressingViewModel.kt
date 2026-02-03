package com.zak.pressmark.feature.refinepressing.vm

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.app.PressmarkRoutes
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.remote.discogs.DiscogsFormat
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val workId: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val candidates: List<PressingCandidateUi> = emptyList(),
    val applyingReleaseId: Long? = null,
)

@HiltViewModel
class RefinePressingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workRepositoryV2: WorkRepositoryV2,
    private val discogsApi: DiscogsApiService,
) : ViewModel() {

    private val workId: String = checkNotNull(savedStateHandle[PressmarkRoutes.ARG_WORK_ID])

    private val _uiState = MutableStateFlow(RefinePressingUiState(workId = workId, isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RefinePressingEvent>()
    val events = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (BuildConfig.DISCOGS_TOKEN.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Service error. Try again.",
                successMessage = null,
                candidates = emptyList(),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null,
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

                val results = discogsApi.searchReleases(
                    type = "release",
                    artist = work.artistLine,
                    releaseTitle = work.title,
                    perPage = 10,
                    page = 1,
                ).results

                val candidates = results.map { result ->
                    val release = runCatching { discogsApi.getRelease(result.id) }.getOrNull()
                    val label = release?.labels?.firstOrNull()
                    val formatSummary = formatSummary(release?.formats?.firstOrNull())
                    val artworkUrl = result.coverImage
                        ?: result.thumb
                        ?: release?.images?.firstOrNull()?.uri150
                        ?: release?.images?.firstOrNull()?.uri

                    PressingCandidateUi(
                        discogsReleaseId = result.id,
                        title = result.title,
                        year = release?.year ?: result.year,
                        country = release?.country,
                        label = label?.name,
                        catalogNo = label?.catalogNo,
                        formatSummary = formatSummary,
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
                val release = discogsApi.getRelease(candidate.discogsReleaseId)
                val label = release.labels?.firstOrNull()?.name
                val catalogNo = release.labels?.firstOrNull()?.catalogNo

                workRepositoryV2.applyDiscogsPressing(
                    workId = workId,
                    discogsReleaseId = candidate.discogsReleaseId,
                    label = label,
                    catalogNo = catalogNo,
                    country = release.country,
                    year = release.year,
                    artworkUrl = candidate.artworkUrl,
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

    private fun formatSummary(format: DiscogsFormat?): String? {
        val formatName = format?.name?.takeIf { it.isNotBlank() }
        val descriptions = format?.descriptions
            ?.filter { it.isNotBlank() }
            ?.joinToString(" · ")
            ?.takeIf { it.isNotBlank() }

        return listOfNotNull(formatName, descriptions)
            .joinToString(" · ")
            .takeIf { it.isNotBlank() }
    }
}

sealed interface RefinePressingEvent {
    data object Applied : RefinePressingEvent
}
