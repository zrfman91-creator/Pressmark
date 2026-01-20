package com.zak.pressmark.data.remote.discogs

/**
 * Normalized metadata extracted from a Discogs release payload.
 */
data class DiscogsReleaseMetadata(
    val format: String?,
    val country: String?,
    val releaseType: String?,
    val notes: String?,
)

fun DiscogsRelease.toReleaseMetadata(): DiscogsReleaseMetadata {
    return DiscogsReleaseMetadata(
        format = normalizeFormat(formats),
        country = country?.trim()?.takeIf { it.isNotBlank() },
        releaseType = normalizeReleaseType(styles, genres),
        notes = notes?.trim()?.takeIf { it.isNotBlank() },
    )
}

private fun normalizeFormat(formats: List<DiscogsFormat>?): String? {
    val name = formats
        ?.firstOrNull()
        ?.name
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    val lower = name.lowercase()
    return when {
        lower.contains("7") && lower.contains("inch") -> "7\""
        lower.contains("12") && lower.contains("inch") -> "12\""
        lower.contains("lp") || lower.contains("long play") -> "LP"
        lower.contains("ep") -> "EP"
        lower.contains("cd") -> "CD"
        lower.contains("cassette") -> "CASSETTE"
        lower.contains("vinyl") -> "LP"
        lower.contains("dvd") -> "DVD"
        lower.contains("blu-ray") || lower.contains("bluray") -> "BLU-RAY"
        lower.contains("file") || lower.contains("digital") -> "DIGITAL"
        else -> name.uppercase()
    }
}

private fun normalizeReleaseType(styles: List<String>?, genres: List<String>?): String? {
    val tokens = (styles.orEmpty() + genres.orEmpty())
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (tokens.isEmpty()) return null

    val normalized = tokens.map { it.lowercase() }

    return when {
        normalized.any { it.contains("live") } -> "LIVE"
        normalized.any { it.contains("compilation") } -> "COMPILATION"
        normalized.any { it.contains("soundtrack") } -> "SOUNDTRACK"
        normalized.any { it.contains("greatest hits") || it.contains("best of") } -> "GREATEST_HITS"
        else -> null
    }
}
