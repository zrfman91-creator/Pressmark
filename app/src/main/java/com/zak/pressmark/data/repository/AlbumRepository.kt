package com.zak.pressmark.data.repository

import com.zak.pressmark.data.local.dao.AlbumDao
import com.zak.pressmark.data.local.entity.AlbumEntity
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
        val notes: String?,
    )

    fun observeAllWithArtist(): Flow<List<AlbumWithArtistName>> = dao.observeAllWithArtist()

    fun observeByArtistId(artistId: Long): Flow<List<AlbumWithArtistName>> = dao.observeByArtistId(artistId)

    suspend fun getByIdWithArtist(id: String): AlbumWithArtistName? = dao.getByIdWithArtist(id)

    suspend fun addAlbum(
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
        notes: String?,
    ): String {
        val sanitized = sanitizeAlbumInput(
            title = title,
            artistId = artistId,
            releaseYear = releaseYear,
            catalogNo = catalogNo,
            label = label,
            format = format,
            notes = notes,
        )

        val id = UUID.randomUUID().toString()
        dao.insert(
            AlbumEntity(
                id = id,
                title = sanitized.title,
                artistId = sanitized.artistId,
                releaseYear = sanitized.releaseYear,
                catalogNo = sanitized.catalogNo,
                label = sanitized.label,
                format = sanitized.format,
                notes = sanitized.notes,
            )
        )
        return id
    }

    suspend fun updateAlbum(
        id: String,
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
        notes: String?,
    ) {
        val sanitized = sanitizeAlbumInput(
            title = title,
            artistId = artistId,
            releaseYear = releaseYear,
            catalogNo = catalogNo,
            label = label,
            format = format,
            notes = notes,
        )

        dao.update(
            AlbumEntity(
                id = id,
                title = sanitized.title,
                artistId = sanitized.artistId,
                releaseYear = sanitized.releaseYear,
                catalogNo = sanitized.catalogNo,
                label = sanitized.label,
                format = sanitized.format,
                notes = sanitized.notes,
            )
        )
    }

    suspend fun deleteAlbum(id: String) {
        dao.deleteById(id)
    }

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

    private fun sanitizeAlbumInput(
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
        notes: String?,
    ): SanitizedAlbumInput {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Title required." }

        val cleanYear = releaseYear?.let { y ->
            // Basic sanity: allow null, or a plausible year
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
            notes = cleanOptional(notes),
        )
    }
}
