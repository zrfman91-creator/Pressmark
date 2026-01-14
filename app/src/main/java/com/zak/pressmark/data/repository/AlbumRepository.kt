package com.zak.pressmark.data.repository

import com.zak.pressmark.data.local.dao.AlbumDao
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AlbumRepository(
    private val dao: AlbumDao
)
{
    private data class SanitizedAlbumInput(
        val title: String,
        val artistId: Long?,
        val releaseYear: Int?,
        val catalogNo: String?,
        val label: String?,
        val format: String?,
    )

    private fun sanitizeAndValidate(
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ): SanitizedAlbumInput {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Title is required" }

        // Keep artist optional, but normalize “0”/negative to null (defensive)
        val cleanArtistId = artistId?.takeIf { it > 0L }

        // Optional sanity range: loose and future-proof
        val cleanYear = releaseYear?.let { y ->
            require(y in 1877..2100) { "Year looks invalid" }
            y
        }

        val cleanCatalog = catalogNo?.trim()?.takeIf { it.isNotBlank() }
        val cleanLabel = label?.trim()?.takeIf { it.isNotBlank() }
        val cleanFormat = format?.trim()?.takeIf { it.isNotBlank() }

        return SanitizedAlbumInput(
            title = cleanTitle,
            artistId = cleanArtistId,
            releaseYear = cleanYear,
            catalogNo = cleanCatalog,
            label = cleanLabel,
            format = cleanFormat,
        )
    }

    // Canonical reads for UI
    fun observeAllWithArtistName(): Flow<List<AlbumWithArtistName>> =
        dao.observeAllWithArtist()

    fun observeByIdWithArtistName(id: String): Flow<AlbumWithArtistName?> =
        dao.observeByIdWithArtist(id)

    fun observeByArtistIdWithArtistName(artistId: Long): Flow<List<AlbumWithArtistName>> =
        dao.observeByArtistIdWithArtist(artistId)

    // Legacy reads (keep for now)
    fun observeAll(): Flow<List<AlbumEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<AlbumEntity?> = dao.observeById(id)

    suspend fun getById(id: String): AlbumEntity? = dao.getById(id)

    // Writes (canonical: artistId, not artist string)
    suspend fun addAlbum(
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ) {
        val s = sanitizeAndValidate(title, artistId, releaseYear, catalogNo, label, format)

        dao.insert(
            AlbumEntity(
                id = UUID.randomUUID().toString(),
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

    suspend fun refreshFromDiscogs(albumId: String) {
        // TODO: implement later
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

}
