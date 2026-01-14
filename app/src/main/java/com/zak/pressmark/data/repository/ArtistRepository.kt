package com.zak.pressmark.data.repository

import com.zak.pressmark.data.local.dao.ArtistDao
import com.zak.pressmark.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class ArtistRepository(
    private val dao: ArtistDao
) {

    fun observeTopArtists(limit: Int = 20): Flow<List<ArtistEntity>> =
        dao.observeTopArtists(limit)

    fun searchByName(query: String, limit: Int = 20): Flow<List<ArtistEntity>> =
        dao.searchByName(query, limit)

    fun observeById(artistId: Long): Flow<ArtistEntity?> =
        dao.observeById(artistId)

    suspend fun getOrCreateArtistId(
        displayName: String,
        artistType: String = "Unknown"
    ): Long {
        val normalized = normalize(displayName)
        val trimmed = displayName.trim()
        val existing = dao.findByNormalizedName(normalized)
        if (existing != null) {
            // If user entered a “better” name (casing/punctuation/etc), persist it
            if (existing.displayName != trimmed || existing.sortName != trimmed) {
                dao.updateNames(existing.id, trimmed, trimmed)
            }
            return existing.id
        }

        val entity = ArtistEntity(
            displayName = displayName.trim(),
            sortName = displayName.trim(),
            nameNormalized = normalized,
            artistType = artistType,
        )

        // Insert is ABORT; if a race happens, the exception will be thrown.
        // We handle that by re-checking.
        return try {
            dao.insert(entity)
        } catch (_: Throwable) {
            dao.findByNormalizedName(normalized)?.id
                ?: throw IllegalStateException("Failed to create artist record")
        }
    }

    private fun normalize(name: String): String =
        name.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
}
