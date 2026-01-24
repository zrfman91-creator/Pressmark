package com.zak.pressmark.feature.scanconveyor.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.model.inbox.CsvImportRow
import com.zak.pressmark.data.model.inbox.CsvImportSummary
import com.zak.pressmark.data.model.inbox.InboxErrorCode
import com.zak.pressmark.data.remote.provider.MetadataProvider
import com.zak.pressmark.data.remote.provider.RateLimitException
import com.zak.pressmark.data.repository.CatalogRepository
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.ReleaseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class ScanConveyorViewModel(
    private val inboxRepository: InboxRepository,
    private val metadataProvider: MetadataProvider,
    private val releaseRepository: ReleaseRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {
    val inboxCount: StateFlow<Int> = inboxRepository.observeInboxCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val libraryCount: StateFlow<Int> = catalogRepository
        .observeCatalogItemSummaries(
            query = kotlinx.coroutines.flow.flowOf(""),
            sort = kotlinx.coroutines.flow.flowOf(com.zak.pressmark.feature.catalog.vm.CatalogSort.AddedNewest),
        )
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun quickAdd(title: String, artist: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val id = inboxRepository.createQuickAdd(title, artist)
            onDone(id)
        }
    }

    fun addBarcode(barcode: String, onDone: (String, Boolean) -> Unit) {
        viewModelScope.launch {
            val id = inboxRepository.createBarcode(barcode)
            val autoCommitted = runCatching {
                val candidates = metadataProvider.lookupByBarcode(barcode)
                val errorCode = if (candidates.isEmpty()) InboxErrorCode.NO_MATCH else InboxErrorCode.NONE
                val autoCommitCandidate = inboxRepository.applyLookupResults(id, candidates, errorCode)
                if (autoCommitCandidate != null) {
                    runCatching {
                        val releaseId = releaseRepository.upsertFromProvider(
                            provider = autoCommitCandidate.provider,
                            providerItemId = autoCommitCandidate.providerItemId,
                        )
                    }.getOrNull()?.let {
                        inboxRepository.markCommitted(
                            inboxItemId = id,
                            committedProviderItemId = autoCommitCandidate.providerItemId,
                            releaseId = it,
                        )
                        true
                    } ?: false
                } else {
                    false
                }
            }.getOrElse { error ->
                val errorCode = when (error) {
                    is IOException -> InboxErrorCode.OFFLINE
                    is RateLimitException -> InboxErrorCode.RATE_LIMIT
                    else -> InboxErrorCode.API_ERROR
                }
                inboxRepository.applyLookupResults(id, emptyList(), errorCode)
                false
            }
            onDone(id, autoCommitted)
        }
    }

    suspend fun importCsv(rows: List<CsvImportRow>): CsvImportSummary {
        return inboxRepository.createCsvImport(rows)
    }
}
