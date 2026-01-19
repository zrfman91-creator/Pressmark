// file: app/src/main/java/com/zak/pressmark/data/local/model/ArtistCreditFormatMapper.kt
package com.zak.pressmark.data.local.model

import com.zak.pressmark.core.credits.ArtistCreditFormatter
import com.zak.pressmark.data.local.entity.CreditRole
import com.zak.pressmark.data.local.entity.ReleaseArtistCreditEntity

/**
 * Data -> Core mapping helpers for credit display formatting.
 *
 * Keeps Room entities out of UI/presentation and standardizes role mapping.
 */
object ArtistCreditFormatMapper {

    /**
     * Map one persisted credit + its resolved artist displayName into a formatter Credit.
     *
     * @param artistDisplayName The ArtistEntity.displayName (already canonical casing).
     */
    fun toFormatterCredit(
        credit: ReleaseArtistCreditEntity,
        artistDisplayName: String
    ): ArtistCreditFormatter.Credit {
        return ArtistCreditFormatter.Credit(
            displayName = artistDisplayName,
            role = credit.role.toFormatterRole(),
            position = credit.position,
            displayHint = credit.displayHint
        )
    }

    /**
     * Convenience for mapping a list of (credit, artistDisplayName) pairs into formatter credits.
     * Caller should supply pairs already ordered by credit.position.
     */
    fun toFormatterCredits(
        creditsWithNames: List<Pair<ReleaseArtistCreditEntity, String>>
    ): List<ArtistCreditFormatter.Credit> {
        return creditsWithNames.map { (credit, name) ->
            toFormatterCredit(credit = credit, artistDisplayName = name)
        }
    }

    private fun CreditRole.toFormatterRole(): ArtistCreditFormatter.Role =
        when (this) {
            CreditRole.PRIMARY -> ArtistCreditFormatter.Role.PRIMARY
            CreditRole.WITH -> ArtistCreditFormatter.Role.WITH
            CreditRole.ORCHESTRA -> ArtistCreditFormatter.Role.ORCHESTRA
            CreditRole.ENSEMBLE -> ArtistCreditFormatter.Role.ENSEMBLE
            CreditRole.FEATURED -> ArtistCreditFormatter.Role.FEATURED
            CreditRole.CONDUCTOR -> ArtistCreditFormatter.Role.CONDUCTOR
        }
}
