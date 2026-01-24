// FILE: app/src/main/java/com/zak/pressmark/feature/addalbum/vm/AddAlbumViewModelFactory.kt
package com.zak.pressmark.feature.addalbum.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.v1.ReleaseRepository
import com.zak.pressmark.data.repository.v1.ArtistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi

class AddAlbumViewModelFactory(
    private val artistRepository: ArtistRepository,
    private val releaseRepository: ReleaseRepository,
) : ViewModelProvider.Factory {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(AddAlbumViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        return AddAlbumViewModel(
            artistRepository = artistRepository,
            releaseRepository = releaseRepository,
        ) as T
    }
}
