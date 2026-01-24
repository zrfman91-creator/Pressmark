// FILE: app/src/main/java/com/zak/pressmark/data/local/model/ReleaseListItem.kt
package com.zak.pressmark.data.local.model

import com.zak.pressmark.core.credits.ArtistCreditFormatter
import com.zak.pressmark.data.local.entity.v1.ReleaseArtistCreditEntity
import com.zak.pressmark.data.local.entity.v1.ReleaseEntity

/**
 * UI-ready list item built from [ReleaseListRowFlat] rows.
 */
data class ReleaseListItem(
    val release: ReleaseEntity,
    val artworkId: Long?,
    val artworkUri: String?,
    val artistLine: String,
)

object ReleaseListItemMapper {

    /**
     * Group flat rows (1 row per credit) into one list item per release.
     * Assumes rows are ordered by Release addedAt DESC and credit position ASC (as in DAO query).
     */
    fun fromFlatRows(rows: List<ReleaseListRowFlat>): List<ReleaseListItem> {
        if (rows.isEmpty()) return emptyList()

        val result = ArrayList<ReleaseListItem>()
        var i = 0

        while (i < rows.size) {
            val first = rows[i]
            val releaseId = first.release.id

            val groupRows = ArrayList<ReleaseListRowFlat>()
            while (i < rows.size && rows[i].release.id == releaseId) {
                groupRows.add(rows[i])
                i++
            }

            val creditsForFormatter = groupRows
                .filter { it.creditArtistId != null && it.creditRole != null && it.creditPosition != null && !it.artistDisplayName.isNullOrBlank() }
                .map { r ->
                    // Reconstruct a minimal ReleaseArtistCreditEntity for mapping consistency.
                    val creditEntity = ReleaseArtistCreditEntity(
                        id = 0L, // not needed for formatting
                        releaseId = r.release.id,
                        artistId = r.creditArtistId!!,
                        role = r.creditRole!!,
                        position = r.creditPosition!!,
                        displayHint = r.creditDisplayHint
                    )
                    ArtistCreditFormatMapper.toFormatterCredit(
                        credit = creditEntity,
                        artistDisplayName = r.artistDisplayName!!
                    )
                }

            val artistLine = ArtistCreditFormatter.formatSingleLine(creditsForFormatter)

            result += ReleaseListItem(
                release = first.release,
                artworkId = first.artworkId,
                artworkUri = first.artworkUri,
                artistLine = artistLine
            )
        }

        return result
    }
}
