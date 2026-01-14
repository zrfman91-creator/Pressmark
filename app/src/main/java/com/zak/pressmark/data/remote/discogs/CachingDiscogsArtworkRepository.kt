// File: app/src/main/java/com/zak/pressmark/data/remote/discogs/CachingDiscogsArtworkRepository.kt
package com.zak.pressmark.data.remote.discogs

import com.zak.pressmark.data.local.entity.AlbumEntity
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Decorator for DiscogsArtworkRepository:
 * - in-memory LRU cache (including caching null = "not found")
 * - in-flight de-dupe (multiple callers for same album share one request)
 */
class CachingDiscogsArtworkRepository(
    private val delegate: DiscogsArtworkRepository,
    cacheSize: Int = 500
) : DiscogsArtworkRepository {

    private val mutex = Mutex()
    private val cache = LruCache<CacheKey, DiscogsArtwork?>(maxSize = cacheSize)
    private val inFlight = mutableMapOf<CacheKey, Deferred<DiscogsArtwork?>>()

    override suspend fun getArtwork(album: AlbumEntity): DiscogsArtwork? = coroutineScope {
        val key = CacheKey.from(album)

        mutex.withLock {
            if (cache.contains(key)) return@coroutineScope cache.get(key)
            inFlight[key]?.let { return@coroutineScope it.await() }
        }

        val deferred = mutex.withLock {
            inFlight[key]?.let { return@withLock it }

            val created = async(Dispatchers.IO) {
                delegate.getArtwork(album)
            }
            inFlight[key] = created
            created
        }

        val result = deferred.await()

        mutex.withLock {
            cache.put(key, result)
            inFlight.remove(key)
        }

        result
    }

    private data class CacheKey(
        val title: String,
        val artistId: String?,
        val year: Int?,
        val label: String?,
        val catno: String?
    ) {
        companion object {
            fun from(album: AlbumEntity): CacheKey {
                val title = album.title.trim()
                val artist = album.artistId?.takeIf { it > 0L }?.toString()
                val label = album.label?.trim()?.takeIf { it.isNotBlank() }
                val catno = album.catalogNo?.trim()?.takeIf { it.isNotBlank() }

                return CacheKey(
                    title = title,
                    artistId = artist,
                    year = album.releaseYear,
                    label = label,
                    catno = catno
                )
            }
        }
    }

    private class LruCache<K, V>(private val maxSize: Int) {
        private val map = LinkedHashMap<K, V>(maxSize, 0.75f, true)

        fun contains(key: K): Boolean = map.containsKey(key)

        fun get(key: K): V? = map[key]

        fun put(key: K, value: V) {
            map[key] = value
            if (map.size > maxSize) {
                val eldestKey = map.entries.iterator().next().key
                map.remove(eldestKey)
            }
        }
    }
}
