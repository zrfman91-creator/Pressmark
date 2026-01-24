package com.zak.pressmark.core.util

data class CsvParseResult(
    val headers: List<String>,
    val rows: List<List<String>>,
)

object CsvParser {
    fun parse(text: String): CsvParseResult {
        val lines = text.split(Regex("\r?\n"))
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) return CsvParseResult(emptyList(), emptyList())

        val headers = parseLine(lines.first())
        val rows = lines.drop(1).map { parseLine(it) }

        return CsvParseResult(headers, rows)
    }

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i += 1
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
            i += 1
        }
        result.add(current.toString())
        return result.map { it.trim() }
    }
}
