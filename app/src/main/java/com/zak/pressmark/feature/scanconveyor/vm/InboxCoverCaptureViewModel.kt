package com.zak.pressmark.feature.scanconveyor.vm

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.core.ocr.TextExtractor
import com.zak.pressmark.data.model.inbox.ReasonCode
import com.zak.pressmark.data.repository.ExtractedFields
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.repository.OcrParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

data class InboxCoverCaptureUiState(
    val isSaving: Boolean = false,
    val sessionActive: Boolean = false,
    val elapsedMs: Long = 0L,
    val stableCount: Int = 0,
    val lastLines: List<String> = emptyList(),
    val bestFields: ExtractedFields? = null,
    val confidenceScore: Int? = null,
    val reasonCodes: List<String> = emptyList(),
)

sealed interface InboxCoverCaptureEffect {
    data object Done : InboxCoverCaptureEffect
}

class InboxCoverCaptureViewModel(
    private val inboxRepository: InboxRepository,
    private val textExtractor: TextExtractor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InboxCoverCaptureUiState())
    val uiState: StateFlow<InboxCoverCaptureUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<InboxCoverCaptureEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    fun saveCover(uri: Uri, logCandidates: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, sessionActive = true)
            val session = runOcrSession(uri, logCandidates)
            Log.d(
                "OcrSession",
                "OCR session done score=${session.confidenceScore} reasons=${session.reasonCodes.joinToString(",")}"
            )
            val inboxId = inboxRepository.createCoverCapture(uri)
            if (session.success) {
                inboxRepository.applyOcrResult(
                    inboxItemId = inboxId,
                    extractedFields = session.fields,
                    success = true,
                    confidenceScore = session.confidenceScore,
                    confidenceReasonsJson = ReasonCode.encode(session.reasonCodes),
                )
            }
            _uiState.value = InboxCoverCaptureUiState(isSaving = false)
            _effects.tryEmit(InboxCoverCaptureEffect.Done)
        }
    }

    private suspend fun runOcrSession(uri: Uri, logCandidates: Boolean): OcrSessionResult {
        val start = System.currentTimeMillis()
        val timeoutMs = OCR_SESSION_TIMEOUT_MS
        var attempt = 0
        var stableCount = 0
        var best = OcrSessionCandidate()
        val candidates = mutableListOf<OcrSessionCandidate>()

        while (System.currentTimeMillis() - start < timeoutMs && attempt < OCR_MAX_ATTEMPTS) {
            attempt++
            val result = textExtractor.extract(uri)
            val lines = result.getOrNull()?.lines.orEmpty()
            val fields = OcrParser.parse(lines)
            val candidate = buildCandidate(fields)
            candidates += candidate

            if (logCandidates) {
                Log.d(
                    "OcrSession",
                    "OCR candidate #$attempt score=${candidate.confidenceScore} " +
                        "title=${fields.title} artist=${fields.artist}"
                )
            }

            if (candidate.confidenceScore > best.confidenceScore) {
                best = candidate
                stableCount = 0
            } else if (candidate.isSameAs(best)) {
                stableCount += 1
            }

            _uiState.value = _uiState.value.copy(
                sessionActive = true,
                elapsedMs = System.currentTimeMillis() - start,
                stableCount = stableCount,
                lastLines = lines,
                bestFields = best.fields,
                confidenceScore = best.confidenceScore,
                reasonCodes = best.reasonCodes,
            )

            if (stableCount >= OCR_STABLE_REQUIRED) break
            delay(OCR_FRAME_DELAY_MS)
        }

        val reasonCodes = best.reasonCodes.toMutableList()
        val runnerUp = candidates
            .filter { it.fields != null }
            .sortedByDescending { it.confidenceScore }
            .getOrNull(1)
        if (runnerUp != null && abs(best.confidenceScore - runnerUp.confidenceScore) <= OCR_RUNNER_UP_GAP) {
            reasonCodes += ReasonCode.MULTIPLE_CANDIDATES
        }

        if (best.confidenceScore < OCR_LOW_CONFIDENCE_THRESHOLD) {
            reasonCodes += ReasonCode.LOW_SIGNAL
        }

        return OcrSessionResult(
            success = best.fields != null,
            fields = best.fields ?: ExtractedFields(null, null, null, null),
            confidenceScore = best.confidenceScore,
            reasonCodes = reasonCodes.distinct(),
        )
    }

    private fun buildCandidate(fields: ExtractedFields): OcrSessionCandidate {
        val reasonCodes = mutableListOf<String>()
        val titlePresent = !fields.title.isNullOrBlank()
        val artistPresent = !fields.artist.isNullOrBlank()
        val labelPresent = !fields.label.isNullOrBlank()
        val catnoPresent = !fields.catalogNo.isNullOrBlank()

        if (!titlePresent) reasonCodes += ReasonCode.MISSING_TITLE
        if (!artistPresent) reasonCodes += ReasonCode.MISSING_ARTIST

        val score = (if (titlePresent) 40 else 0) +
            (if (artistPresent) 40 else 0) +
            (if (labelPresent) 10 else 0) +
            (if (catnoPresent) 10 else 0)

        return OcrSessionCandidate(
            fields = if (titlePresent || artistPresent || labelPresent || catnoPresent) fields else null,
            confidenceScore = score,
            reasonCodes = reasonCodes,
        )
    }
}

private data class OcrSessionCandidate(
    val fields: ExtractedFields? = null,
    val confidenceScore: Int = 0,
    val reasonCodes: List<String> = emptyList(),
) {
    fun isSameAs(other: OcrSessionCandidate): Boolean {
        val left = fields ?: return false
        val right = other.fields ?: return false
        return left.title == right.title && left.artist == right.artist
    }
}

private data class OcrSessionResult(
    val success: Boolean,
    val fields: ExtractedFields,
    val confidenceScore: Int,
    val reasonCodes: List<String>,
)

private const val OCR_SESSION_TIMEOUT_MS = 4_000L
private const val OCR_FRAME_DELAY_MS = 650L
private const val OCR_MAX_ATTEMPTS = 5
private const val OCR_STABLE_REQUIRED = 2
private const val OCR_RUNNER_UP_GAP = 5
private const val OCR_LOW_CONFIDENCE_THRESHOLD = 60
