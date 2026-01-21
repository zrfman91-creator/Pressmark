package com.zak.pressmark.data.local.db

import androidx.room.TypeConverter
import com.zak.pressmark.data.model.inbox.InboxErrorCode
import com.zak.pressmark.data.model.inbox.InboxSourceType
import com.zak.pressmark.data.model.inbox.LookupStatus
import com.zak.pressmark.data.model.inbox.OcrStatus
import org.json.JSONArray

class InboxTypeConverters {
    @TypeConverter
    fun toInboxSourceType(value: String?): InboxSourceType? =
        value?.let { InboxSourceType.valueOf(it) }

    @TypeConverter
    fun fromInboxSourceType(value: InboxSourceType?): String? = value?.name

    @TypeConverter
    fun toOcrStatus(value: String?): OcrStatus? = value?.let { OcrStatus.valueOf(it) }

    @TypeConverter
    fun fromOcrStatus(value: OcrStatus?): String? = value?.name

    @TypeConverter
    fun toLookupStatus(value: String?): LookupStatus? = value?.let { LookupStatus.valueOf(it) }

    @TypeConverter
    fun fromLookupStatus(value: LookupStatus?): String? = value?.name

    @TypeConverter
    fun toInboxErrorCode(value: String?): InboxErrorCode? = value?.let { InboxErrorCode.valueOf(it) }

    @TypeConverter
    fun fromInboxErrorCode(value: InboxErrorCode?): String? = value?.name

    @TypeConverter
    fun toPhotoUris(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        val array = JSONArray(json)
        return List(array.length()) { index -> array.getString(index) }
    }

    @TypeConverter
    fun fromPhotoUris(uris: List<String>?): String? {
        if (uris.isNullOrEmpty()) return null
        val array = JSONArray()
        uris.forEach { array.put(it) }
        return array.toString()
    }
}
