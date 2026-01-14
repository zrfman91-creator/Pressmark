package com.zak.pressmark.feature.albumdetails.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.app.di.AppGraph

class AlbumDetailsViewModelFactory(
    private val graph: AppGraph,
    private val albumId: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(AlbumDetailsViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        return AlbumDetailsViewModel(
            albumId = albumId,
            repo = graph.albumRepository,
            artistRepo = graph.artistRepository,
        ) as T
    }
}
