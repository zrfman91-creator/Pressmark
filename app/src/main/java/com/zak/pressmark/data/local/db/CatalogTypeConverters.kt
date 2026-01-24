package com.zak.pressmark.data.local.db

import androidx.room.TypeConverter
import com.zak.pressmark.data.local.entity.CatalogItemState
import com.zak.pressmark.data.local.entity.EvidenceSource
import com.zak.pressmark.data.local.entity.EvidenceType
import com.zak.pressmark.data.local.entity.Provider
import com.zak.pressmark.data.local.entity.VerificationEventType

/**
 * Persist enums as stable strings.
 *
 * IMPORTANT: keep enum names stable once released; changing names requires migration.
 */
class CatalogTypeConverters {

    @TypeConverter fun providerToString(v: Provider?): String? = v?.name
    @TypeConverter fun stringToProvider(v: String?): Provider? = v?.let { Provider.valueOf(it) }

    @TypeConverter fun catalogStateToString(v: CatalogItemState?): String? = v?.name
    @TypeConverter fun stringToCatalogState(v: String?): CatalogItemState? = v?.let { CatalogItemState.valueOf(it) }

    @TypeConverter fun evidenceTypeToString(v: EvidenceType?): String? = v?.name
    @TypeConverter fun stringToEvidenceType(v: String?): EvidenceType? = v?.let { EvidenceType.valueOf(it) }

    @TypeConverter fun evidenceSourceToString(v: EvidenceSource?): String? = v?.name
    @TypeConverter fun stringToEvidenceSource(v: String?): EvidenceSource? = v?.let { EvidenceSource.valueOf(it) }

    @TypeConverter fun verificationEventTypeToString(v: VerificationEventType?): String? = v?.name
    @TypeConverter fun stringToVerificationEventType(v: String?): VerificationEventType? = v?.let { VerificationEventType.valueOf(it) }
}
