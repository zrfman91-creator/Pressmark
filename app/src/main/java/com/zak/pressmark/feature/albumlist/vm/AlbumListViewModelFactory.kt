package com.zak.pressmark.feature.albumlist.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.app.di.AppGraph

class AlbumListViewModelFactory(
    private val graph: AppGraph,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlbumListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlbumListViewModel(
                // Pass the public repository from the graph
                albumRepository = graph.albumRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
