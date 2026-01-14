// File: app/src/main/java/com/zak/pressmark/data/remote/discogs/DiscogsArtworkRepository.kt
package com.zak.pressmark.data.remote.discogs

import com.zak.pressmark.data.local.entity.AlbumEntity


//Step: return BOTH Discogs release id + cover URL.

data class DiscogsArtwork(
    val releaseId: Long,
    val coverUrl: String,
)

interface DiscogsArtworkRepository {

    suspend fun getArtwork(album: AlbumEntity): DiscogsArtwork?

    suspend fun getCoverUrl(album: AlbumEntity): String? = getArtwork(album)?.coverUrl
}

class DefaultDiscogsArtworkRepository(
    private val api: DiscogsArtworkApi
) : DiscogsArtworkRepository {

    override suspend fun getArtwork(album: AlbumEntity): DiscogsArtwork? {
        val title = album.title.trim()
        if (title.isBlank()) return null

        val result = api.findCover(
            title = title,
            artist = null,
            year = album.releaseYear,
            catno = album.catalogNo?.trim().takeIf { !it.isNullOrBlank() },
            label = album.label?.trim().takeIf { !it.isNullOrBlank() },
        ) ?: return null

        return DiscogsArtwork(
            releaseId = result.releaseId,
            coverUrl = result.coverUrl,
        )
    }
}
