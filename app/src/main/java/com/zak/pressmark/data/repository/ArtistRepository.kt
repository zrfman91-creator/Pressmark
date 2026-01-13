package com.zak.pressmark.data.repository

import com.zak.pressmark.data.local.dao.ArtistDao
import com.zak.pressmark.data.local.entity.ArtistEntity
import com.zak.pressmark.data.local.entity.ArtistType
import com.zak.pressmark.data.local.entity.canonicalDisplayName
import com.zak.pressmark.data.local.entity.normalizeArtistName
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ArtistRepository(
    private val artistDao: ArtistDao,
) {

    fun observeAll(): Flow<List<ArtistEntity>> = artistDao.observeAll()

    fun observeById(id: Long): Flow<ArtistEntity?> = artistDao.observeById(id)

    suspend fun findByName(query: String): List<ArtistEntity> {
        val normalized = normalizeArtistName(query)
        if (normalized.isBlank()) return emptyList()
        return artistDao.searchByNormalizedPrefix(normalized, limit = 20)
    }

    suspend fun getOrCreate(displayName: String, artistType: ArtistType): ArtistEntity {
        val canonical = canonicalDisplayName(displayName)
        val normalized = normalizeArtistName(canonical)

        val existing = artistDao.getByNormalized(normalized)
        if (existing != null) return existing

        // *** THE FIX IS HERE ***
        // Create a new ArtistEntity instance correctly.
        val newArtist = ArtistEntity(
            displayName = canonical,
            sortName = canonical, // Or apply specific sort logic if needed
            nameNormalized = normalized,
            artistType = artistType.name
        )
        artistDao.insertOrIgnore(newArtist)

        return artistDao.getByNormalized(normalized)
            ?: error("Failed to create artist: $displayName")
    }

    suspend fun ensureArtist(artist: ArtistEntity): ArtistEntity {
        val existing = artistDao.getByNormalized(artist.nameNormalized)
        if (existing != null) return existing

        artistDao.insertOrIgnore(artist)

        return artistDao.getByNormalized(artist.nameNormalized)
            ?: error("Failed to create artist: ${artist.displayName}")
    }
}
