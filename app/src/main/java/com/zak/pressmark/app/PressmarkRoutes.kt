package com.zak.pressmark.app

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
}
