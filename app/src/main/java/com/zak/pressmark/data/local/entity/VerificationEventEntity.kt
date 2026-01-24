package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema.VerificationEvent

@Entity(
    tableName = VerificationEvent.TABLE,
    indices = [
        Index(value = [VerificationEvent.CATALOG_ITEM_ID]),
        Index(value = [VerificationEvent.PROVIDER]),
        Index(value = [VerificationEvent.PROVIDER_ITEM_ID]),
    ],
)
data class VerificationEventEntity(
    @PrimaryKey
    @ColumnInfo(name = VerificationEvent.ID) val id: String,
    @ColumnInfo(name = VerificationEvent.CATALOG_ITEM_ID) val catalogItemId: String,
    @ColumnInfo(name = VerificationEvent.EVENT_TYPE) val eventType: String,
    @ColumnInfo(name = VerificationEvent.PROVIDER) val provider: String?,
    @ColumnInfo(name = VerificationEvent.PROVIDER_ITEM_ID) val providerItemId: String?,
    @ColumnInfo(name = VerificationEvent.PREVIOUS_RELEASE_ID) val previousReleaseId: String?,
    @ColumnInfo(name = VerificationEvent.NEW_RELEASE_ID) val newReleaseId: String?,
    @ColumnInfo(name = VerificationEvent.REASONS_JSON) val reasonsJson: String?,
    @ColumnInfo(name = VerificationEvent.CREATED_AT) val createdAt: Long,
)
