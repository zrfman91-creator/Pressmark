package com.zak.pressmark.feature.artworkpicker.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.core.artwork.ArtworkCandidate
import com.zak.pressmark.core.artwork.ArtworkPickerDialog
import com.zak.pressmark.core.artwork.ArtworkProviderId
import com.zak.pressmark.feature.artworkpicker.ArtworkPickerViewModelFactory
import com.zak.pressmark.feature.artworkpicker.DiscogsCoverSearchViewModel

@Composable
fun CoverSearchRoute(
    graph: AppGraph,
    albumId: String,
    artist: String,
    title: String,
    onClose: () -> Unit,
) {
    val factory = remember(graph) {
        ArtworkPickerViewModelFactory(
            albumRepository = graph.albumRepository,
            discogsApi = graph.discogsApiService,
        )
    }

    val vm: DiscogsCoverSearchViewModel = viewModel(
        key = "cover_search_$albumId",
        factory = factory,
    )

    val state by vm.uiState.collectAsStateWithLifecycle()

    // Kick off / update search when inputs change
    LaunchedEffect(albumId, artist, title) {
        if (albumId.isNotBlank()) {
            vm.start(
                albumId = albumId,
                artist = artist,
                title = title,
            )
        }
    }

    val discogsResults = state.results
    val discogsById = remember(discogsResults) {
        discogsResults.associateBy { it.id.toString() }
    }

    val candidates: List<ArtworkCandidate> = remember(discogsResults) {
        discogsResults.map { r ->
            ArtworkCandidate(
                provider = ArtworkProviderId.DISCOGS,
                providerItemId = r.id.toString(),
                imageUrl = r.coverImage ?: r.thumb,
                thumbUrl = r.thumb,
                displayTitle = r.title.toString(),
                displayArtist = null,
                subtitle = null,
            )
        }
    }

    ArtworkPickerDialog(
        artist = artist,
        title = title,
        results = candidates,
        onPick = { candidate ->
            discogsById[candidate.providerItemId]?.let { picked ->
                vm.pickResult(picked)
            }
            onClose()
        },
        onDismiss = onClose,
    )
}
