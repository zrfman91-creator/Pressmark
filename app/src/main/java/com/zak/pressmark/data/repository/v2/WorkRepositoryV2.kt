// FILE: app/src/main/java/com/zak/pressmark/data/repository/v2/WorkRepositoryV2.kt
package com.zak.pressmark.data.repository.v2

import androidx.room.withTransaction
import com.zak.pressmark.data.local.dao.v2.PressingDaoV2
import com.zak.pressmark.data.local.dao.v2.ReleaseDaoV2
import com.zak.pressmark.data.local.dao.v2.VariantDaoV2
import com.zak.pressmark.data.local.dao.v2.WorkDaoV2
import com.zak.pressmark.data.local.db.v2.AppDatabaseV2
import com.zak.pressmark.data.local.entity.v2.PressingEntityV2
import com.zak.pressmark.data.local.entity.v2.ReleaseEntityV2
import com.zak.pressmark.data.local.entity.v2.VariantEntityV2
import com.zak.pressmark.data.local.entity.v2.WorkEntityV2
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkRepositoryV2 @Inject constructor(
    private val db: AppDatabaseV2,
    private val workDao: WorkDaoV2,
    private val releaseDao: ReleaseDaoV2,
    private val pressingDao: PressingDaoV2,
    private val variantDao: VariantDaoV2,
) {

    suspend fun createWork(
        title: String,
        artistLine: String,
        year: Int?,
        primaryArtworkUri: String? = null,
    ): String {
        val now = System.currentTimeMillis()
        val workId = "work:${sha1("$title|$artistLine|$year")}"

        db.withTransaction {
            workDao.upsert(
                WorkEntityV2(
                    id = workId,
                    title = title,
                    titleNormalized = normalize(title),
                    artistLine = artistLine,
                    artistNormalized = normalize(artistLine),
                    year = year,
                    genresJson = "[]",
                    stylesJson = "[]",
                    primaryArtworkUri = primaryArtworkUri,
                    discogsMasterId = null,
                    musicBrainzReleaseGroupId = null,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }

        return workId
    }

    suspend fun upsertDiscogsMasterWork(
        discogsMasterId: Long,
        title: String,
        artistLine: String,
        year: Int?,
        primaryArtworkUri: String? = null,
        genres: List<String> = emptyList(),
        styles: List<String> = emptyList(),
    ): String {
        val now = System.currentTimeMillis()

        val existing = workDao.getByDiscogsMasterId(discogsMasterId)
        val workId = existing?.id ?: "work:discogsMaster:$discogsMasterId"

        val entity = WorkEntityV2(
            id = workId,
            title = title,
            titleNormalized = normalize(title),
            artistLine = artistLine,
            artistNormalized = normalize(artistLine),
            year = year,
            genresJson = toJsonArray(genres),
            stylesJson = toJsonArray(styles),
            primaryArtworkUri = primaryArtworkUri,
            discogsMasterId = discogsMasterId,
            musicBrainzReleaseGroupId = existing?.musicBrainzReleaseGroupId,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        db.withTransaction { workDao.upsert(entity) }

        return workId
    }

    suspend fun addRelease(
        workId: String,
        label: String?,
        catalogNo: String?,
        country: String?,
        format: String?,
        year: Int?,
    ): String {
        val id = "release:${sha1("$workId|$label|$catalogNo|$country|$format|$year")}"
        val now = System.currentTimeMillis()

        releaseDao.upsert(
            ReleaseEntityV2(
                id = id,
                workId = workId,
                label = label,
                labelNormalized = normalizeOrNull(label),
                catalogNo = catalogNo,
                catalogNoNormalized = normalizeOrNull(catalogNo),
                country = country,
                format = format,
                releaseYear = year,
                releaseType = null,
                createdAt = now,
                updatedAt = now,
            )
        )

        return id
    }

    suspend fun addPressing(
        releaseId: String,
        barcode: String?,
        label: String?,
        catalogNo: String?,
        country: String?,
        format: String?,
        year: Int?,
    ): String {
        val id = "pressing:${sha1("$releaseId|$barcode|$catalogNo")}"
        val now = System.currentTimeMillis()

        pressingDao.upsert(
            PressingEntityV2(
                id = id,
                releaseId = releaseId,
                barcode = barcode,
                barcodeNormalized = barcode?.filter(Char::isDigit),
                runoutsJson = "[]",
                pressingPlant = null,
                label = label,
                catalogNo = catalogNo,
                country = country,
                format = format,
                releaseYear = year,
                discogsReleaseId = null,
                musicBrainzReleaseId = null,
                createdAt = now,
                updatedAt = now,
            )
        )

        return id
    }

    suspend fun addVariant(
        workId: String,
        pressingId: String,
        variantKey: String = "default",
    ): String {
        val id = "variant:${sha1("$workId|$pressingId|$variantKey")}"

        variantDao.upsert(
            VariantEntityV2(
                id = id,
                workId = workId,
                pressingId = pressingId,
                variantKey = variantKey,
                notes = null,
                rating = null,
                addedAt = System.currentTimeMillis(),
                lastPlayedAt = null,
            )
        )

        return id
    }

    fun observeAllWorks() = workDao.observeAll()

    suspend fun getWork(workId: String) = workDao.getById(workId)

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").trim()

    private fun normalizeOrNull(value: String?): String? =
        value?.let { normalize(it).ifBlank { null } }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun toJsonArray(values: List<String>): String {
        if (values.isEmpty()) return "[]"
        val escaped = values
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { v ->
                val s = v
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                "\"$s\""
            }
        return "[${escaped.joinToString(",")}]"
    }
}
