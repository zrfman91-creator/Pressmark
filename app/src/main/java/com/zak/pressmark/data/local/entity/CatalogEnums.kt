package com.zak.pressmark.data.local.entity

/**
 * Provider identifiers for external metadata sources.
 * Keep values stable; persisted in DB via TypeConverters.
 */
enum class Provider {
    DISCOGS,
    MUSICBRAINZ,
    OTHER
}

enum class CatalogItemState {
    MASTER_ONLY,
    CANDIDATES_PRESENTED,
    RELEASE_CONFIRMED,
    RELEASE_CORRECTED
}

enum class EvidenceType {
    BARCODE,
    CATNO,
    RUNOUT,
    LABEL,
    PHOTO,
    TEXT_OCR
}

enum class EvidenceSource {
    USER_INPUT,
    BARCODE_SCAN,
    OCR_COVER,
    OCR_SPINE,
    API_LOOKUP,
    IMPORT
}

enum class VerificationEventType {
    CANDIDATE_PRESENTED,
    CONFIRM,
    REJECT,
    CORRECT
}
