package com.zak.pressmark.data.repository

object OcrParser {
    fun parse(lines: List<String>): ExtractedFields {
        val title = lines.getOrNull(0)?.takeIf { it.isNotBlank() }
        val artist = lines.getOrNull(1)?.takeIf { it.isNotBlank() }

        var label: String? = null
        var catalogNo: String? = null

        lines.drop(2).forEach { line ->
            val parts = line.split("-", "#").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2 && label == null && catalogNo == null) {
                label = parts.first()
                catalogNo = parts.last()
            }
        }

        return ExtractedFields(
            title = title,
            artist = artist,
            label = label,
            catalogNo = catalogNo,
        )
    }
}
