// FILE: app/src/main/java/com/zak/pressmark/data/repository/v2/WorkRepositoryV2.kt
package com.zak.pressmark.data.repository.v2

import androidx.room.withTransaction
import com.zak.pressmark.data.local.dao.v2.PressingDaoV2
import com.zak.pressmark.data.local.dao.v2.ReleaseDaoV2
import com.zak.pressmark.data.local.dao.v2.VariantDaoV2
import com.zak.pressmark.data.local.dao.v2.WorkGenreStyleDaoV2
import com.zak.pressmark.data.local.dao.v2.WorkDaoV2
import com.zak.pressmark.data.local.db.v2.AppDatabaseV2
import com.zak.pressmark.data.local.db.v2.DbSchemaV2
import com.zak.pressmark.data.local.dao.v2.NamedHeading
import com.zak.pressmark.data.local.entity.v2.PressingEntityV2
import com.zak.pressmark.data.local.entity.v2.ReleaseEntityV2
import com.zak.pressmark.data.local.entity.v2.VariantEntityV2
import com.zak.pressmark.data.local.entity.v2.WorkEntityV2
import com.zak.pressmark.data.local.entity.v2.GenreEntityV2
import com.zak.pressmark.data.local.entity.v2.StyleEntityV2
import com.zak.pressmark.data.local.entity.v2.WorkGenreCrossRefEntityV2
import com.zak.pressmark.data.local.entity.v2.WorkStyleCrossRefEntityV2
import com.zak.pressmark.data.prefs.LibrarySortKey
import com.zak.pressmark.data.prefs.LibrarySortSpec
import com.zak.pressmark.data.prefs.SortDirection
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val workGenreStyleDao: WorkGenreStyleDaoV2,
) {

    sealed class UpsertResult {
        data class Created(val workId: String) : UpsertResult()
        data class UpdatedExisting(val workId: String) : UpsertResult()
        data class PossibleDuplicate(val existingWorkId: String?, val reason: String) : UpsertResult()
    }

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
                    titleSort = normalizeForSort(title, stripLeadingThe = true),
                    artistLine = artistLine,
                    artistNormalized = normalize(artistLine),
                    artistSort = normalizeForSort(artistLine, stripLeadingThe = true),
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
    ): UpsertResult {
        val now = System.currentTimeMillis()

        val existing = workDao.getByDiscogsMasterId(discogsMasterId)
        val workId = existing?.id ?: "work:discogsMaster:$discogsMasterId"

        val entity = WorkEntityV2(
            id = workId,
            title = title,
            titleNormalized = normalize(title),
            titleSort = normalizeForSort(title, stripLeadingThe = true),
            artistLine = artistLine,
            artistNormalized = normalize(artistLine),
            artistSort = normalizeForSort(artistLine, stripLeadingThe = true),
            year = year,
            genresJson = toJsonArray(genres),
            stylesJson = toJsonArray(styles),
            primaryArtworkUri = primaryArtworkUri,
            discogsMasterId = discogsMasterId,
            musicBrainzReleaseGroupId = existing?.musicBrainzReleaseGroupId,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        db.withTransaction {
            workDao.upsert(entity)
            updateWorkGenresAndStyles(workId = workId, genres = genres, styles = styles)
        }

        return if (existing == null) {
            UpsertResult.Created(workId)
        } else {
            UpsertResult.UpdatedExisting(workId)
        }
    }

    suspend fun upsertManualWork(
        title: String,
        artistLine: String,
        year: Int?,
        primaryArtworkUri: String? = null,
    ): UpsertResult {
        val now = System.currentTimeMillis()
        val titleNormalized = normalize(title)
        val artistNormalized = normalize(artistLine)

        val existingExact = workDao.getByNormalized(
            artistNorm = artistNormalized,
            titleNorm = titleNormalized,
            year = year,
        )

        if (existingExact != null) {
            val updated = existingExact.copy(
                title = title,
                titleNormalized = titleNormalized,
                titleSort = normalizeForSort(title, stripLeadingThe = true),
                artistLine = artistLine,
                artistNormalized = artistNormalized,
                artistSort = normalizeForSort(artistLine, stripLeadingThe = true),
                year = year,
                primaryArtworkUri = primaryArtworkUri ?: existingExact.primaryArtworkUri,
                updatedAt = now,
            )

            db.withTransaction { workDao.upsert(updated) }

            return UpsertResult.UpdatedExisting(existingExact.id)
        }

        val possibleDuplicate = if (year == null) {
            workDao.getByNormalizedIgnoringYear(
                artistNorm = artistNormalized,
                titleNorm = titleNormalized,
            )
        } else {
            null
        }

        val workId = "work:${sha1("$titleNormalized|$artistNormalized|$year")}"
        val entity = WorkEntityV2(
            id = workId,
            title = title,
            titleNormalized = titleNormalized,
            titleSort = normalizeForSort(title, stripLeadingThe = true),
            artistLine = artistLine,
            artistNormalized = artistNormalized,
            artistSort = normalizeForSort(artistLine, stripLeadingThe = true),
            year = year,
            genresJson = "[]",
            stylesJson = "[]",
            primaryArtworkUri = primaryArtworkUri,
            discogsMasterId = null,
            musicBrainzReleaseGroupId = null,
            createdAt = now,
            updatedAt = now,
        )

        db.withTransaction { workDao.upsert(entity) }

        return if (possibleDuplicate != null) {
            UpsertResult.PossibleDuplicate(
                existingWorkId = possibleDuplicate.id,
                reason = "Similar work already exists in your library.",
            )
        } else {
            UpsertResult.Created(workId)
        }
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

    fun observeWork(workId: String) = workDao.observeById(workId)

    fun observeAllWorksSorted(sortSpec: LibrarySortSpec) = when (sortSpec.key) {
        LibrarySortKey.TITLE -> if (sortSpec.direction == SortDirection.ASC) {
            workDao.observeAllByTitle()
        } else {
            workDao.observeAllByTitleDesc()
        }
        LibrarySortKey.ARTIST -> if (sortSpec.direction == SortDirection.ASC) {
            workDao.observeAllByArtist()
        } else {
            workDao.observeAllByArtistDesc()
        }
        LibrarySortKey.RECENTLY_ADDED -> workDao.observeAllByCreatedDesc()
        LibrarySortKey.YEAR -> if (sortSpec.direction == SortDirection.ASC) {
            workDao.observeAllByYearAsc()
        } else {
            workDao.observeAllByYearDesc()
        }
    }

    fun observeArtistHeadings(): Flow<List<String>> =
        workDao.observeArtistHeadings().map { headings -> headings.map { it.label } }

    fun observeYearHeadings(sortSpec: LibrarySortSpec): Flow<List<Int?>> =
        if (sortSpec.key == LibrarySortKey.YEAR && sortSpec.direction == SortDirection.DESC) {
            workDao.observeYearHeadingsDesc().map { headings -> headings.map { it.year } }
        } else {
            workDao.observeYearHeadingsAsc().map { headings -> headings.map { it.year } }
        }

    fun observeDecadeHeadings(sortSpec: LibrarySortSpec): Flow<List<Int?>> =
        if (sortSpec.key == LibrarySortKey.YEAR && sortSpec.direction == SortDirection.DESC) {
            workDao.observeDecadeHeadingsDesc().map { headings -> headings.map { it.decade } }
        } else {
            workDao.observeDecadeHeadingsAsc().map { headings -> headings.map { it.decade } }
        }

    fun observeGenreHeadings(): Flow<List<NamedHeading>> = workDao.observeGenreHeadings()

    fun observeStyleHeadings(): Flow<List<NamedHeading>> = workDao.observeStyleHeadings()

    fun observeArtistHeadingsForYear(year: Int?): Flow<List<String>> =
        workDao.observeArtistHeadingsForYear(year).map { headings -> headings.map { it.label } }

    fun observeArtistHeadingsForUnknownYear(): Flow<List<String>> =
        workDao.observeArtistHeadingsForUnknownYear().map { headings -> headings.map { it.label } }

    fun observeArtistHeadingsForDecade(decade: Int?): Flow<List<String>> =
        if (decade == null) {
            workDao.observeArtistHeadingsForUnknownYear().map { headings -> headings.map { it.label } }
        } else {
            workDao.observeArtistHeadingsForDecade(decade, decade + 9).map { headings -> headings.map { it.label } }
        }

    fun observeArtistHeadingsForGenre(normalized: String): Flow<List<String>> =
        workDao.observeArtistHeadingsForGenre(normalized).map { headings -> headings.map { it.label } }

    fun observeArtistHeadingsForUnknownGenre(): Flow<List<String>> =
        workDao.observeArtistHeadingsForUnknownGenre().map { headings -> headings.map { it.label } }

    fun observeArtistHeadingsForStyle(normalized: String): Flow<List<String>> =
        workDao.observeArtistHeadingsForStyle(normalized).map { headings -> headings.map { it.label } }

    fun observeArtistHeadingsForUnknownStyle(): Flow<List<String>> =
        workDao.observeArtistHeadingsForUnknownStyle().map { headings -> headings.map { it.label } }

    fun observeWorksForArtist(artistLine: String, sortSpec: LibrarySortSpec): Flow<List<WorkEntityV2>> {
        val query = buildWorksQuery(
            whereClause = "${DbSchemaV2.Work.ARTIST_LINE} = ?",
            args = arrayOf<Any?>(artistLine),
            sortSpec = sortSpec,
        )
        return workDao.observeWorksByQuery(query)
    }

    fun observeWorksForYearAndArtist(year: Int?, artistLine: String, sortSpec: LibrarySortSpec): Flow<List<WorkEntityV2>> {
        val query = buildWorksQuery(
            whereClause = "(${DbSchemaV2.Work.YEAR} = ? OR (${DbSchemaV2.Work.YEAR} IS NULL AND ? IS NULL)) AND ${DbSchemaV2.Work.ARTIST_LINE} = ?",
            args = arrayOf<Any?>(year, year, artistLine),
            sortSpec = sortSpec,
        )
        return workDao.observeWorksByQuery(query)
    }

    fun observeWorksForDecadeAndArtist(decadeStart: Int?, artistLine: String, sortSpec: LibrarySortSpec): Flow<List<WorkEntityV2>> {
        val (whereClause, args) = if (decadeStart == null) {
            "${DbSchemaV2.Work.YEAR} IS NULL AND ${DbSchemaV2.Work.ARTIST_LINE} = ?" to arrayOf<Any?>(artistLine)
        } else {
            "${DbSchemaV2.Work.YEAR} BETWEEN ? AND ? AND ${DbSchemaV2.Work.ARTIST_LINE} = ?" to arrayOf<Any?>(
                decadeStart,
                decadeStart + 9,
                artistLine,
            )
        }
        return workDao.observeWorksByQuery(buildWorksQuery(whereClause, args, sortSpec))
    }

    fun observeWorksForGenreAndArtist(genreNormalized: String?, artistLine: String, sortSpec: LibrarySortSpec): Flow<List<WorkEntityV2>> {
        val (whereClause, args) = if (genreNormalized == null) {
            "NOT EXISTS (SELECT 1 FROM ${DbSchemaV2.WorkGenre.TABLE} WHERE ${DbSchemaV2.WorkGenre.TABLE}.${DbSchemaV2.WorkGenre.WORK_ID} = ${DbSchemaV2.Work.TABLE}.${DbSchemaV2.Work.ID}) AND ${DbSchemaV2.Work.ARTIST_LINE} = ?" to arrayOf<Any?>(artistLine)
        } else {
            """
            EXISTS (
              SELECT 1 FROM ${DbSchemaV2.WorkGenre.TABLE}
              INNER JOIN ${DbSchemaV2.Genre.TABLE}
                ON ${DbSchemaV2.Genre.TABLE}.${DbSchemaV2.Genre.ID} = ${DbSchemaV2.WorkGenre.TABLE}.${DbSchemaV2.WorkGenre.GENRE_ID}
              WHERE ${DbSchemaV2.WorkGenre.TABLE}.${DbSchemaV2.WorkGenre.WORK_ID} = ${DbSchemaV2.Work.TABLE}.${DbSchemaV2.Work.ID}
                AND ${DbSchemaV2.Genre.TABLE}.${DbSchemaV2.Genre.NAME_NORMALIZED} = ?
            ) AND ${DbSchemaV2.Work.ARTIST_LINE} = ?
            """.trimIndent() to arrayOf<Any?>(genreNormalized, artistLine)
        }
        return workDao.observeWorksByQuery(buildWorksQuery(whereClause, args, sortSpec))
    }

    fun observeWorksForStyleAndArtist(styleNormalized: String?, artistLine: String, sortSpec: LibrarySortSpec): Flow<List<WorkEntityV2>> {
        val (whereClause, args) = if (styleNormalized == null) {
            "NOT EXISTS (SELECT 1 FROM ${DbSchemaV2.WorkStyle.TABLE} WHERE ${DbSchemaV2.WorkStyle.TABLE}.${DbSchemaV2.WorkStyle.WORK_ID} = ${DbSchemaV2.Work.TABLE}.${DbSchemaV2.Work.ID}) AND ${DbSchemaV2.Work.ARTIST_LINE} = ?" to arrayOf<Any?>(artistLine)
        } else {
            """
            EXISTS (
              SELECT 1 FROM ${DbSchemaV2.WorkStyle.TABLE}
              INNER JOIN ${DbSchemaV2.Style.TABLE}
                ON ${DbSchemaV2.Style.TABLE}.${DbSchemaV2.Style.ID} = ${DbSchemaV2.WorkStyle.TABLE}.${DbSchemaV2.WorkStyle.STYLE_ID}
              WHERE ${DbSchemaV2.WorkStyle.TABLE}.${DbSchemaV2.WorkStyle.WORK_ID} = ${DbSchemaV2.Work.TABLE}.${DbSchemaV2.Work.ID}
                AND ${DbSchemaV2.Style.TABLE}.${DbSchemaV2.Style.NAME_NORMALIZED} = ?
            ) AND ${DbSchemaV2.Work.ARTIST_LINE} = ?
            """.trimIndent() to arrayOf<Any?>(styleNormalized, artistLine)
        }
        return workDao.observeWorksByQuery(buildWorksQuery(whereClause, args, sortSpec))
    }

    suspend fun getWork(workId: String) = workDao.getById(workId)

    suspend fun deleteWork(workId: String) {
        db.withTransaction {
            variantDao.deleteByWorkId(workId)
            val releases = releaseDao.getByWorkId(workId)
            releases.forEach { release ->
                pressingDao.deleteByReleaseId(release.id)
            }
            releaseDao.deleteByWorkId(workId)
            workDao.deleteById(workId)
        }
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").trim()

    private fun normalizeOrNull(value: String?): String? =
        value?.let { normalize(it).ifBlank { null } }

    private fun normalizeForSort(value: String, stripLeadingThe: Boolean): String {
        val trimmed = value.trim().lowercase().replace(Regex("\\s+"), " ")
        if (!stripLeadingThe) return trimmed
        return trimmed.removePrefix("the ").trimStart()
    }

    private fun normalizeName(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")

    private suspend fun updateWorkGenresAndStyles(
        workId: String,
        genres: List<String>,
        styles: List<String>,
    ) {
        workGenreStyleDao.deleteWorkGenres(workId)
        workGenreStyleDao.deleteWorkStyles(workId)

        val genreIds = genres
            .mapNotNull { raw ->
                val normalized = normalizeName(raw)
                if (normalized.isBlank()) return@mapNotNull null
                val display = raw.trim().ifBlank { normalized }
                getOrCreateGenreId(normalized, display)
            }
            .distinct()

        if (genreIds.isNotEmpty()) {
            val entries = genreIds.map { id ->
                WorkGenreCrossRefEntityV2(workId = workId, genreId = id)
            }
            workGenreStyleDao.insertWorkGenres(entries)
        }

        val styleIds = styles
            .mapNotNull { raw ->
                val normalized = normalizeName(raw)
                if (normalized.isBlank()) return@mapNotNull null
                val display = raw.trim().ifBlank { normalized }
                getOrCreateStyleId(normalized, display)
            }
            .distinct()

        if (styleIds.isNotEmpty()) {
            val entries = styleIds.map { id ->
                WorkStyleCrossRefEntityV2(workId = workId, styleId = id)
            }
            workGenreStyleDao.insertWorkStyles(entries)
        }
    }

    private suspend fun getOrCreateGenreId(normalized: String, display: String): Long {
        val existing = workGenreStyleDao.getGenreIdByNormalized(normalized)
        if (existing != null) return existing
        val inserted = workGenreStyleDao.insertGenre(
            GenreEntityV2(nameNormalized = normalized, nameDisplay = display)
        )
        return if (inserted != -1L) inserted else workGenreStyleDao.getGenreIdByNormalized(normalized) ?: 0L
    }

    private suspend fun getOrCreateStyleId(normalized: String, display: String): Long {
        val existing = workGenreStyleDao.getStyleIdByNormalized(normalized)
        if (existing != null) return existing
        val inserted = workGenreStyleDao.insertStyle(
            StyleEntityV2(nameNormalized = normalized, nameDisplay = display)
        )
        return if (inserted != -1L) inserted else workGenreStyleDao.getStyleIdByNormalized(normalized) ?: 0L
    }

    private fun buildWorksQuery(
        whereClause: String,
        args: Array<Any?>,
        sortSpec: LibrarySortSpec,
    ): SimpleSQLiteQuery {
        val orderBy = when (sortSpec.key) {
            LibrarySortKey.ARTIST -> "${DbSchemaV2.Work.TITLE_SORT} ASC, ${DbSchemaV2.Work.ID} ASC"
            LibrarySortKey.TITLE -> {
                val direction = if (sortSpec.direction == SortDirection.ASC) "ASC" else "DESC"
                "${DbSchemaV2.Work.TITLE_SORT} $direction, ${DbSchemaV2.Work.ID} ASC"
            }
            LibrarySortKey.YEAR -> {
                val direction = if (sortSpec.direction == SortDirection.ASC) "ASC" else "DESC"
                val nullOrdering = if (sortSpec.direction == SortDirection.ASC) {
                    "${DbSchemaV2.Work.YEAR} IS NULL"
                } else {
                    "${DbSchemaV2.Work.YEAR} IS NULL DESC"
                }
                "$nullOrdering, ${DbSchemaV2.Work.YEAR} $direction, ${DbSchemaV2.Work.TITLE_SORT} ASC, ${DbSchemaV2.Work.ID} ASC"
            }
            LibrarySortKey.RECENTLY_ADDED -> {
                val direction = if (sortSpec.direction == SortDirection.ASC) "ASC" else "DESC"
                "${DbSchemaV2.Work.CREATED_AT} $direction, ${DbSchemaV2.Work.TITLE_SORT} ASC, ${DbSchemaV2.Work.ID} ASC"
            }
        }
        return SimpleSQLiteQuery(
            "SELECT * FROM ${DbSchemaV2.Work.TABLE} WHERE $whereClause ORDER BY $orderBy",
            args,
        )
    }

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
