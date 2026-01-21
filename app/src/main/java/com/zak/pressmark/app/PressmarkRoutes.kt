// FILE: app/src/main/java/com/zak/pressmark/app/PressmarkRoutes.kt
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

    // Inbox pipeline
    const val SCAN_CONVEYOR = "scan_conveyor"
    const val INBOX = "inbox"
    const val RESOLVE_INBOX = "resolve_inbox"
    const val ARG_INBOX_ID = "inboxId"
    const val RESOLVE_INBOX_PATTERN = "$RESOLVE_INBOX/{$ARG_INBOX_ID}"
    fun resolveInbox(inboxId: String): String = "$RESOLVE_INBOX/$inboxId"

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
    const val ARG_COVER_YEAR = "year"
    const val ARG_COVER_LABEL = "label"
    const val ARG_COVER_CATNO = "catno"
    const val ARG_COVER_BARCODE = "barcode"
    const val ARG_COVER_ORIGIN = "origin"

    // Where should Cover Search return?
    const val COVER_ORIGIN_BACK = "back"       // default: just popBackStack()
    const val COVER_ORIGIN_DETAILS = "details" // after closing, go to Album Details
    const val COVER_ORIGIN_LIST_SUCCESS = "list_success" // after closing, go to Album List + success dialog
    const val COVER_ORIGIN_ADD_ANOTHER = "add_another"   // after closing, go back to Add Album and clear form
    const val COVER_SEARCH_PATTERN =
        "$COVER_SEARCH/{$ARG_ALBUM_ID}?" +
            "$ARG_COVER_ARTIST={$ARG_COVER_ARTIST}" +
            "&$ARG_COVER_TITLE={$ARG_COVER_TITLE}" +
            "&$ARG_COVER_YEAR={$ARG_COVER_YEAR}" +
            "&$ARG_COVER_LABEL={$ARG_COVER_LABEL}" +
            "&$ARG_COVER_CATNO={$ARG_COVER_CATNO}" +
            "&$ARG_COVER_BARCODE={$ARG_COVER_BARCODE}" +
            "&$ARG_COVER_ORIGIN={$ARG_COVER_ORIGIN}"

    fun coverSearch(
        albumId: String,
        artist: String,
        title: String,
        releaseYear: String,
        label: String,
        catalogNo: String,
        barcode: String,
        origin: String = COVER_ORIGIN_BACK,
    ): String {
        val a = Uri.encode(artist)
        val t = Uri.encode(title)
        val y = Uri.encode(releaseYear)
        val l = Uri.encode(label)
        val c = Uri.encode(catalogNo)
        val b = Uri.encode(barcode)
        val o = Uri.encode(origin)
        return "$COVER_SEARCH/$albumId?$ARG_COVER_ARTIST=$a&$ARG_COVER_TITLE=$t" +
            "&$ARG_COVER_YEAR=$y&$ARG_COVER_LABEL=$l&$ARG_COVER_CATNO=$c" +
            "&$ARG_COVER_BARCODE=$b&$ARG_COVER_ORIGIN=$o"
    }

    // Camera capture (local cover)
    const val COVER_CAPTURE = "cover_capture"
    const val COVER_CAPTURE_PATTERN = "$COVER_CAPTURE/{$ARG_ALBUM_ID}?$ARG_COVER_ORIGIN={$ARG_COVER_ORIGIN}"

    fun coverCapture(
        albumId: String,
        origin: String,
    ): String {
        val o = Uri.encode(origin)
        return "$COVER_CAPTURE/$albumId?$ARG_COVER_ORIGIN=$o"
    }
}
