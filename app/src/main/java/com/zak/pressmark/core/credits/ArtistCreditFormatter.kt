// FILE: app/src/main/java/com/zak/pressmark/core/credits/ArtistCreditFormatter.kt
package com.zak.pressmark.core.credits

/**
 * Pure formatter for turning structured credits into a single display line.
 *
 * Intended usage:
 * - UI read models query structured credits + artist display names
 * - Map them into [Credit] and call [formatSingleLine]
 *
 * Formatting rules (deterministic):
 * - Base = PRIMARY artists joined with " & " (or fallback to first non-primary if no PRIMARY exists)
 * - ORCHESTRA/ENSEMBLE:
 *    - If any such credit has displayHint -> append " (<displayHint>)" and DO NOT list orchestra names
 *    - Else, if exactly one PRIMARY and exactly one orchestra/ensemble derived from it -> append " (and his orchestra)"
 *    - Else -> append " (with <orchestra names joined by &>)"
 * - WITH -> append " with <names joined by &>"
 * - FEATURED -> append " feat. <names joined by &>"
 */
object ArtistCreditFormatter {

    data class Credit(
        val displayName: String,
        val role: Role,
        val position: Int = 1,
        val displayHint: String? = null,
    )

    enum class Role {
        PRIMARY,
        WITH,
        ORCHESTRA,
        ENSEMBLE,
        FEATURED,
        CONDUCTOR,
    }

    fun formatSingleLine(credits: List<Credit>): String {
        if (credits.isEmpty()) return ""

        val ordered = credits
            .filter { it.displayName.isNotBlank() }
            .sortedWith(compareBy<Credit> { it.position }.thenBy { it.role.ordinal }.thenBy { it.displayName })

        val primary = ordered.filter { it.role == Role.PRIMARY }
        val with = ordered.filter { it.role == Role.WITH }
        val featured = ordered.filter { it.role == Role.FEATURED }
        val orchestral = ordered.filter { it.role == Role.ORCHESTRA || it.role == Role.ENSEMBLE }
        // (Reserved) conductor not currently rendered; keep role for future expansion.
        // val conductor = ordered.filter { it.role == Role.CONDUCTOR }

        val baseNames = when {
            primary.isNotEmpty() -> primary.map { it.displayName }
            else -> listOf(ordered.first().displayName)
        }

        val base = baseNames.distinctPreserveOrder().joinToString(" & ")

        val sb = StringBuilder(base)

        // Orchestra / Ensemble rendering
        if (orchestral.isNotEmpty()) {
            val hinted = orchestral.firstNotNullOfOrNull { it.displayHint?.cleanHint() }
            when {
                hinted != null -> {
                    sb.append(" (").append(hinted).append(")")
                }

                shouldUseDefaultPronounOrchestra(primary, orchestral) -> {
                    sb.append(" (and his orchestra)")
                }

                else -> {
                    val orchNames = orchestral.map { it.displayName }.distinctPreserveOrder()
                    sb.append(" (with ").append(orchNames.joinToString(" & ")).append(")")
                }
            }
        }

        // WITH
        if (with.isNotEmpty()) {
            val withNames = with.map { it.displayName }.distinctPreserveOrder()
            sb.append(" with ").append(withNames.joinToString(" & "))
        }

        // FEATURED
        if (featured.isNotEmpty()) {
            val featNames = featured.map { it.displayName }.distinctPreserveOrder()
            sb.append(" feat. ").append(featNames.joinToString(" & "))
        }

        return sb.toString().trim()
    }

    private fun shouldUseDefaultPronounOrchestra(
        primary: List<Credit>,
        orchestral: List<Credit>
    ): Boolean {
        if (primary.size != 1) return false
        if (orchestral.size != 1) return false

        val p = primary.first().displayName.trim()
        val o = orchestral.first().displayName.trim()
        if (p.isBlank() || o.isBlank()) return false

        // If orchestra looks like a derived "<Primary> Orchestra", use the pronoun form.
        val lowerO = o.lowercase()
        val lowerP = p.lowercase()
        val looksLikeDerived =
            lowerO == "${lowerP} orchestra" ||
                    (lowerO.contains("orchestra") && lowerO.startsWith(lowerP))

        return looksLikeDerived
    }

    private fun String.cleanHint(): String {
        // Parser stores hint phrases like "and his orchestra" (already trimmed).
        // Keep it readable and remove trailing punctuation.
        return this.trim()
            .trimEnd('.', ',', ';')
            .replace(Regex("\\s+"), " ")
    }

    private fun <T> List<T>.distinctPreserveOrder(): List<T> {
        val seen = LinkedHashSet<T>()
        for (item in this) seen.add(item)
        return seen.toList()
    }
}
