package com.zak.pressmark.core.util

import java.util.Locale

object Normalizer {

    //Stable key used for dedupe/lookup.
    fun artistKey(input: String): String = input
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")

    //Canonical display casing for storage/UI
    fun artistDisplay(input: String): String {
        val cleaned = input
            .trim()
            .replace(Regex("\\s+"), " ")
        if (cleaned.isBlank()) return ""

        //Only auto-title-case "plain names"
        val isPlainName = cleaned.matches(Regex("^[A-Za-z][A-Za-z' ]*$+"))
        if (!isPlainName) return cleaned

        return cleaned
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" "){ word ->
                val isAllCaps = word.all { it.isLetter() && it.isUpperCase() }
                val hasVowel = word.any { it in "AEIOUaeiou"}
                val keepAllCaps = isAllCaps && !hasVowel && word.length <=5

                when {keepAllCaps -> word else -> word.lowercase(Locale.US)
                    .replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()}
                }
            }
    }
}