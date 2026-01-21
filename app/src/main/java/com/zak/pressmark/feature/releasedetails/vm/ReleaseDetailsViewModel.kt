package com.zak.pressmark.feature.releasedetails.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.model.ReleaseDetails
import com.zak.pressmark.data.model.ReleaseDiscogsExtras
import com.zak.pressmark.data.repository.ReleaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReleaseDetailsUiState(
    val editOpen: Boolean = false,
    val deleteConfirmOpen: Boolean = false,
    val snackMessage: String? = null,
    val didDelete: Boolean = false,
)

class ReleaseDetailsViewModel(
    private val releaseId: String,
    private val releaseRepository: ReleaseRepository,
) : ViewModel() {

    val release: StateFlow<ReleaseDetails?> =
        releaseRepository.observeReleaseDetailsModel(releaseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(ReleaseDetailsUiState())
    val ui: StateFlow<ReleaseDetailsUiState> = _ui.asStateFlow()

    private val _discogsExtras = MutableStateFlow<ReleaseDiscogsExtras?>(null)
    val discogsExtras: StateFlow<ReleaseDiscogsExtras?> = _discogsExtras.asStateFlow()

    init {
        observeDiscogsExtras()
    }

    fun openEdit() {
        _ui.value = _ui.value.copy(editOpen = true)
    }

    fun closeEdit() {
        _ui.value = _ui.value.copy(editOpen = false)
    }

    fun openDeleteConfirm() {
        _ui.value = _ui.value.copy(deleteConfirmOpen = true)
    }

    fun closeDeleteConfirm() {
        _ui.value = _ui.value.copy(deleteConfirmOpen = false)
    }

    fun saveEdits(
        title: String,
        rawArtist: String,
        releaseYear: Int?,
        label: String?,
        catalogNo: String?,
        format: String?,
        barcode: String?,
        country: String?,
        releaseType: String?,
        notes: String?,
    ) {
        val t = title.trim()
        val a = rawArtist.trim()

        if (t.isBlank()) {
            _ui.value = _ui.value.copy(snackMessage = "Title is required")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = release.value
                val updated = releaseRepository.updateReleaseDetails(
                    releaseId = releaseId,
                    title = t,
                    rawArtist = a,
                    releaseYear = releaseYear,
                    label = label,
                    catalogNo = catalogNo,
                    format = format,
                    barcode = barcode,
                    country = country,
                    releaseType = releaseType,
                    notes = notes,
                    rating = current?.rating,
                    lastPlayedAt = current?.lastPlayedAt,
                )

                if (updated == 0) {
                    reportError("Release not found.")
                    return@launch
                }

                withContext(Dispatchers.Main.immediate) {
                    _ui.value = _ui.value.copy(editOpen = false)
                }
            } catch (t: Throwable) {
                reportError(t.message ?: "Failed to save changes.")
            }
        }
    }

    fun deleteRelease() {
        if (releaseId.isBlank()) {
            _ui.value = _ui.value.copy(snackMessage = "Release not found.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                releaseRepository.deleteRelease(releaseId)
                withContext(Dispatchers.Main.immediate) {
                    _ui.value = _ui.value.copy(deleteConfirmOpen = false, didDelete = true)
                }
            } catch (t: Throwable) {
                reportError(t.message ?: "Failed to delete release.")
            }
        }
    }

    fun setDiscogsCover(
        coverUrl: String?,
        discogsReleaseId: Long?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                releaseRepository.setDiscogsCover(
                    releaseId = releaseId,
                    coverUrl = coverUrl,
                    discogsReleaseId = discogsReleaseId,
                )
            } catch (t: Throwable) {
                reportError(t.message ?: "Failed to set Discogs cover.")
            }
        }
    }

    fun clearCover() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                releaseRepository.setLocalCover(
                    releaseId = releaseId,
                    coverUri = null,
                )
            } catch (t: Throwable) {
                reportError(t.message ?: "Failed to clear cover.")
            }
        }
    }

    fun dismissSnack() {
        _ui.value = _ui.value.copy(snackMessage = null)
    }

    private fun observeDiscogsExtras() {
        viewModelScope.launch(Dispatchers.IO) {
            release
                .map { it?.discogsReleaseId }
                .distinctUntilChanged()
                .collect { discogsId ->
                    if (discogsId == null) {
                        _discogsExtras.value = null
                        return@collect
                    }

                    val extras = releaseRepository.fetchDiscogsExtras(releaseId)
                    _discogsExtras.value = extras
                }
        }
    }

    private fun reportError(message: String) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _ui.value = _ui.value.copy(snackMessage = message)
        }
    }
}
