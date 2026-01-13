// path: app/src/main/java/com/zak/pressmark/feature/artist/vm/ArtistViewModelFactory.kt
package com.zak.pressmark.feature.artist.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.feature.artist.vm.ArtistViewModel

class ArtistViewModelFactory(
    private val graph: AppGraph,
    private val artistId: Long,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArtistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // *** THE FIX IS HERE ***
            // Use the correct parameter names from the ArtistViewModel constructor
            return ArtistViewModel(
                artistId = artistId,
                artistRepository = graph.artistRepository,
                albumRepository = graph.albumRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
