package com.zak.pressmark.data.model.inbox

enum class InboxSourceType {
    BARCODE,
    COVER_PHOTO,
    QUICK_ADD,
    CSV_IMPORT,
}

enum class OcrStatus {
    NOT_STARTED,
    IN_PROGRESS,
    DONE,
    FAILED,
}

enum class LookupStatus {
    NOT_ELIGIBLE,
    PENDING,
    IN_PROGRESS,
    NEEDS_REVIEW,
    FAILED,
    COMMITTED,
}

enum class InboxErrorCode {
    OFFLINE,
    RATE_LIMIT,
    API_ERROR,
    NO_MATCH,
    NONE,
}
