// FILE: app/src/main/java/com/zak/pressmark/data/local/entity/ArtworkEntity.kt
package com.zak.pressmark.data.local.entity.v1

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.v1.DbSchema
import com.zak.pressmark.data.local.db.v1.DbSchema.Artwork

/**
 * Artwork rows allow multiple cover variants per Release (and richer capture: back/label/runout).
 *
 * v1: link artwork to Release only.
 * Later: optionally add itemId (CollectionItem) if you track multiple copies and per-copy photos.
 */
@Entity(
    tableName = Artwork.TABLE,
    indices = [
        Index(value = [Artwork.RELEASE_ID]),
        Index(value = [Artwork.KIND]),
        Index(value = [Artwork.IS_PRIMARY]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ReleaseEntity::class,
            parentColumns = [DbSchema.Release.ID],
            childColumns = [Artwork.RELEASE_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class ArtworkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Artwork.ID)
    val id: Long = 0L,

    @ColumnInfo(name = Artwork.RELEASE_ID)
    val releaseId: String,

    @ColumnInfo(name = Artwork.URI)
    val uri: String,

    @ColumnInfo(name = Artwork.KIND)
    val kind: ArtworkKind = ArtworkKind.COVER_FRONT,

    /**
     * Optional discriminator for "same pressing, different cover" cases.
     * Examples: "alt-1", "red-cover", "promo", etc.
     */
    @ColumnInfo(name = Artwork.VARIANT_KEY)
    val variantKey: String? = null,

    @ColumnInfo(name = Artwork.SOURCE)
    val source: ArtworkSource = ArtworkSource.LOCAL,

    /**
     * If true, this is the image used for the Release list row thumbnail.
     * v1 policy: at most one primary per release; enforce in DAO/repo.
     */
    @ColumnInfo(name = Artwork.IS_PRIMARY)
    val isPrimary: Boolean = false,

    @ColumnInfo(name = Artwork.WIDTH)
    val width: Int? = null,

    @ColumnInfo(name = Artwork.HEIGHT)
    val height: Int? = null,

    @ColumnInfo(name = Artwork.CREATED_AT)
    val createdAt: Long? = null,
)

enum class ArtworkKind {
    COVER_FRONT,
    COVER_BACK,
    LABEL,
    RUNOUT,
    INSERT,
    OTHER,
}

enum class ArtworkSource {
    LOCAL,
    DISCOGS,
    OTHER,
}
