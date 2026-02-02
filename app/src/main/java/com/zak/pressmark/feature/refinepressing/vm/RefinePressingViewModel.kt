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
    val label: String?,
    val catalogNo: String?,
    val year: Int?,
    val country: String?,
)

data class RefinePressingUiState(
    val workId: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val candidates: List<PressingCandidateUi> = emptyList(),
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
                candidates = emptyList(),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val work = workRepositoryV2.getWork(workId)
                if (work == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Work not found.",
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

                val candidates = results.take(5).mapNotNull { result ->
                    val release = runCatching { discogsApi.getRelease(result.id) }.getOrNull()
                    val labelInfo = release?.labels?.firstOrNull()
                    PressingCandidateUi(
                        discogsReleaseId = result.id,
                        title = result.title,
                        label = labelInfo?.name?.trim()?.takeIf { it.isNotBlank() },
                        catalogNo = labelInfo?.catalogNo?.trim()?.takeIf { it.isNotBlank() },
                        year = release?.year ?: result.year,
                        country = release?.country?.trim()?.takeIf { it.isNotBlank() },
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    candidates = candidates,
                )
            } catch (t: Throwable) {
                Log.e("RefinePressingViewModel", "Failed to fetch Discogs matches.", t)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapUserSafeError(t),
                    candidates = emptyList(),
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
