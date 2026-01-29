package com.zak.pressmark.core.util.completion

object CompletionRules {
    const val RELEASE_TYPE_STUDIO = "STUDIO"
    const val RELEASE_TYPE_LIVE = "LIVE"
    const val RELEASE_TYPE_COMPILATION = "COMPILATION"
    const val RELEASE_TYPE_SOUNDTRACK = "SOUNDTRACK"
    const val RELEASE_TYPE_GREATEST_HITS = "GREATEST_HITS"
    const val RELEASE_TYPE_REMIX = "REMIX"
    const val RELEASE_TYPE_BOOTLEG = "BOOTLEG"
    const val RELEASE_TYPE_BOX_SET = "BOX_SET"
    const val RELEASE_TYPE_SINGLE = "SINGLE"

    const val FORMAT_LP = "LP"
    const val FORMAT_EP = "EP"

    val studioFormats = listOf(FORMAT_LP, FORMAT_EP)

    fun inferReleaseType(styles: List<String>?, genres: List<String>?): String {
        val tokens = (styles.orEmpty() + genres.orEmpty())
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        return when {
            tokens.any { it.contains("live") } -> RELEASE_TYPE_LIVE
            tokens.any { it.contains("compilation") } -> RELEASE_TYPE_COMPILATION
            tokens.any { it.contains("soundtrack") } -> RELEASE_TYPE_SOUNDTRACK
            tokens.any { it.contains("greatest hits") || it.contains("best of") } -> RELEASE_TYPE_GREATEST_HITS
            tokens.any { it.contains("remix") } -> RELEASE_TYPE_REMIX
            tokens.any { it.contains("bootleg") } -> RELEASE_TYPE_BOOTLEG
            tokens.any { it.contains("box") && it.contains("set") } -> RELEASE_TYPE_BOX_SET
            tokens.any { it.contains("single") } -> RELEASE_TYPE_SINGLE
            else -> RELEASE_TYPE_STUDIO
        }
    }

    fun inferFormatType(title: String, styles: List<String>?, genres: List<String>?): String {
        val lowered = title.lowercase()
        val tokens = (styles.orEmpty() + genres.orEmpty())
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        return when {
            lowered.contains(" ep") || lowered.contains("ep ") || lowered.contains("e.p") -> FORMAT_EP
            tokens.any { it.contains("ep") } -> FORMAT_EP
            else -> FORMAT_LP
        }
    }
}
