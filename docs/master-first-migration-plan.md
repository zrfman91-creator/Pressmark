# Master-First Migration Plan (Pressmark)

## Goals
- Replace release-first identity with master-first catalog identity.
- Preserve fast ingest (master art + shared metadata) while allowing progressive pressing refinement.
- Persist evidence and verification events instead of deleting them on commit.
- Keep UI responsive at scale by moving heavy filters/sorts into Room where possible.

## Current Anchor Points (Code Reality)
- Release is the top-level entity and powers catalog list/details today.
- Inbox pipeline deletes evidence on commit.
- Discogs lookups are release-scoped; master IDs are only stored as nullable fields.

## Migration Phases

### Phase 0 — Safety + Scaffolding
- Freeze release-first assumptions behind a compatibility layer so we can incrementally flip screens.
- Add a migration feature flag to switch catalog list/detail source (release vs catalog item).

### Phase 1 — New Identity Layer (Master-First)
- Add **CatalogItem** entity (top-level, master-first record).
- Add **MasterIdentity** entity (provider + master id + shared metadata + artwork).
- Add **CatalogItemPressing** (owned pressing) entity or link table that references Release when confirmed.
- Add **CatalogVariant** entity to group visual variants of a pressing (different covers, hype stickers, colored vinyl).

### Phase 2 — Evidence + Verification Graph
- Add **EvidenceArtifact** entity (raw + normalized barcode/catno/runout/label/photo).
- Add **VerificationEvent** entity (confirm/reject/correct + candidate reference).
- Update inbox commit to **persist evidence and verification** before clearing inbox rows.

### Phase 3 — Ingest Flow Rewire
- Barcode/cover/quick add create **CatalogItem** with master-first identity.
- Candidate selection produces **pressing candidates** but does not auto-lock a release.
- “Confirm pressing” transitions CatalogItem to `RELEASE_CONFIRMED` and links the pressing entity.

### Phase 4 — UI Rewire
- Catalog list shows **CatalogItem** artwork/title/artist (master-first).
- Album details screen shows **Master info** at top, and **Owned Pressings/Variants** section below.
- Add a “Refine pressing” CTA to allow later evidence input and candidate rerank.

### Phase 5 — Decommission Release-First Paths
- Update repositories so Release is no longer the primary list/detail source.
- Remove Release-first shortcuts once CatalogItem-based screens fully replace them.

## Data Model Sketch (Proposed)
- CatalogItem
  - id, masterIdentityId, displayTitle, displayArtistLine, primaryArtworkUri
  - state: MASTER_ONLY | CANDIDATES_PRESENTED | RELEASE_CONFIRMED | RELEASE_CORRECTED
- MasterIdentity
  - provider, masterId, title, artistLine, year, genres, styles, artworkUri, rawJson
- CatalogItemPressing
  - catalogItemId, releaseId (optional until confirmed), evidenceScore
- CatalogVariant
  - catalogItemId, pressingId, variantKey, notes
- EvidenceArtifact
  - catalogItemId, type, rawValue, normalizedValue, source, confidence, photoUri
- VerificationEvent
  - catalogItemId, eventType, provider, providerItemId, previousReleaseId, newReleaseId, reasons

## Migration Strategy
- **Additive first**: add new tables/entities without removing Release.
- **Dual-write**: on ingest, create CatalogItem + MasterIdentity while still creating Release for compatibility.
- **Cutover**: switch UI screens to CatalogItem once parity is reached.
- **Cleanup**: remove Release-first entry points after verification.

## Testing Strategy
- Add targeted repository tests for catalog item creation and evidence persistence.
- Add inbox pipeline tests to confirm evidence is stored and not deleted on commit.
- Run UI smoke tests on catalog list/detail flows after cutover.
