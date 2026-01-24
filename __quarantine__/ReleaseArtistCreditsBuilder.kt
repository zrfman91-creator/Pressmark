// FILE: app/src/main/java/com/zak/pressmark/data/repository/ReleaseArtistCreditsBuilder.kt
package com.zak.pressmark.data.repository.v1

import com.zak.pressmark.core.credits.ArtistCreditParser
import com.zak.pressmark.data.local.entity.v1.CreditRole
import com.zak.pressmark.data.local.entity.v1.ReleaseArtistCreditEntity

/**
 * Data-layer builder that:
 * - parses a raw artist string into structured credits (core, pure)
 * - resolves/creates canonical ArtistEntity rows via ArtistRepository
 * - returns ordered ReleaseArtistCreditEntity rows ready to persist
 *
 * No Room transaction here; callers should wrap persistence in their own transaction (e.g., ReleaseRepository.upsertRelease).
 */
class ReleaseArtistCreditsBuilder(
    private val artistRepository: ArtistRepository
) {

    /**
     * @param releaseId Release id to stamp into each credit entity (ReleaseRepository will also normalize this).
     * @param rawArtist Raw artist string (user-entered or remote metadata).
     */
    suspend fun buildForRelease(
        releaseId: String,
        rawArtist: String
    ): List<ReleaseArtistCreditEntity> {
        if (rawArtist.isBlank()) return emptyList()

        val parsed = ArtistCreditParser.parse(rawArtist)
        if (parsed.isEmpty()) return emptyList()

        // Resolve artists in parsed order to preserve credit ordering.
        val credits = ArrayList<ReleaseArtistCreditEntity>(parsed.size)
        for (p in parsed) {
            // Use canonical displayName from parser; repository re-normalizes for safety.
            val artistId = artistRepository.getOrCreateArtistId(p.displayName)

            credits += ReleaseArtistCreditEntity(
                releaseId = releaseId,
                artistId = artistId,
                role = p.role.toDataRole(),
                position = p.position,
                displayHint = p.displayHint
            )
        }

        // Stable ordering (position is 1-based from parser).
        return credits.sortedWith(compareBy<ReleaseArtistCreditEntity> { it.position }.thenBy { it.artistId })
    }

    private fun ArtistCreditParser.Role.toDataRole(): CreditRole =
        when (this) {
            ArtistCreditParser.Role.PRIMARY -> CreditRole.PRIMARY
            ArtistCreditParser.Role.WITH -> CreditRole.WITH
            ArtistCreditParser.Role.ORCHESTRA -> CreditRole.ORCHESTRA
            ArtistCreditParser.Role.ENSEMBLE -> CreditRole.ENSEMBLE
            ArtistCreditParser.Role.FEATURED -> CreditRole.FEATURED
            ArtistCreditParser.Role.CONDUCTOR -> CreditRole.CONDUCTOR
        }
}
