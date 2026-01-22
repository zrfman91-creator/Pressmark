package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema
import com.zak.pressmark.data.model.inbox.InboxErrorCode
import com.zak.pressmark.data.model.inbox.InboxSourceType
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.model.inbox.OcrStatus

@Entity(tableName = DbSchema.InboxItem.TABLE)
data class InboxItemEntity(
    @PrimaryKey
    @ColumnInfo(name = DbSchema.InboxItem.ID)
    val id: String,

    @ColumnInfo(name = DbSchema.InboxItem.SOURCE_TYPE)
    val sourceType: InboxSourceType,

    @ColumnInfo(name = DbSchema.InboxItem.CREATED_AT)
    val createdAt: Long,

    @ColumnInfo(name = DbSchema.InboxItem.UPDATED_AT)
    val updatedAt: Long,

    @ColumnInfo(name = DbSchema.InboxItem.BARCODE)
    val barcode: String?,

    @ColumnInfo(name = DbSchema.InboxItem.RAW_TITLE)
    val rawTitle: String?,

    @ColumnInfo(name = DbSchema.InboxItem.RAW_ARTIST)
    val rawArtist: String?,

    @ColumnInfo(name = DbSchema.InboxItem.RAW_ROW_JSON)
    val rawRowJson: String?,

    @ColumnInfo(name = DbSchema.InboxItem.PHOTO_URIS_JSON)
    val photoUris: List<String>,

    @ColumnInfo(name = DbSchema.InboxItem.OCR_STATUS)
    val ocrStatus: OcrStatus,

    @ColumnInfo(name = DbSchema.InboxItem.LOOKUP_STATUS)
    val lookupStatus: LookupStatus,

    @ColumnInfo(name = DbSchema.InboxItem.ERROR_CODE)
    val errorCode: InboxErrorCode,

    @ColumnInfo(name = DbSchema.InboxItem.RETRY_COUNT)
    val retryCount: Int,

    @ColumnInfo(name = DbSchema.InboxItem.NEXT_OCR_AT)
    val nextOcrAt: Long?,

    @ColumnInfo(name = DbSchema.InboxItem.NEXT_LOOKUP_AT)
    val nextLookupAt: Long?,

    @ColumnInfo(name = DbSchema.InboxItem.LAST_TRIED_AT)
    val lastTriedAt: Long?,

    @ColumnInfo(name = DbSchema.InboxItem.EXTRACTED_TITLE)
    val extractedTitle: String?,

    @ColumnInfo(name = DbSchema.InboxItem.EXTRACTED_ARTIST)
    val extractedArtist: String?,

    @ColumnInfo(name = DbSchema.InboxItem.EXTRACTED_LABEL)
    val extractedLabel: String?,

    @ColumnInfo(name = DbSchema.InboxItem.EXTRACTED_CATNO)
    val extractedCatalogNo: String?,

    @ColumnInfo(name = DbSchema.InboxItem.CONFIDENCE)
    val confidenceScore: Int?,

    @ColumnInfo(name = DbSchema.InboxItem.REASONS_JSON)
    val confidenceReasonsJson: String?,

    @ColumnInfo(name = DbSchema.InboxItem.WAS_UNDONE)
    val wasUndone: Boolean,

    @ColumnInfo(name = DbSchema.InboxItem.COMMITTED_PROVIDER_ITEM_ID)
    val committedProviderItemId: String?,

    @ColumnInfo(name = DbSchema.InboxItem.IS_UNKNOWN)
    val isUnknown: Boolean,

    @ColumnInfo(name = DbSchema.InboxItem.DELETED_AT)
    val deletedAt: Long?,

    @ColumnInfo(name = DbSchema.InboxItem.REFERENCE_PHOTO_URI)
    val referencePhotoUri: String?,
)
