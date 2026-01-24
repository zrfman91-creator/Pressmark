// FILE: app/src/main/java/com/zak/pressmark/data/repository/AlbumRepository.kt
package com.zak.pressmark.data.repository

import com.zak.pressmark.data.local.dao.v1.AlbumDao
import com.zak.pressmark.data.local.entity.v1.AlbumEntity
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AlbumRepository(
    private val dao: AlbumDao
) {
    private data class SanitizedAlbumInput(
        val title: String,
        val artistId: Long?,
        val releaseYear: Int?,
        val catalogNo: String?,
        val label: String?,
        val format: String?,
    )

    // ✅ Canonical (artist display name comes from Artist table)
    fun observeAllWithArtistName(): Flow<List<AlbumWithArtistName>> =
        dao.observeAllWithArtist()

    fun observeByIdWithArtistName(albumId: String): Flow<AlbumWithArtistName?> =
        dao.observeByIdWithArtist(albumId)

    fun observeByArtistIdWithArtistName(artistId: Long): Flow<List<AlbumWithArtistName>> =
        dao.observeByArtistIdWithArtist(artistId)

    // Legacy (optional)
    fun observeAll(): Flow<List<AlbumEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<AlbumEntity?> = dao.observeById(id)

    suspend fun getById(id: String): AlbumEntity? = dao.getById(id)

    suspend fun addAlbum(
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ) {
        // Kept for API compatibility with existing call sites.
        addAlbumReturningId(
            title = title,
            artistId = artistId,
            releaseYear = releaseYear,
            catalogNo = catalogNo,
            label = label,
            format = format,
        )
    }

    /**
     * Creates a new album and returns its generated albumId.
     *
     * Use this for flows that need to immediately navigate using the saved albumId
     * (e.g., Save → Cover Picker → Details).
     */
    suspend fun addAlbumReturningId(
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ): String {
        val s = sanitizeAndValidate(title, artistId, releaseYear, catalogNo, label, format)

        val id = UUID.randomUUID().toString()
        dao.insert(
            AlbumEntity(
                id = id,
                title = s.title,
                artistId = s.artistId,
                releaseYear = s.releaseYear,
                catalogNo = s.catalogNo,
                label = s.label,
                format = s.format,
                coverUri = null,
                discogsReleaseId = null,
                addedAt = System.currentTimeMillis(),
            )
        )
        return id
    }

    suspend fun updateAlbum(
        albumId: String,
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ) {
        val existing = dao.getById(albumId)
            ?: throw IllegalStateException("Album not found")

        val s = sanitizeAndValidate(title, artistId, releaseYear, catalogNo, label, format)

        dao.update(
            existing.copy(
                title = s.title,
                artistId = s.artistId,
                releaseYear = s.releaseYear,
                catalogNo = s.catalogNo,
                label = s.label,
                format = s.format,
            )
        )
    }

    suspend fun backfillArtworkProviderFromLegacyDiscogs(): Int {
        return dao.backfillArtworkProviderFromLegacyDiscogs()
    }

    suspend fun deleteAlbum(album: AlbumEntity) {
        dao.delete(album)
    }

    // -----------------------------
    // Covers
    // -----------------------------

    suspend fun setLocalCover(albumId: String, coverUri: String?) {
        setArtworkSelection(
            albumId = albumId,
            coverUrl = coverUri,
            provider = null,
            providerItemId = null,
        )
    }

    suspend fun setDiscogsCover(
        albumId: String,
        coverUrl: String?,
        discogsReleaseId: Long?,
    ) {
        setArtworkSelection(
            albumId = albumId,
            coverUrl = coverUrl,
            provider = "discogs",
            providerItemId = discogsReleaseId?.toString(),
        )
    }

    suspend fun clearDiscogsCover(albumId: String) {
        setArtworkSelection(
            albumId = albumId,
            coverUrl = null,
            provider = null,
            providerItemId = null,
        )
    }

    // ✅ Canonical artwork setter (used by picker dialog)
    suspend fun setArtworkSelection(
        albumId: String,
        coverUrl: String?,
        provider: String?,
        providerItemId: String?,
    ) {
        val normalizedCover = coverUrl?.trim()?.takeIf { it.isNotBlank() }
        val normalizedProvider = provider?.trim()?.takeIf { it.isNotBlank() }
        val normalizedItemId = providerItemId?.trim()?.takeIf { it.isNotBlank() }

        // Keep legacy discogsReleaseId populated when provider == "discogs"
        val discogsReleaseId: Long? =
            if (normalizedProvider == "discogs") normalizedItemId?.toLongOrNull() else null

        dao.updateCover(
            id = albumId,
            coverUri = normalizedCover,
            discogsReleaseId = discogsReleaseId,
            artworkProvider = normalizedProvider,
            artworkProviderItemId = normalizedItemId,
        )
    }

    suspend fun fillMissingFields(
        albumId: String,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ): Int {
        return dao.fillMissingFields(
            id = albumId,
            releaseYear = releaseYear,
            catalogNo = catalogNo,
            label = label,
            format = format,
        )
    }

    private fun sanitizeAndValidate(
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ): SanitizedAlbumInput {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Title required." }

        val cleanYear = releaseYear?.let { y ->
            require(y in 1800..2100) { "Year looks invalid." }
            y
        }

        fun cleanOptional(s: String?): String? = s?.trim()?.takeIf { it.isNotBlank() }

        return SanitizedAlbumInput(
            title = cleanTitle,
            artistId = artistId,
            releaseYear = cleanYear,
            catalogNo = cleanOptional(catalogNo),
            label = cleanOptional(label),
            format = cleanOptional(format),
        )
    }
}
