package com.zak.pressmark.app

import android.net.Uri

/**
 * Centralized navigation route definitions for Pressmark.
 * These must stay in sync with the arguments used in PressmarkNavHost.
 */
object PressmarkRoutes {

    // List Screen (start destination)
    const val LIST = "album_list"
    const val ADD = "add"

    // Album Details
    const val DETAILS = "album_details"
    const val ARG_ALBUM_ID = "albumId"
    const val DETAILS_PATTERN = "$DETAILS/{$ARG_ALBUM_ID}"
    fun details(albumId: String): String = "$DETAILS/$albumId"

    // Artist Screen
    const val ARTIST = "artist"
    const val ARG_ARTIST_ID = "artistId"
    const val ARTIST_PATTERN = "$ARTIST/{$ARG_ARTIST_ID}"
    fun artist(artistId: Long): String = "$ARTIST/$artistId"

    // Cover Search (Artwork picker)
    const val COVER_SEARCH = "cover_search"
    const val ARG_COVER_ARTIST = "artist"
    const val ARG_COVER_TITLE = "title"
    const val COVER_SEARCH_PATTERN =
        "$COVER_SEARCH/{$ARG_ALBUM_ID}?$ARG_COVER_ARTIST={$ARG_COVER_ARTIST}&$ARG_COVER_TITLE={$ARG_COVER_TITLE}"

    fun coverSearch(albumId: String, artist: String, title: String): String {
        val a = Uri.encode(artist)
        val t = Uri.encode(title)
        return "$COVER_SEARCH/$albumId?$ARG_COVER_ARTIST=$a&$ARG_COVER_TITLE=$t"
    }
}
