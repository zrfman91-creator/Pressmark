package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema

@Entity(tableName = DbSchema.ProviderSnapshot.TABLE)
data class ProviderSnapshotEntity(
    @PrimaryKey
    @ColumnInfo(name = DbSchema.ProviderSnapshot.ID)
    val id: String,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.INBOX_ITEM_ID)
    val inboxItemId: String,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.PROVIDER)
    val provider: String,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.PROVIDER_ITEM_ID)
    val providerItemId: String,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.TITLE)
    val title: String,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.ARTIST)
    val artist: String,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.LABEL)
    val label: String?,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.CATALOG_NO)
    val catalogNo: String?,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.BARCODE)
    val barcode: String?,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.RAW_JSON)
    val rawJson: String,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.CONFIDENCE)
    val confidence: Int,

    @ColumnInfo(name = DbSchema.ProviderSnapshot.REASONS_JSON)
    val reasonsJson: String,
)
