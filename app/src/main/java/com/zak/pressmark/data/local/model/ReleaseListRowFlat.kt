// FILE: app/src/main/java/com/zak/pressmark/data/local/model/ReleaseListRowFlat.kt
package com.zak.pressmark.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.zak.pressmark.data.local.entity.v1.CreditRole
import com.zak.pressmark.data.local.entity.v1.ReleaseEntity

/**
 * Flat row for the Release list read model.
 *
 * One Release may produce multiple rows (one per credit) due to the LEFT JOIN on credits.
 * Group by [release.id] in a higher layer to build a single UI row per release.
 */
data class ReleaseListRowFlat(
    @Embedded
    val release: ReleaseEntity,

    // Artwork: primary if present, otherwise latest (via correlated subquery in DAO)
    @ColumnInfo(name = "artwork_id")
    val artworkId: Long?,

    @ColumnInfo(name = "artwork_uri")
    val artworkUri: String?,

    // Credit (nullable if release has no credits yet)
    @ColumnInfo(name = "credit_artist_id")
    val creditArtistId: Long?,

    @ColumnInfo(name = "credit_role")
    val creditRole: CreditRole?,

    @ColumnInfo(name = "credit_position")
    val creditPosition: Int?,

    @ColumnInfo(name = "credit_display_hint")
    val creditDisplayHint: String?,

    // Joined artist display name (nullable if no credits)
    @ColumnInfo(name = "artist_display_name")
    val artistDisplayName: String?,
)
