package com.zak.pressmark.feature.albumlist.coversearch.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.app.di.AppGraph

class DiscogsCoverSearchViewModelFactory(
    private val graph: AppGraph,
    private val albumId: String,
    private val artist: String,
    private val title: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscogsCoverSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscogsCoverSearchViewModel(
                albumId = albumId,
                artist = artist,
                title = title,
                albumRepository = graph.albumRepository,
                discogsApi = graph.discogsApiService, // Pass the API service
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
