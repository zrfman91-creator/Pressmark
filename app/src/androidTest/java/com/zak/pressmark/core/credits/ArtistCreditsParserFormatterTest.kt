// FILE: app/src/test/java/com/zak/pressmark/core/credits/ArtistCreditsParserFormatterTest.kt
package com.zak.pressmark.core.credits

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistCreditsParserFormatterTest {

    @Test
    fun glennMillerAndHisOrchestra_parsesPrimaryPlusOrchestraHint() {
        val parsed = ArtistCreditParser.parse("Glenn Miller and his orchestra")

        assertEquals(2, parsed.size)

        assertEquals(ArtistCreditParser.Role.PRIMARY, parsed[0].role)
        assertEquals("Glenn Miller", parsed[0].displayName)

        assertEquals(ArtistCreditParser.Role.ORCHESTRA, parsed[1].role)
        assertEquals("Glenn Miller Orchestra", parsed[1].displayName) // derived name
        assertEquals("and his orchestra", parsed[1].displayHint)

        val formatted = ArtistCreditFormatter.formatSingleLine(
            parsed.map {
                ArtistCreditFormatter.Credit(
                    displayName = it.displayName,
                    role = it.role.toFormatterRole(),
                    position = it.position,
                    displayHint = it.displayHint
                )
            }
        )
        assertEquals("Glenn Miller (and his orchestra)", formatted)
    }

    @Test
    fun ellaAndLouis_parsesTwoPrimaries() {
        val parsed = ArtistCreditParser.parse("Ella Fitzgerald & Louis Armstrong")
        assertEquals(2, parsed.size)

        assertEquals(ArtistCreditParser.Role.PRIMARY, parsed[0].role)
        assertEquals("Ella Fitzgerald", parsed[0].displayName)

        assertEquals(ArtistCreditParser.Role.PRIMARY, parsed[1].role)
        assertEquals("Louis Armstrong", parsed[1].displayName)

        val formatted = ArtistCreditFormatter.formatSingleLine(
            parsed.map {
                ArtistCreditFormatter.Credit(
                    displayName = it.displayName,
                    role = it.role.toFormatterRole(),
                    position = it.position,
                    displayHint = it.displayHint
                )
            }
        )
        assertEquals("Ella Fitzgerald & Louis Armstrong", formatted)
    }

    @Test
    fun xFeatY_parsesFeatured() {
        val parsed = ArtistCreditParser.parse("X feat. Y")
        assertEquals(2, parsed.size)

        assertEquals(ArtistCreditParser.Role.PRIMARY, parsed[0].role)
        assertEquals("X", parsed[0].displayName)

        assertEquals(ArtistCreditParser.Role.FEATURED, parsed[1].role)
        assertEquals("Y", parsed[1].displayName)

        val formatted = ArtistCreditFormatter.formatSingleLine(
            parsed.map {
                ArtistCreditFormatter.Credit(
                    displayName = it.displayName,
                    role = it.role.toFormatterRole(),
                    position = it.position,
                    displayHint = it.displayHint
                )
            }
        )
        assertEquals("X feat. Y", formatted)
    }

    @Test
    fun soAndSoWithHisOrchestra_parsesWithOrchestraHint() {
        val parsed = ArtistCreditParser.parse("So-and-so with His Orchestra")
        assertEquals(2, parsed.size)

        assertEquals(ArtistCreditParser.Role.PRIMARY, parsed[0].role)
        assertEquals("So-and-so", parsed[0].displayName)

        assertEquals(ArtistCreditParser.Role.ORCHESTRA, parsed[1].role)
        assertEquals("So-and-so Orchestra", parsed[1].displayName)
        assertEquals("with his orchestra", parsed[1].displayHint)

        val formatted = ArtistCreditFormatter.formatSingleLine(
            parsed.map {
                ArtistCreditFormatter.Credit(
                    displayName = it.displayName,
                    role = it.role.toFormatterRole(),
                    position = it.position,
                    displayHint = it.displayHint
                )
            }
        )
        assertEquals("So-and-so (with his orchestra)", formatted)
    }

    private fun ArtistCreditParser.Role.toFormatterRole(): ArtistCreditFormatter.Role =
        when (this) {
            ArtistCreditParser.Role.PRIMARY -> ArtistCreditFormatter.Role.PRIMARY
            ArtistCreditParser.Role.WITH -> ArtistCreditFormatter.Role.WITH
            ArtistCreditParser.Role.ORCHESTRA -> ArtistCreditFormatter.Role.ORCHESTRA
            ArtistCreditParser.Role.ENSEMBLE -> ArtistCreditFormatter.Role.ENSEMBLE
            ArtistCreditParser.Role.FEATURED -> ArtistCreditFormatter.Role.FEATURED
            ArtistCreditParser.Role.CONDUCTOR -> ArtistCreditFormatter.Role.CONDUCTOR
        }
}
