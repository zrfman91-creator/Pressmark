package com.zak.pressmark.data.repository

import com.zak.pressmark.data.local.dao.AlbumDao
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AlbumRepository(
    private val dao: AlbumDao
) {

    // -----------------------------
    // Canonical reads for UI
    // -----------------------------
    fun observeAllWithArtistName(): Flow<List<AlbumWithArtistName>> =
        dao.observeAllWithArtist()

    fun observeByIdWithArtistName(id: String): Flow<AlbumWithArtistName?> =
        dao.observeByIdWithArtist(id)

    fun observeByArtistIdWithArtistName(artistId: Long): Flow<List<AlbumWithArtistName>> =
        dao.observeByArtistIdWithArtist(artistId)

    // -----------------------------
    // Legacy reads (keep for now)
    // -----------------------------
    fun observeAll(): Flow<List<AlbumEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<AlbumEntity?> = dao.observeById(id)

    suspend fun getById(id: String): AlbumEntity? = dao.getById(id)

    // -----------------------------
    // Writes (canonical: artistId, not artist string)
    // -----------------------------
    suspend fun addAlbum(
        title: String,
        artistId: Long?,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        format: String?,
    ) {
        dao.insert(
            AlbumEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                artistId = artistId,
                releaseYear = releaseYear,
                catalogNo = catalogNo,
                label = label,
                format = format?.trim()?.takeIf { it.isNotBlank() },
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

        dao.update(
            existing.copy(
                title = title,
                artistId = artistId,
                releaseYear = releaseYear,
                catalogNo = catalogNo,
                label = label,
                format = format?.trim()?.takeIf { it.isNotBlank() },
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
        val normalized = coverUri?.trim()?.takeIf { it.isNotBlank() }
        dao.updateCover(
            id = albumId,
            coverUri = normalized,
            discogsReleaseId = null,
        )
    }

    suspend fun setDiscogsCover(
        albumId: String,
        coverUrl: String?,
        discogsReleaseId: Long?,
    ) {
        val normalizedCover = coverUrl?.trim()?.takeIf { it.isNotBlank() }
        dao.updateCover(
            id = albumId,
            coverUri = normalizedCover,
            discogsReleaseId = discogsReleaseId,
        )
    }

    suspend fun clearDiscogsCover(albumId: String) {
        dao.updateCover(
            id = albumId,
            coverUri = null,
            discogsReleaseId = null,
        )
    }

    suspend fun refreshFromDiscogs(albumId: String) {
        // TODO: implement later
    }
}
