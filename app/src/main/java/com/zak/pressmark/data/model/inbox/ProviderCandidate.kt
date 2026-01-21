package com.zak.pressmark.data.model.inbox

data class ProviderCandidate(
    val provider: String,
    val providerItemId: String,
    val title: String,
    val artist: String,
    val label: String? = null,
    val catalogNo: String? = null,
    val barcode: String? = null,
    val rawJson: String,
)

data class CandidateScore(
    val confidence: Int,
    val reasonsJson: String,
)
