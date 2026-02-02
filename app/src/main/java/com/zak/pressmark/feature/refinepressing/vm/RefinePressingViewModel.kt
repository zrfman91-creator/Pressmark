package com.zak.pressmark.feature.refinepressing.vm

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.BuildConfig
import com.zak.pressmark.app.PressmarkRoutes
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class PressingCandidateUi(
    val discogsReleaseId: Long,
    val title: String,
    val year: Int?,
    val country: String?,
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
                    PressingCandidateUi(
                        discogsReleaseId = result.id,
                        title = result.title,
                        year = result.year,
                        country = null,
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
                )

                _uiState.value = _uiState.value.copy(
                    applyingReleaseId = null,
                    successMessage = "Pressing saved.",
                )
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
