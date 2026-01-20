// FILE: app/src/main/java/com/zak/pressmark/core/credits/ArtistCreditParser.kt
package com.zak.pressmark.core.credits

import com.zak.pressmark.core.util.Normalizer

/**
 * Pure (non-DB) parser that converts a raw artist string into structured, ordered credits.
 *
 * Output:
 * - Canonical displayName (Normalizer.artistDisplay)
 * - Canonical normalizedKey (Normalizer.artistKey)
 * - Role (core enum; map to data.local.entity.CreditRole in a higher layer)
 * - 1-based position (matches ReleaseArtistCreditEntity default expectation)
 * - Optional displayHint for human phrasing preservation (e.g., "and his orchestra")
 */
object ArtistCreditParser {

    fun parse(raw: String): List<ParsedArtistCredit> {
        val input = raw.normalizeWhitespace()
        if (input.isBlank()) return emptyList()

        // 1) Prefer explicit "X ... feat ... Y" split.
        val (leftAfterFeat, rightAfterFeat) = splitOnFeaturing(input)

        // 2) Handle "X (and|with) (his|her|their) orchestra" first (because "and" is also a general splitter).
        val pronounOrch = splitOffPronounOrchestra(leftAfterFeat)
        if (pronounOrch != null) {
            val primaryNames = splitArtistList(pronounOrch.primaryPart)
            val primaryCredits = primaryNames.map { name ->
                ParsedArtistCredit(
                    displayName = Normalizer.artistDisplay(name),
                    normalizedKey = Normalizer.artistKey(name),
                    role = Role.PRIMARY,
                    displayHint = null
                )
            }

            val orchestraName = inferOrchestraName(primaryNames)
            val orchestraCredits =
                if (orchestraName != null) {
                    listOf(
                        ParsedArtistCredit(
                            displayName = Normalizer.artistDisplay(orchestraName),
                            normalizedKey = Normalizer.artistKey(orchestraName),
                            role = Role.ORCHESTRA,
                            displayHint = pronounOrch.hintPhrase
                        )
                    )
                } else {
                    emptyList()
                }

            val featuredCredits = parseFeatured(rightAfterFeat)

            return orderAndIndex(primaryCredits + orchestraCredits + featuredCredits)
        }

        // 3) General "X with Y" split (only if not the pronoun-orchestra form).
        val withSplit = splitOnWith(leftAfterFeat)

        val primaryPart = withSplit?.left ?: leftAfterFeat
        val withPart = withSplit?.right

        val primaryNames = splitArtistList(primaryPart)
        val primaryCredits = primaryNames.map { name ->
            ParsedArtistCredit(
                displayName = Normalizer.artistDisplay(name),
                normalizedKey = Normalizer.artistKey(name),
                role = Role.PRIMARY,
                displayHint = null
            )
        }

        val withCredits =
            withPart
                ?.let { splitArtistList(it) }
                ?.map { name ->
                    ParsedArtistCredit(
                        displayName = Normalizer.artistDisplay(name),
                        normalizedKey = Normalizer.artistKey(name),
                        role = Role.WITH,
                        displayHint = null
                    )
                }
                .orEmpty()

        val featuredCredits = parseFeatured(rightAfterFeat)

        return orderAndIndex(primaryCredits + withCredits + featuredCredits)
    }

    data class ParsedArtistCredit(
        val displayName: String,
        val normalizedKey: String,
        val role: Role,
        val position: Int = 1,
        val displayHint: String? = null,
    )

    /**
     * Core-layer role enum. Map to data.local.entity.CreditRole in the data layer.
     */
    enum class Role {
        PRIMARY,
        WITH,
        ORCHESTRA,
        ENSEMBLE,
        FEATURED,
        CONDUCTOR,
    }

    // --- Internals ---

    private data class Split2(val left: String, val right: String)

    private data class PronounOrchestraSplit(
        val primaryPart: String,
        val hintPhrase: String,
    )

    private fun parseFeatured(featuredPart: String?): List<ParsedArtistCredit> {
        if (featuredPart.isNullOrBlank()) return emptyList()
        val names = splitArtistList(featuredPart)
        return names.map { name ->
            ParsedArtistCredit(
                displayName = Normalizer.artistDisplay(name),
                normalizedKey = Normalizer.artistKey(name),
                role = Role.FEATURED,
                displayHint = null
            )
        }
    }

    /**
     * Splits on the first featuring marker, if present.
     *
     * Examples:
     * - "X feat. Y" -> ("X", "Y")
     * - "X featuring Y & Z" -> ("X", "Y & Z")
     */
    private fun splitOnFeaturing(input: String): Pair<String, String?> {
        val lower = input.lowercase()
        val markers = listOf(" feat. ", " feat ", " ft. ", " ft ", " featuring ")
        val hit = markers
            .map { m -> lower.indexOf(m) to m }
            .filter { (i, _) -> i >= 0 }
            .minByOrNull { it.first }
            ?: return input to null

        val splitIndex = hit.first
        val marker = hit.second

        val left = input.take(splitIndex).normalizeWhitespace().trimTrimPunct()
        val right = input.substring(splitIndex + marker.length).normalizeWhitespace().trimTrimPunct()

        if (left.isBlank() || right.isBlank()) return input to null
        return left to right
    }

    /**
     * Handles:
     * - "Glenn Miller and his orchestra"
     * - "So-and-so with His Orchestra"
     *
     * Returns primaryPart and a displayHint phrase.
     *
     * NOTE: displayHint is canonicalized to the lowercase phrase ("with his orchestra"),
     * not the input casing, so formatting is stable.
     */
    private fun splitOffPronounOrchestra(input: String): PronounOrchestraSplit? {
        val lower = input.lowercase()

        val patterns = listOf(
            " and his orchestra",
            " and her orchestra",
            " and their orchestra",
            " with his orchestra",
            " with her orchestra",
            " with their orchestra",
        )

        val hit = patterns
            .map { p -> lower.indexOf(p) to p }
            .filter { (i, _) -> i >= 0 }
            .minByOrNull { it.first }
            ?: return null

        val idx = hit.first
        val phrase = hit.second

        val primary = input.take(idx).normalizeWhitespace().trimTrimPunct()
        if (primary.isBlank()) return null

        // Canonical, stable hint phrase (lowercase, no leading space, no trailing punct)
        val hint = phrase
            .trim()
            .trimEnd('.', ',', ';')

        return PronounOrchestraSplit(
            primaryPart = primary,
            hintPhrase = hint
        )
    }

    /**
     * General "X with Y" split:
     * - "Artist with Guest" -> PRIMARY: Artist, WITH: Guest
     *
     * Avoids splitting if "with" is at the start/end or the right side is empty.
     */
    private fun splitOnWith(input: String): Split2? {
        val lower = input.lowercase()
        val needle = " with "
        val idx = lower.indexOf(needle)
        if (idx <= 0) return null

        val left = input.take(idx).normalizeWhitespace().trimTrimPunct()
        val right = input.substring(idx + needle.length).normalizeWhitespace().trimTrimPunct()
        if (left.isBlank() || right.isBlank()) return null

        return Split2(left, right)
    }

    /**
     * Splits a list of artists:
     * - "&" / "and" / "," / ";" / "/" / " x " / "+"
     *
     * Note: Higher-level logic can choose to re-join tokens if needed.
     */
    private fun splitArtistList(input: String): List<String> {
        val cleaned = input
            .normalizeWhitespace()
            .trimTrimPunct()

        if (cleaned.isBlank()) return emptyList()

        val standardized = cleaned
            .replace(";", ",")
            .replace("/", ",")
            .replace(" x ", ",", ignoreCase = true)
            .replace(" & ", ",")
            .replace(" + ", ",")
            .replace(" and ", ",", ignoreCase = true)

        return standardized
            .split(',')
            .map { it.normalizeWhitespace().trimTrimPunct() }
            .filter { it.isNotBlank() }
    }

    /**
     * For "X ... his orchestra" style inputs, infer orchestra name as:
     * - If there is exactly one PRIMARY artist: "<PRIMARY> Orchestra"
     * - Otherwise: null
     */
    private fun inferOrchestraName(primaryArtists: List<String>): String? {
        if (primaryArtists.size != 1) return null
        val primary = primaryArtists.first().normalizeWhitespace().trimTrimPunct()
        if (primary.isBlank()) return null

        val lower = primary.lowercase()
        if (lower.contains("orchestra")) return primary

        return "$primary Orchestra"
    }

    private fun orderAndIndex(list: List<ParsedArtistCredit>): List<ParsedArtistCredit> {
        // Preserve input order; drop duplicates by normalizedKey+role (keep first occurrence).
        val seen = HashSet<String>()
        val deduped = ArrayList<ParsedArtistCredit>(list.size)

        for (c in list) {
            if (c.normalizedKey.isBlank() || c.displayName.isBlank()) continue
            val key = "${c.role}:${c.normalizedKey}"
            if (seen.add(key)) deduped.add(c)
        }

        // 1-based positions (align with ReleaseArtistCreditEntity.position default = 1)
        return deduped.mapIndexed { index, c -> c.copy(position = index + 1) }
    }

    // --- String utilities ---

    private fun String.normalizeWhitespace(): String =
        trim()
            .replace(Regex("\\s+"), " ")

    private fun String.trimTrimPunct(): String {
        var s = normalizeWhitespace().trim()

        while (s.isNotEmpty() && (s.first() == '"' || s.first() == '\'' || s.first() == '(' || s.first() == '[')) {
            s = s.drop(1).trimStart()
        }
        while (
            s.isNotEmpty() && (
                    s.last() == '"' ||
                            s.last() == '\'' ||
                            s.last() == ')' ||
                            s.last() == ']' ||
                            s.last() == '.' ||
                            s.last() == ',' ||
                            s.last() == ';'
                    )
        ) {
            s = s.dropLast(1).trimEnd()
        }

        return s.normalizeWhitespace()
    }
}
