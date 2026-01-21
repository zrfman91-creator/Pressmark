package com.zak.pressmark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zak.pressmark.data.local.db.DbSchema

@Entity(tableName = DbSchema.ImportBatch.TABLE)
data class ImportBatchEntity(
    @PrimaryKey
    @ColumnInfo(name = DbSchema.ImportBatch.ID)
    val id: String,

    @ColumnInfo(name = DbSchema.ImportBatch.CREATED_AT)
    val createdAt: Long,

    @ColumnInfo(name = DbSchema.ImportBatch.MAPPING_JSON)
    val mappingJson: String,

    @ColumnInfo(name = DbSchema.ImportBatch.TOTAL_COUNT)
    val totalCount: Int,

    @ColumnInfo(name = DbSchema.ImportBatch.SUCCESS_COUNT)
    val successCount: Int,

    @ColumnInfo(name = DbSchema.ImportBatch.FAILURE_COUNT)
    val failureCount: Int,
)
