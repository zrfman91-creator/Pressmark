package com.zak.pressmark.data.local.model

import androidx.room.ColumnInfo
import com.zak.pressmark.data.local.entity.CreditRole

// Flat credit row joined with artist display name for Release details rendering.

data class ReleaseCreditRow(
    @ColumnInfo(name = "credit_artist_id")
    val artistId: Long,

    @ColumnInfo(name = "credit_role")
    val role: CreditRole,

    @ColumnInfo(name = "credit_position")
    val position: Int,

    @ColumnInfo(name = "credit_display_hint")
    val displayHint: String?,

    @ColumnInfo(name = "artist_display_name")
    val artistDisplayName: String,
)