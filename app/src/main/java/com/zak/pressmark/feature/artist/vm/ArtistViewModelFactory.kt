package com.zak.pressmark.feature.artist.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.app.di.AppGraph

class ArtistViewModelFactory(
    private val graph: AppGraph,
    private val artistId: Long,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(ArtistViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        return ArtistViewModel(
            artistId = artistId,
            albumRepository = graph.albumRepository,
            artistRepository = graph.artistRepository,
        ) as T
    }
}
