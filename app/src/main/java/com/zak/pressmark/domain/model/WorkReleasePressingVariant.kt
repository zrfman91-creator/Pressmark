// FILE: app/src/main/java/com/zak/pressmark/domain/model/WorkReleasePressingVariant.kt
package com.zak.pressmark.domain.model

/**
 * Domain Model Contract (V2)
 *
 * Goal: a single, unambiguous vocabulary for the entire app:
 *   Work -> Release -> Pressing -> Variant
 *
 * Definitions (canonical):
 * - Work: the musical work people refer to (album as a concept). Roughly:
 *     Discogs "Master" / MusicBrainz "Release Group".
 *
 * - Release: a marketed/configured edition of a Work (label + catno + country + format + year).
 *     This is a "release package" that may have multiple pressings.
 *
 * - Pressing: a specific manufactured instance/run of a Release.
 *     This is the object most external providers return as their "release" (e.g., Discogs Release).
 *
 * - Variant: the user-owned copy variant / copy-specific notes / condition / user metadata.
 *
 * IMPORTANT:
 * - This file is the SOURCE OF TRUTH for naming. New features should model their data using these
 *   types, even while we are still migrating persistence/UI from legacy tables.
 * - Room entities may remain String IDs for now; these inline IDs wrap String to keep code explicit.
 *
 * Suggested mapping from current schema (transitional, until legacy tables are removed):
 * - MasterIdentityEntity            -> External Work identity snapshot (provider-scoped)
 * - CatalogItemEntity               -> Work (library row)
 * - ReleaseEntity (releases table)  -> Pressing (provider "release"-like record)
 * - CatalogItemPressingEntity       -> WorkPressing link (Work <-> Pressing)
 * - CatalogVariantEntity            -> Variant (user copy tied to a pressing)
 */
object DomainV2

@JvmInline value class WorkId(val value: String)
@JvmInline value class ReleaseId(val value: String)
@JvmInline value class PressingId(val value: String)
@JvmInline value class VariantId(val value: String)
@JvmInline value class ArtworkId(val value: String)

/**
 * Provider namespace for external IDs and snapshots.
 * Add to this enum rather than using raw strings in new code.
 */
enum class ProviderKey {
    DISCOGS,
    MUSICBRAINZ,
    OTHER,
}

/**
 * The "kind" of thing a provider ID refers to.
 * Example: Discogs masterId -> WORK, Discogs releaseId -> PRESSING.
 */
enum class ExternalEntityKind {
    WORK,
    RELEASE,
    PRESSING,
    VARIANT,
    ARTIST,
    LABEL,
}

/**
 * Stable reference to an external provider entity (id string is provider-native).
 */
data class ExternalRef(
    val provider: ProviderKey,
    val kind: ExternalEntityKind,
    val id: String,
)

/**
 * Lightweight credit line for a Work (and optionally for Releases/Pressings later).
 * Keeping it simple for now: a pre-formatted display line plus optional structured parts.
 */
data class ArtistCreditLine(
    val display: String,
    val primaryNames: List<String> = emptyList(),
)

/**
 * Artwork associated with a Work or Pressing (or both).
 * Keep as URIs and lightweight metadata; persist details in Room entities.
 */
enum class ArtworkKind {
    COVER_FRONT,
    COVER_BACK,
    LABEL,
    RUNOUT,
    OTHER,
}

data class ArtworkRef(
    val id: ArtworkId,
    val uri: String,
    val kind: ArtworkKind = ArtworkKind.COVER_FRONT,
    val isPrimary: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
)

/**
 * WORK
 * - Primary list item in the user's library.
 * - Stable anchor for search/sort/filter.
 */
data class Work(
    val id: WorkId,
    val title: String,
    val artist: ArtistCreditLine,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
    val primaryArtworkUri: String? = null,
    val externalRefs: List<ExternalRef> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * RELEASE
 * - Groups pressings for the same marketed/configured edition.
 * - Example: "US 1977, Sire, LP, CatNo SRK 6058".
 *
 * NOTE: Some providers do not explicitly expose this as a distinct entity. That's OK.
 * We can synthesize Release rows when importing/committing by grouping on key fields.
 */
data class Release(
    val id: ReleaseId,
    val workId: WorkId,
    val label: String? = null,
    val catalogNo: String? = null,
    val country: String? = null,
    val format: String? = null,         // e.g. "LP", "12IN", "7IN", "CD"
    val releaseYear: Int? = null,
    val releaseType: String? = null,    // e.g. "STUDIO", "LIVE", "COMPILATION"
    val externalRefs: List<ExternalRef> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * PRESSING
 * - Provider "release" objects usually map here (Discogs Release, MB Release).
 * - Carries pressing-specific identifiers: barcode, runouts, plant, etc.
 */
data class Pressing(
    val id: PressingId,
    val releaseId: ReleaseId,
    val barcode: String? = null,
    val runouts: List<String> = emptyList(),
    val pressingPlant: String? = null,
    val country: String? = null,        // sometimes differs from Release; keep optional duplication
    val catalogNo: String? = null,      // sometimes differs from Release; keep optional duplication
    val label: String? = null,          // sometimes differs from Release; keep optional duplication
    val format: String? = null,
    val releaseYear: Int? = null,
    val artwork: List<ArtworkRef> = emptyList(),
    val externalRefs: List<ExternalRef> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * VARIANT
 * - The user's owned copy and copy-specific metadata.
 * - This is what "I own this pressing" ultimately means.
 */
data class Variant(
    val id: VariantId,
    val workId: WorkId,
    val pressingId: PressingId,
    val variantKey: String,             // stable key, e.g. "default", "red-vinyl", "signed"
    val notes: String? = null,
    val rating: Int? = null,
    val addedAt: Long,
    val lastPlayedAt: Long? = null,
)

/**
 * Convenience container used by UI and repositories.
 * Prefer passing this around rather than mixing legacy Release/Album/Catalog types.
 */
data class WorkGraph(
    val work: Work,
    val releases: List<Release> = emptyList(),
    val pressings: List<Pressing> = emptyList(),
    val variants: List<Variant> = emptyList(),
)
