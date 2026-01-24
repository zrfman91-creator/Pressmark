// FILE: app/src/main/java/com/zak/pressmark/data/local/db/AppDatabase.kt
package com.zak.pressmark.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zak.pressmark.data.local.dao.v1.AlbumDao
import com.zak.pressmark.data.local.dao.v1.ArtistDao
import com.zak.pressmark.data.local.dao.v1.ArtworkDao
import com.zak.pressmark.data.local.dao.v1.CatalogItemDao
import com.zak.pressmark.data.local.dao.v1.CatalogItemPressingDao
import com.zak.pressmark.data.local.dao.v1.CatalogVariantDao
import com.zak.pressmark.data.local.dao.v1.EvidenceArtifactDao
import com.zak.pressmark.data.local.dao.v1.GenreDao
import com.zak.pressmark.data.local.dao.v1.ImportBatchDao
import com.zak.pressmark.data.local.dao.v1.InboxItemDao
import com.zak.pressmark.data.local.dao.v1.MasterIdentityDao
import com.zak.pressmark.data.local.dao.v1.ProviderSnapshotDao
import com.zak.pressmark.data.local.dao.v1.ReleaseArtistCreditDao
import com.zak.pressmark.data.local.dao.v1.ReleaseDao
import com.zak.pressmark.data.local.dao.v1.VerificationEventDao
import com.zak.pressmark.data.local.dao.v2.PressingDaoV2
import com.zak.pressmark.data.local.dao.v2.ReleaseDaoV2
import com.zak.pressmark.data.local.dao.v2.VariantDaoV2
import com.zak.pressmark.data.local.dao.v2.WorkDaoV2
import com.zak.pressmark.data.local.entity.v1.AlbumEntity
import com.zak.pressmark.data.local.entity.v1.AlbumGenreCrossRef
import com.zak.pressmark.data.local.entity.v1.ArtistEntity
import com.zak.pressmark.data.local.entity.v1.ArtworkEntity
import com.zak.pressmark.data.local.entity.v1.CatalogItemEntity
import com.zak.pressmark.data.local.entity.v1.CatalogItemPressingEntity
import com.zak.pressmark.data.local.entity.v1.CatalogVariantEntity
import com.zak.pressmark.data.local.entity.v1.EvidenceArtifactEntity
import com.zak.pressmark.data.local.entity.v1.GenreEntity
import com.zak.pressmark.data.local.entity.v1.ImportBatchEntity
import com.zak.pressmark.data.local.entity.v1.InboxItemEntity
import com.zak.pressmark.data.local.entity.v1.MasterIdentityEntity
import com.zak.pressmark.data.local.entity.v1.ProviderSnapshotEntity
import com.zak.pressmark.data.local.entity.v1.ReleaseArtistCreditEntity
import com.zak.pressmark.data.local.entity.v1.ReleaseEntity
import com.zak.pressmark.data.local.entity.v1.VerificationEventEntity
import com.zak.pressmark.data.local.entity.v2.PressingEntityV2
import com.zak.pressmark.data.local.entity.v2.ReleaseEntityV2
import com.zak.pressmark.data.local.entity.v2.VariantEntityV2
import com.zak.pressmark.data.local.entity.v2.WorkEntityV2


@Database(
    entities = [
        // --- Legacy (kept temporarily for bottom-up refactor) ---
        AlbumEntity::class,
        GenreEntity::class,
        AlbumGenreCrossRef::class,

        // --- Canonical entities ---
        ArtistEntity::class,

        // --- New Release-first model ---
        ReleaseEntity::class,
        ReleaseArtistCreditEntity::class,
        ArtworkEntity::class,

        // --- Master-first model ---
        CatalogItemEntity::class,
        MasterIdentityEntity::class,
        CatalogItemPressingEntity::class,
        CatalogVariantEntity::class,
        EvidenceArtifactEntity::class,
        VerificationEventEntity::class,

        // --- Inbox pipeline --- //
        InboxItemEntity::class,
        ProviderSnapshotEntity::class,
        ImportBatchEntity::class,

        // --- V2 canonical model (Work -> Release -> Pressing -> Variant) ---
        WorkEntityV2::class,
        ReleaseEntityV2::class,
        PressingEntityV2::class,
        VariantEntityV2::class,
    ],
    version = 14, // bump version (wipe anyway)
    exportSchema = true,
)
@TypeConverters(InboxTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    // --- Legacy DAOs (do not remove yet) ---
    abstract fun albumDao(): AlbumDao
    abstract fun genreDao(): GenreDao

    // --- Canonical ---
    abstract fun artistDao(): ArtistDao

    // --- New DAOs (added in next steps) ---
    abstract fun releaseDao(): ReleaseDao
    abstract fun releaseArtistCreditDao(): ReleaseArtistCreditDao
    abstract fun artworkDao(): ArtworkDao

    // --- Catalog (master-first) --- //
    abstract fun catalogItemDao(): CatalogItemDao
    abstract fun masterIdentityDao(): MasterIdentityDao
    abstract fun catalogItemPressingDao(): CatalogItemPressingDao
    abstract fun catalogVariantDao(): CatalogVariantDao
    abstract fun evidenceArtifactDao(): EvidenceArtifactDao
    abstract fun verificationEventDao(): VerificationEventDao

    // --- Inbox pipeline --- //
    abstract fun inboxItemDao(): InboxItemDao
    abstract fun providerSnapshotDao(): ProviderSnapshotDao
    abstract fun importBatchDao(): ImportBatchDao

    // --- V2 canonical DAOs ---
    abstract fun workDaoV2(): WorkDaoV2
    abstract fun releaseDaoV2(): ReleaseDaoV2
    abstract fun pressingDaoV2(): PressingDaoV2
    abstract fun variantDaoV2(): VariantDaoV2
}
