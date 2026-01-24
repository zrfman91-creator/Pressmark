// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/ReleaseArtistCreditEntity.kt
package com.zak.pressmark.data.local.entity.v1

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema
import com.zak.pressmark.data.local.db.DbSchema.Artist
import com.zak.pressmark.data.local.db.DbSchema.ReleaseArtistCredit

/**
 * Join table between Release and Artist, with roles and ordering.
 *
 * This replaces the single Album.artist_id FK and enables:
 * - multiple primary artists (duets/splits)
 * - orchestras/ensembles that remain searchable but not browse-listed
 * - "with" and "feat." credits
 */
@Entity(
    tableName = ReleaseArtistCredit.TABLE,
    indices = [
        Index(value = [ReleaseArtistCredit.RELEASE_ID]),
        Index(value = [ReleaseArtistCredit.ARTIST_ID]),
        Index(value = [ReleaseArtistCredit.ROLE]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ReleaseEntity::class,
            parentColumns = [DbSchema.Release.ID],
            childColumns = [ReleaseArtistCredit.RELEASE_ID],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = [Artist.ID],
            childColumns = [ReleaseArtistCredit.ARTIST_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class ReleaseArtistCreditEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ReleaseArtistCredit.ID)
    val id: Long = 0L,

    @ColumnInfo(name = ReleaseArtistCredit.RELEASE_ID)
    val releaseId: String,

    @ColumnInfo(name = ReleaseArtistCredit.ARTIST_ID)
    val artistId: Long,

    @ColumnInfo(name = ReleaseArtistCredit.ROLE)
    val role: CreditRole = CreditRole.PRIMARY,

    @ColumnInfo(name = ReleaseArtistCredit.POSITION)
    val position: Int = 1,

    /**
     * Optional hint to preserve exact human phrasing for display.
     * Example: "and his orchestra" / "and her orchestra" / "and the orchestra"
     */
    @ColumnInfo(name = ReleaseArtistCredit.DISPLAY_HINT)
    val displayHint: String? = null,
)

/**
 * Minimal, extensible credit roles.
 *
 * v1 policy (browse/search):
 * - PRIMARY is used for "main" artists and drives the A–Z artist browse list.
 * - ORCHESTRA / ENSEMBLE / WITH / FEATURED are searchable and can show under an
 *   "Appears on" tab, but do not clutter A–Z by default.
 */
enum class CreditRole {
    PRIMARY,
    WITH,
    ORCHESTRA,
    ENSEMBLE,
    FEATURED,
    CONDUCTOR,
}
