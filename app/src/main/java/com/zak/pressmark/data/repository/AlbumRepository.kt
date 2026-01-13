package com.zak.pressmark.data.repository

import androidx.room.Transaction
import com.zak.pressmark.data.local.dao.AlbumDao
import com.zak.pressmark.data.local.dao.ArtistDao
import com.zak.pressmark.data.local.dao.GenreDao
import com.zak.pressmark.data.local.entity.AlbumEntity
import com.zak.pressmark.data.local.entity.AlbumGenreCrossRef
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.data.local.entity.GenreEntity
import com.zak.pressmark.data.local.entity.normalizeArtistName
// Import the new service and data models
import com.zak.pressmark.data.remote.discogs.DiscogsApiService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AlbumRepository(
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val genreDao: GenreDao, // 2. Inject the GenreDao
    private val discogsApi: DiscogsApiService, // 1. Inject the API service


) {
    fun observeAll(): Flow<List<AlbumEntity>> = albumDao.observeAll()

    fun observeById(id: String): Flow<AlbumEntity?> = albumDao.observeById(id)

    suspend fun getById(id: String): AlbumEntity? = albumDao.getById(id)

    fun observeByArtistId(artistId: Long): Flow<List<AlbumEntity>> =
        albumDao.observeByArtistId(artistId)
    // ... (observeAll, observeById, getById, etc. remain the same)

    /**
     * Fetches the latest data for an album from the Discogs API and updates
     * the local database with the new metadata and cover art.
     */
    @Transaction
    suspend fun refreshFromDiscogs(albumId: String) {
        val currentAlbum = albumDao.getById(albumId) ?: error("Album not found.")
        val discogsId = currentAlbum.discogsReleaseId ?: return

        val release = discogsApi.getRelease(discogsId)
        val primaryImage = release.images?.firstOrNull { it.type == "primary" }?.uri

        // Handle genres and styles with the new many-to-many relationship
        val genreNames = (release.genres.orEmpty() + release.styles.orEmpty()).distinct()
        if (genreNames.isNotEmpty()) {
            genreDao.insertOrIgnore(genreNames.map { GenreEntity(name = it) })
            val genreIds = genreDao.getIdsByNames(genreNames)
            albumDao.clearAlbumGenres(albumId)
            albumDao.linkGenresToAlbum(genreIds.map { AlbumGenreCrossRef(albumId, it) })
        }

        val tracklistText = release.tracklist?.joinToString("\n") { track ->
            val duration = track.duration?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
            "${track.position}. ${track.title}${duration}"
        }?.takeIf { it.isNotBlank() }

        val notesText = release.notes?.trim()?.takeIf { it.isNotBlank() }
        val fetchedArtistName = release.artists?.firstOrNull()?.name ?: currentAlbum.artist
        val artistId = resolveArtistId(fetchedArtistName)
        val formatText = release.formats?.firstOrNull()?.name

        // Update the local album with the new, clean data
        albumDao.update(
            currentAlbum.copy(
                title = release.title,
                artist = fetchedArtistName,
                artistId = artistId,
                releaseYear = release.year ?: currentAlbum.releaseYear,
                coverUri = primaryImage ?: currentAlbum.coverUri,
                label = release.labels?.firstOrNull()?.name ?: currentAlbum.label,
                catalogNo = release.labels?.firstOrNull()?.catalogNumber ?: currentAlbum.catalogNo,
                //tracklist = tracklistText ?: currentAlbum.tracklist,
                //notes = notesText ?: currentAlbum.notes,
                masterId = release.masterId ?: currentAlbum.masterId,
                format = formatText ?: currentAlbum.format
                // The genre and styles fields that were here are now correctly removed.
            )
        )
    }


    suspend fun addAlbum(
        title: String,
        artist: String,
        artistId: Long? = null,
        releaseYear: Int? = null,
        catalogNo: String? = null,
        label: String? = null,
        //tracklist: String? = null,
        //notes: String? = null,
    ) {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()

        require(cleanTitle.isNotBlank()) { "Title is required." }
        require(cleanArtist.isNotBlank()) { "Artist is required." }

        val artistId = resolveArtistId(cleanArtist)

        val album = AlbumEntity(
            id = UUID.randomUUID().toString(),
            addedAt = System.currentTimeMillis(), //Set creation time
            title = cleanTitle,
            artist = cleanArtist,
            artistId = artistId,
            releaseYear = releaseYear,
            catalogNo = catalogNo?.trim().takeIf { !it.isNullOrBlank() },
            label = label?.trim().takeIf { !it.isNullOrBlank() },
            //tracklist = tracklist?.trim().takeIf { !it.isNullOrBlank() },
            //notes = notes?.trim().takeIf { !it.isNullOrBlank() },
        )

        albumDao.insert(album)
    }

    suspend fun updateAlbum(
        albumId: String,
        title: String,
        artist: String,
        releaseYear: Int?,
        catalogNo: String?,
        label: String?,
        //tracklist: () -> Unit,
        //notes: () -> Unit,
    ) {
        val current = albumDao.getById(albumId) ?: error("Album not found.")

        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()

        require(cleanTitle.isNotBlank()) { "Title is required." }
        require(cleanArtist.isNotBlank()) { "Artist is required." }

        val artistId = resolveArtistId(cleanArtist)

        albumDao.update(
            current.copy(
                title = cleanTitle,
                artist = cleanArtist,
                artistId = artistId,
                releaseYear = releaseYear,
                catalogNo = catalogNo?.trim().takeIf { !it.isNullOrBlank() },
                label = label?.trim().takeIf { !it.isNullOrBlank() },
                //tracklist = tracklist?.trim().takeIf { !it.isNullOrBlank() },
                //notes = notes?.trim().takeIf { !it.isNullOrBlank() },
                addedAt = current.addedAt // Preserve the original creation date
            )
        )
    }
    suspend fun deleteAlbum(album: AlbumEntity) {
        albumDao.delete(album)
    }

    suspend fun setLocalCover(
        albumId: String,
        coverUri: String,
    ) {
        albumDao.updateCover(
            id = albumId,
            coverUri = coverUri,
            discogsReleaseId = null,
        )
    }

    suspend fun setDiscogsCover(
        albumId: String,
        coverUrl: String,
        discogsReleaseId: Long,
    ) {
        albumDao.updateCover(
            id = albumId,
            coverUri = coverUrl,
            discogsReleaseId = discogsReleaseId,
        )
    }

    suspend fun clearDiscogsCover(albumId: String) {
        albumDao.updateCover(
            id = albumId,
            coverUri = null,
            discogsReleaseId = null,
        )
    }

    /**
     * Back-compat / alpha repair:
     * If an old album row has no artistId, link it to a master Artist row.
     */
    suspend fun ensureArtistMasterLink(album: AlbumEntity) {
        if (album.artistId != null) return

        val raw = album.artist.trim()
        if (raw.isBlank()) return

        val artistId = resolveArtistId(raw)

        albumDao.update(album.copy(artistId = artistId))
    }

    private suspend fun resolveArtistId(displayName: String): Long {
        val raw = displayName.trim()
        val strict = normalizeArtistName(raw)
        val legacy = raw.lowercase() // migration v4->v5 used LOWER(TRIM()) (no collapsing)

        // Prefer strict; fall back to legacy to match existing backfilled rows.
        val existing =
            artistDao.getIdByNormalized(strict)
                ?: artistDao.getIdByNormalized(legacy)

        if (existing != null) return existing

        // Insert (unique on name_normalized will dedupe under contention).
        artistDao.insertOrIgnore(
            ArtistEntity(
                displayName = raw,
                sortName = raw,
                nameNormalized = strict,
            )
        )

        return artistDao.getIdByNormalized(strict)
            ?: artistDao.getIdByNormalized(legacy)
            ?: error("Failed to resolve artist id for '$displayName'")
    }
}
