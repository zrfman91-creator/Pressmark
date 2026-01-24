// FILE: app/src/main/java/com/zak/pressmark/feature/artist/vm/ArtistViewModel.kt
package com.zak.pressmark.feature.artist.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import com.zak.pressmark.data.repository.v1.AlbumRepository
import com.zak.pressmark.data.repository.v1.ArtistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ArtistViewModel(
    val artistId: Long,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
) : ViewModel() {

    val artistName: StateFlow<String?> =
        artistRepository.observeById(artistId)
            .map { it?.displayName }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    val albums: StateFlow<List<AlbumWithArtistName>> =
        albumRepository.observeByArtistIdWithArtistName(artistId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )
}
